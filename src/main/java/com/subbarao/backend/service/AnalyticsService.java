package com.subbarao.backend.service;

import com.subbarao.backend.entity.AdminActivityLog;
import com.subbarao.backend.entity.DashboardUploadStats;
import com.subbarao.backend.repository.AdminActivityLogRepository;
import com.subbarao.backend.repository.DashboardUploadStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {
    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;
    
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    
    public List<DashboardUploadStats> getUploadStatsByAdmin(String adminId) {
        return dashboardUploadStatsRepository.findByAdminId(adminId);
    }
    
    public List<DashboardUploadStats> getUploadStatsByConference(String conferenceId) {
        return dashboardUploadStatsRepository.findByConferenceId(conferenceId);
    }
    
    public List<AdminActivityLog> getActivityLogsByAdmin(String adminId) {
        return adminActivityLogRepository.findByAdminId(adminId);
    }
    
    public List<AdminActivityLog> getAllActivityLogs() {
        return adminActivityLogRepository.findAll();
    }
}
