package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.AdminActivityLogRepository;
import com.sciinov.dbms.repository.DashboardDataRepository;
import com.sciinov.dbms.repository.DashboardUploadStatsRepository;
import com.sciinov.dbms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalyticsService {

    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogSseService logSseService;

    // ── Upload Stats ──────────────────────────────────────────────────

    public List<DashboardUploadStats> getUploadStatsByAdmin(String adminId) {
        return dashboardUploadStatsRepository.findByAdminIdOrderByUploadedAtDesc(adminId);
    }

    public List<DashboardUploadStats> getUploadStatsByConference(String conferenceId) {
        return dashboardUploadStatsRepository.findByConferenceIdOrderByUploadedAtDesc(conferenceId);
    }

    public List<DashboardUploadStats> getUploadStatsByAdminAndConference(String adminId, String conferenceId) {
        return dashboardUploadStatsRepository.findByAdminIdAndConferenceId(adminId, conferenceId);
    }

    public List<DashboardUploadStats> getAllUploadStats() {
        return dashboardUploadStatsRepository.findAllByOrderByUploadedAtDesc();
    }

    // ── Activity Logs ─────────────────────────────────────────────────

    public List<AdminActivityLog> getActivityLogsByAdmin(String adminId) {
        return populateAdminNames(adminActivityLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId));
    }

    public List<AdminActivityLog> getAllActivityLogs() {
        return populateAdminNames(adminActivityLogRepository.findAllByOrderByCreatedAtDesc());
    }

    public List<AdminActivityLog> getActivityLogsByConference(String conferenceId) {
        return populateAdminNames(adminActivityLogRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId));
    }

    public List<AdminActivityLog> getActivityLogsByAdminAndConference(String adminId, String conferenceId) {
        return populateAdminNames(adminActivityLogRepository.findByAdminIdAndConferenceIdOrderByCreatedAtDesc(adminId, conferenceId));
    }

    /**
     * Save a log entry and immediately push it to all SSE subscribers in real-time.
     * Called by ExportService, ExcelService, ConferenceDocumentService after every admin action.
     */
    public AdminActivityLog saveAndPushLog(AdminActivityLog log) {
        if (log.getAdminName() == null && log.getAdminId() != null) {
            userRepository.findById(log.getAdminId())
                .ifPresent(u -> log.setAdminName(u.getFirstName() + " " + u.getLastName()));
        }
        AdminActivityLog saved = adminActivityLogRepository.save(log);
        // Push to SSE — super admin gets it, admin gets their own
        logSseService.pushDataLog(saved.getAdminId(), saved);
        return saved;
    }

    // ── Log Records ───────────────────────────────────────────────────

    /**
     * Fetch the actual DashboardData records linked to an activity log.
     * Works for UPLOAD_EXCEL and DOWNLOAD_* logs — any log with fromSerialNo/toSerialNo set.
     */
    public Map<String, Object> getLogRecords(String logId, int page, int size) {
        AdminActivityLog log = adminActivityLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));

        if (log.getFromSerialNo() == null || log.getToSerialNo() == null) {
            return Map.of(
                "success", false,
                "message", "This log has no record range — no records to show",
                "logId", logId,
                "actionType", log.getActionType().name()
            );
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("serialNo").ascending());
        List<DashboardData> records = dashboardDataRepository
                .findByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        log.getConferenceId(), log.getDashboardMasterId(),
                        log.getFromSerialNo(), log.getToSerialNo(), pageable);

        long total = dashboardDataRepository
                .countByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        log.getConferenceId(), log.getDashboardMasterId(),
                        log.getFromSerialNo(), log.getToSerialNo());

        return Map.of(
            "success", true,
            "logId", logId,
            "actionType", log.getActionType().name(),
            "fromSerialNo", log.getFromSerialNo(),
            "toSerialNo", log.getToSerialNo(),
            "page", page,
            "size", size,
            "totalRecords", total,
            "data", records
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private List<AdminActivityLog> populateAdminNames(List<AdminActivityLog> logs) {
        for (AdminActivityLog log : logs) {
            if (log.getAdminName() == null && log.getAdminId() != null) {
                Optional<User> admin = userRepository.findById(log.getAdminId());
                admin.ifPresent(u -> log.setAdminName(u.getFirstName() + " " + u.getLastName()));
            }
        }
        return logs;
    }
}
