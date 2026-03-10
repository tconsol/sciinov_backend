package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.ConferenceDocument;
import com.sciinov.dbms.entity.ConferenceDocumentLog;
import com.sciinov.dbms.repository.ConferenceDocumentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for conference document audit logs.
 * Completely separate from AdminActivityLog (dashboard data logs).
 * Every save is also pushed to SSE subscribers via LogSseService.
 */
@Service
public class ConferenceDocumentLogService {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceDocumentLogService.class);

    @Autowired
    private ConferenceDocumentLogRepository logRepository;

    @Autowired
    private LogSseService logSseService;

    // ── Log a document action ─────────────────────────────────────────

    public void log(String adminId, String adminName, String ipAddress,
                    ConferenceDocumentLog.ActionType actionType,
                    ConferenceDocument doc) {
        try {
            ConferenceDocumentLog entry = new ConferenceDocumentLog();
            entry.setAdminId(adminId);
            entry.setAdminName(adminName);
            entry.setIpAddress(ipAddress);
            entry.setActionType(actionType);
            entry.setConferenceId(doc.getConferenceId());
            entry.setConferenceName(doc.getConferenceName());
            entry.setDocumentId(doc.getId());
            entry.setFileName(doc.getFileName());
            entry.setDocumentType(doc.getDocumentType());
            entry.setYear(doc.getYear());
            entry.setDescription(buildDescription(actionType, doc));
            entry.setCreatedAt(LocalDateTime.now());
            ConferenceDocumentLog saved = logRepository.save(entry);
            // Push to SSE in real-time
            logSseService.pushDocLog(adminId, saved);
        } catch (Exception e) {
            logger.error("Failed to save conference document log: {}", e.getMessage());
        }
    }

    /** Log a VIEW/list action (no specific document) */
    public void logView(String adminId, String adminName, String ipAddress,
                        String conferenceId, String conferenceName, String detail) {
        try {
            ConferenceDocumentLog entry = new ConferenceDocumentLog();
            entry.setAdminId(adminId);
            entry.setAdminName(adminName);
            entry.setIpAddress(ipAddress);
            entry.setActionType(ConferenceDocumentLog.ActionType.VIEW);
            entry.setConferenceId(conferenceId);
            entry.setConferenceName(conferenceName);
            entry.setDescription("Viewed documents" + (detail != null ? " | " + detail : ""));
            entry.setCreatedAt(LocalDateTime.now());
            ConferenceDocumentLog saved = logRepository.save(entry);
            logSseService.pushDocLog(adminId, saved);
        } catch (Exception e) {
            logger.error("Failed to save conference document view log: {}", e.getMessage());
        }
    }

    // ── Query methods ─────────────────────────────────────────────────

    /** Admin: own logs (all conferences), latest first */
    public List<ConferenceDocumentLog> getMyLogs(String adminId) {
        return logRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    /** Admin: own logs scoped to one conference, latest first */
    public List<ConferenceDocumentLog> getMyLogsByConference(String adminId, String conferenceId) {
        return logRepository.findByAdminIdAndConferenceIdOrderByCreatedAtDesc(adminId, conferenceId);
    }

    /** Super Admin: all logs, latest first */
    public List<ConferenceDocumentLog> getAllLogs() {
        return logRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Super Admin: logs for a specific admin, latest first */
    public List<ConferenceDocumentLog> getLogsByAdmin(String adminId) {
        return logRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    /** Super Admin: logs for a specific conference, latest first */
    public List<ConferenceDocumentLog> getLogsByConference(String conferenceId) {
        return logRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId);
    }

    // ── Helper ────────────────────────────────────────────────────────

    private String buildDescription(ConferenceDocumentLog.ActionType actionType, ConferenceDocument doc) {
        String typeName = doc.getDocumentTypeDisplayName() != null
                ? doc.getDocumentTypeDisplayName()
                : (doc.getDocumentType() != null ? doc.getDocumentType() : "Unknown");
        String fileName = doc.getFileName() != null ? doc.getFileName() : "";
        int year = doc.getYear() != null ? doc.getYear() : 0;
        String conf = doc.getConferenceName() != null ? doc.getConferenceName() : doc.getConferenceId();
        switch (actionType) {
            case UPLOAD:   return "Uploaded " + typeName + " '" + fileName + "' (Year: " + year + ") for " + conf;
            case DOWNLOAD: return "Downloaded " + typeName + " '" + fileName + "' (Year: " + year + ") from " + conf;
            case DELETE:   return "Deleted " + typeName + " '" + fileName + "' (Year: " + year + ") from " + conf;
            case UPDATE:   return "Updated " + typeName + " '" + fileName + "' (Year: " + year + ") for " + conf;
            default:       return typeName + " '" + fileName + "' — " + actionType.name();
        }
    }
}
