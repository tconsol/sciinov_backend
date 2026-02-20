package com.sciinov.dbms.controller;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.ConferenceDocumentLog;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.AnalyticsService;
import com.sciinov.dbms.service.ConferenceDocumentLogService;
import com.sciinov.dbms.service.LogSseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ConferenceDocumentLogService conferenceDocumentLogService;

    @Autowired
    private LogSseService logSseService;

    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ═══════════════════════════════════════════════════════════════════
    // SSE — Real-time Log Streaming (no page refresh needed)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * SUPER_ADMIN: Subscribe to ALL real-time log events (data logs + doc logs).
     *
     * Frontend usage:
     *   const es = new EventSource('/api/analytics/stream?token=<JWT>');
     *   es.addEventListener('data-log', e => { const log = JSON.parse(e.data); ... });
     *   es.addEventListener('doc-log',  e => { const log = JSON.parse(e.data); ... });
     *   es.addEventListener('connected', e => console.log('connected'));
     *
     * GET /api/analytics/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SseEmitter streamAllLogs() {
        logger.info("SUPER_ADMIN subscribed to real-time log stream");
        return logSseService.subscribeSuperAdmin();
    }

    /**
     * ADMIN: Subscribe to their own real-time log events only.
     *
     * Frontend usage (admin dashboard):
     *   const es = new EventSource('/api/analytics/stream/me?token=<JWT>');
     *   es.addEventListener('data-log', e => { ... });
     *   es.addEventListener('doc-log',  e => { ... });
     *
     * GET /api/analytics/stream/me
     */
    @GetMapping(value = "/stream/me", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public SseEmitter streamMyLogs() {
        String adminId = getCurrentUser().getId();
        logger.info("ADMIN {} subscribed to own real-time log stream", adminId);
        return logSseService.subscribeAdmin(adminId);
    }

    /**
     * Health check — returns number of active SSE connections.
     * GET /api/analytics/stream/connections
     */
    @GetMapping("/stream/connections")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getConnectionStats() {
        return ResponseEntity.ok(logSseService.getConnectionStats());
    }

    // ── ADMIN: own upload stats ───────────────────────────────────────

    /** GET /api/analytics/upload-stats/me
     *  Admin sees only their own upload history (all conferences) */
    @GetMapping("/upload-stats/me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyUploadStats() {
        String adminId = getCurrentUser().getId();
        logger.info("GET /api/analytics/upload-stats/me - admin {}", adminId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByAdmin(adminId);
        return ResponseEntity.ok(Map.of("success", true, "count", stats.size(), "data", stats));
    }

    /** GET /api/analytics/upload-stats/me/conference/{conferenceId}
     *  Admin sees their own upload history scoped to one conference */
    @GetMapping("/upload-stats/me/conference/{conferenceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyUploadStatsByConference(@PathVariable String conferenceId) {
        String adminId = getCurrentUser().getId();
        logger.info("GET upload-stats/me/conference/{} - admin {}", conferenceId, adminId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByAdminAndConference(adminId, conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", stats.size(), "data", stats));
    }

    // ── ADMIN: own activity logs ──────────────────────────────────────

    /** GET /api/analytics/logs/me
     *  Admin sees only their own activity logs */
    @GetMapping("/logs/me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyLogs() {
        String adminId = getCurrentUser().getId();
        logger.info("GET /api/analytics/logs/me - admin {}", adminId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByAdmin(adminId);
        return ResponseEntity.ok(Map.of("success", true, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/logs/me/conference/{conferenceId}
     *  Admin sees their own logs for a specific conference */
    @GetMapping("/logs/me/conference/{conferenceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyLogsByConference(@PathVariable String conferenceId) {
        String adminId = getCurrentUser().getId();
        logger.info("GET logs/me/conference/{} - admin {}", conferenceId, adminId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByAdminAndConference(adminId, conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", logs.size(), "data", logs));
    }

    // ── SUPER ADMIN: all upload stats ─────────────────────────────────

    /** GET /api/analytics/upload-stats
     *  Super Admin sees all upload stats across all admins */
    @GetMapping("/upload-stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllUploadStats() {
        logger.info("GET /api/analytics/upload-stats - SUPER_ADMIN");
        List<DashboardUploadStats> stats = analyticsService.getAllUploadStats();
        return ResponseEntity.ok(Map.of("success", true, "count", stats.size(), "data", stats));
    }

    /** GET /api/analytics/upload-stats/admin/{adminId}
     *  Super Admin views a specific admin's upload stats */
    @GetMapping("/upload-stats/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getUploadStatsByAdmin(@PathVariable String adminId) {
        logger.info("GET upload-stats/admin/{}", adminId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByAdmin(adminId);
        return ResponseEntity.ok(Map.of("success", true, "adminId", adminId, "count", stats.size(), "data", stats));
    }

    /** GET /api/analytics/upload-stats/conference/{conferenceId}
     *  Super Admin views upload stats for a specific conference */
    @GetMapping("/upload-stats/conference/{conferenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getUploadStatsByConference(@PathVariable String conferenceId) {
        logger.info("GET upload-stats/conference/{}", conferenceId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByConference(conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", stats.size(), "data", stats));
    }

    // ── SUPER ADMIN: all activity logs ────────────────────────────────

    /** GET /api/analytics/logs
     *  Super Admin sees all activity logs across all admins */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllLogs() {
        logger.info("GET /api/analytics/logs - SUPER_ADMIN");
        List<AdminActivityLog> logs = analyticsService.getAllActivityLogs();
        return ResponseEntity.ok(Map.of("success", true, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/logs/admin/{adminId}
     *  Super Admin views a specific admin's activity logs */
    @GetMapping("/logs/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getLogsByAdmin(@PathVariable String adminId) {
        logger.info("GET logs/admin/{}", adminId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByAdmin(adminId);
        return ResponseEntity.ok(Map.of("success", true, "adminId", adminId, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/logs/conference/{conferenceId}
     *  Super Admin views all data-logs for a specific conference */
    @GetMapping("/logs/conference/{conferenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getLogsByConference(@PathVariable String conferenceId) {
        logger.info("GET logs/conference/{}", conferenceId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByConference(conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", logs.size(), "data", logs));
    }

    // ── CONFERENCE DOCUMENT LOGS (separate collection) ─────────────────

    /** GET /api/analytics/doc-logs/me
     *  Admin sees their own conference-document logs (upload/download/delete/view), latest first */
    @GetMapping("/doc-logs/me")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyDocLogs() {
        String adminId = getCurrentUser().getId();
        logger.info("GET /api/analytics/doc-logs/me - admin {}", adminId);
        List<ConferenceDocumentLog> logs = conferenceDocumentLogService.getMyLogs(adminId);
        return ResponseEntity.ok(Map.of("success", true, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/doc-logs/me/conference/{conferenceId}
     *  Admin sees their own document logs scoped to one conference, latest first */
    @GetMapping("/doc-logs/me/conference/{conferenceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getMyDocLogsByConference(@PathVariable String conferenceId) {
        String adminId = getCurrentUser().getId();
        logger.info("GET /api/analytics/doc-logs/me/conference/{} - admin {}", conferenceId, adminId);
        List<ConferenceDocumentLog> logs = conferenceDocumentLogService.getMyLogsByConference(adminId, conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/doc-logs
     *  Super Admin sees ALL conference-document logs across all admins, latest first */
    @GetMapping("/doc-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllDocLogs() {
        logger.info("GET /api/analytics/doc-logs - SUPER_ADMIN");
        List<ConferenceDocumentLog> logs = conferenceDocumentLogService.getAllLogs();
        return ResponseEntity.ok(Map.of("success", true, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/doc-logs/admin/{adminId}
     *  Super Admin views a specific admin's document logs, latest first */
    @GetMapping("/doc-logs/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getDocLogsByAdmin(@PathVariable String adminId) {
        logger.info("GET /api/analytics/doc-logs/admin/{}", adminId);
        List<ConferenceDocumentLog> logs = conferenceDocumentLogService.getLogsByAdmin(adminId);
        return ResponseEntity.ok(Map.of("success", true, "adminId", adminId, "count", logs.size(), "data", logs));
    }

    /** GET /api/analytics/doc-logs/conference/{conferenceId}
     *  Super Admin views all document logs for a specific conference, latest first */
    @GetMapping("/doc-logs/conference/{conferenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getDocLogsByConference(@PathVariable String conferenceId) {
        logger.info("GET /api/analytics/doc-logs/conference/{}", conferenceId);
        List<ConferenceDocumentLog> logs = conferenceDocumentLogService.getLogsByConference(conferenceId);
        return ResponseEntity.ok(Map.of("success", true, "conferenceId", conferenceId, "count", logs.size(), "data", logs));
    }
}
