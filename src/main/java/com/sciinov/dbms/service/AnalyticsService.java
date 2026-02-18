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
    private UserRepository userRepository; // Autowire UserRepository
    
    public List<DashboardUploadStats> getUploadStatsByAdmin(String adminId) {
        return dashboardUploadStatsRepository.findByAdminId(adminId);
    }
    
    public List<DashboardUploadStats> getUploadStatsByConference(String conferenceId) {
        return dashboardUploadStatsRepository.findByConferenceId(conferenceId);
    }
    
    public List<AdminActivityLog> getActivityLogsByAdmin(String adminId) {
        List<AdminActivityLog> logs = adminActivityLogRepository.findByAdminId(adminId);
        return populateAdminNames(logs);
    }
    
    public List<AdminActivityLog> getAllActivityLogs() {
        List<AdminActivityLog> logs = adminActivityLogRepository.findAll();
        return populateAdminNames(logs);
    }

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
