package com.subbarao.backend.controller;

import com.subbarao.backend.entity.AdminActivityLog;
import com.subbarao.backend.entity.DashboardUploadStats;
import com.subbarao.backend.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/upload-stats/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DashboardUploadStats> getUploadStatsByAdmin(@PathVariable String adminId) {
        return analyticsService.getUploadStatsByAdmin(adminId);
    }
    
    @GetMapping("/upload-stats/conference/{conferenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DashboardUploadStats> getUploadStatsByConference(@PathVariable String conferenceId) {
        return analyticsService.getUploadStatsByConference(conferenceId);
    }
    
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AdminActivityLog> getAllLogs() {
        return analyticsService.getAllActivityLogs();
    }
    
    @GetMapping("/logs/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AdminActivityLog> getLogsByAdmin(@PathVariable String adminId) {
        return analyticsService.getActivityLogsByAdmin(adminId);
    }
}
