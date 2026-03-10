package com.sciinov.dbms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Manages Server-Sent Event (SSE) emitters for real-time log streaming.
 *
 * Two channels:
 *  - SUPER_ADMIN channel: receives ALL log events (data logs + doc logs)
 *  - ADMIN channel (per adminId): receives only that admin's own log events
 */
@Service
public class LogSseService {

    private static final Logger logger = LoggerFactory.getLogger(LogSseService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final ObjectMapper objectMapper;

    // SUPER_ADMIN emitters — all connected super-admin clients
    private final List<SseEmitter> superAdminEmitters = new CopyOnWriteArrayList<>();

    // ADMIN emitters — keyed by adminId
    private final Map<String, List<SseEmitter>> adminEmitters = new ConcurrentHashMap<>();

    public LogSseService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ── Subscribe ─────────────────────────────────────────────────────

    /**
     * SUPER_ADMIN subscribes to the global log stream.
     * Receives all admin activity logs + doc logs in real time.
     */
    public SseEmitter subscribeSuperAdmin() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        superAdminEmitters.add(emitter);
        logger.info("SUPER_ADMIN subscribed to log stream. Total connections: {}", superAdminEmitters.size());

        emitter.onCompletion(() -> {
            superAdminEmitters.remove(emitter);
            logger.debug("SUPER_ADMIN SSE connection completed. Remaining: {}", superAdminEmitters.size());
        });
        emitter.onTimeout(() -> {
            superAdminEmitters.remove(emitter);
            logger.debug("SUPER_ADMIN SSE connection timed out. Remaining: {}", superAdminEmitters.size());
        });
        emitter.onError(e -> {
            superAdminEmitters.remove(emitter);
            logger.debug("SUPER_ADMIN SSE connection error: {}", e.getMessage());
        });

        // Send an initial "connected" event so the frontend knows the stream is live
        sendToEmitter(emitter, "connected", Map.of("message", "Connected to real-time log stream", "role", "SUPER_ADMIN"));
        return emitter;
    }

    /**
     * ADMIN subscribes to their own log stream.
     * Receives only their own activity events in real time.
     */
    public SseEmitter subscribeAdmin(String adminId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        adminEmitters.computeIfAbsent(adminId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        logger.info("ADMIN {} subscribed to log stream.", adminId);

        emitter.onCompletion(() -> removeAdminEmitter(adminId, emitter));
        emitter.onTimeout(() -> removeAdminEmitter(adminId, emitter));
        emitter.onError(e -> removeAdminEmitter(adminId, emitter));

        sendToEmitter(emitter, "connected", Map.of("message", "Connected to real-time log stream", "role", "ADMIN", "adminId", adminId));
        return emitter;
    }

    // ── Push events ───────────────────────────────────────────────────

    /**
     * Push a data log event (AdminActivityLog) to:
     *  - All SUPER_ADMIN subscribers (channel: "data-log")
     *  - The specific admin's own subscribers (channel: "data-log")
     */
    public void pushDataLog(String adminId, Object logEntry) {
        String eventName = "data-log";
        // Push to super admin
        pushToSuperAdmins(eventName, logEntry);
        // Push to the admin's own stream
        pushToAdmin(adminId, eventName, logEntry);
    }

    /**
     * Push a document log event (ConferenceDocumentLog) to:
     *  - All SUPER_ADMIN subscribers (channel: "doc-log")
     *  - The specific admin's own subscribers (channel: "doc-log")
     */
    public void pushDocLog(String adminId, Object logEntry) {
        String eventName = "doc-log";
        pushToSuperAdmins(eventName, logEntry);
        pushToAdmin(adminId, eventName, logEntry);
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private void pushToSuperAdmins(String eventName, Object data) {
        if (superAdminEmitters.isEmpty()) {
            logger.trace("No super admin SSE subscribers");
            return;
        }
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : superAdminEmitters) {
            if (!sendToEmitter(emitter, eventName, data)) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            superAdminEmitters.removeAll(deadEmitters);
            logger.debug("Removed {} dead super admin SSE emitters. Active: {}", deadEmitters.size(), superAdminEmitters.size());
        }
    }

    private void pushToAdmin(String adminId, String eventName, Object data) {
        List<SseEmitter> emitters = adminEmitters.get(adminId);
        if (emitters == null || emitters.isEmpty()) {
            logger.trace("No SSE subscribers for admin: {}", adminId);
            return;
        }
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            if (!sendToEmitter(emitter, eventName, data)) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            logger.debug("Removed {} dead SSE emitters for admin: {}. Active: {}", deadEmitters.size(), adminId, emitters.size());
        }
    }

    /**
     * @return true if sent successfully, false if emitter is dead
     * Gracefully handles disconnected clients without propagating errors
     */
    private boolean sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(json)
                    .reconnectTime(5000)); // Auto-reconnect after 5 seconds if connection drops
            return true;
        } catch (IOException e) {
            // Client disconnected or connection aborted - this is expected and not an error
            // Just silently return false so the emitter can be removed
            logger.debug("SSE client disconnected: {}", e.getClass().getSimpleName());
            return false;
        } catch (IllegalStateException e) {
            // Emitter already closed
            logger.debug("SSE emitter already closed");
            return false;
        } catch (Exception e) {
            // Unexpected error
            logger.warn("Unexpected error sending SSE event: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private void removeAdminEmitter(String adminId, SseEmitter emitter) {
        List<SseEmitter> emitters = adminEmitters.get(adminId);
        if (emitters != null) {
            emitters.remove(emitter);
            logger.debug("ADMIN {} SSE connection removed. Remaining for this admin: {}", adminId, emitters.size());
        }
    }

    /** Returns count of active SSE connections (for health/debug) */
    public Map<String, Object> getConnectionStats() {
        int adminTotal = adminEmitters.values().stream().mapToInt(List::size).sum();
        return Map.of(
            "superAdminConnections", superAdminEmitters.size(),
            "adminConnections", adminTotal,
            "adminsConnected", adminEmitters.size()
        );
    }
}

