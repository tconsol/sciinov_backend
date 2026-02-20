package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.AdminActivityLogRepository;
import com.sciinov.dbms.repository.DashboardUploadStatsRepository;
import com.sciinov.dbms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AnalyticsService {

    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private UserRepository userRepository;

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
        List<AdminActivityLog> logs = adminActivityLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
        return populateAdminNames(logs);
    }

    public List<AdminActivityLog> getAllActivityLogs() {
        List<AdminActivityLog> logs = adminActivityLogRepository.findAllByOrderByCreatedAtDesc();
        return populateAdminNames(logs);
    }

    public List<AdminActivityLog> getActivityLogsByConference(String conferenceId) {
        List<AdminActivityLog> logs = adminActivityLogRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId);
        return populateAdminNames(logs);
    }

    public List<AdminActivityLog> getActivityLogsByAdminAndConference(String adminId, String conferenceId) {
        List<AdminActivityLog> logs = adminActivityLogRepository.findByAdminIdAndConferenceId(adminId, conferenceId);
        return populateAdminNames(logs);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private List<AdminActivityLog> populateAdminNames(List<AdminActivityLog> logs) {
        for (AdminActivityLog log : logs) {
            if (log.getAdminName() == null && log.getAdminId() != null) {
                Optional<User> admin = userRepository.findById(log.getAdminId());
                admin.ifPresent(user -> log.setAdminName(user.getFirstName() + " " + user.getLastName()));
            }
        }
        return logs;
    }
}
