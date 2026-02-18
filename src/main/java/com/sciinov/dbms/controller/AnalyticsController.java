package com.sciinov.dbms.controller;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/upload-stats/me")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DashboardUploadStats> getMyAdminUploadStats() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String adminId = userDetails.getId();
        logger.info("GET /api/analytics/upload-stats/me - Retrieving upload stats for admin {}", adminId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByAdmin(adminId);
        logger.info("GET /api/analytics/upload-stats/me - Retrieved {} upload stats for admin {}", stats.size(), adminId);
        return stats;
    }

    @GetMapping("/logs/me")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminActivityLog> getMyAdminLogs() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String adminId = userDetails.getId();
        logger.info("GET /api/analytics/logs/me - Retrieving activity logs for admin {}", adminId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByAdmin(adminId);
        logger.info("GET /api/analytics/logs/me - Retrieved {} activity logs for admin {}", logs.size(), adminId);
        return logs;
    }

    @GetMapping("/upload-stats/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DashboardUploadStats> getUploadStatsByAdmin(@PathVariable String adminId) {
        logger.info("GET /api/analytics/upload-stats/admin/{} - Retrieving upload stats", adminId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByAdmin(adminId);
        logger.info("GET /api/analytics/upload-stats/admin/{} - Retrieved {} upload stats", adminId, stats.size());
        return stats;
    }
    
    @GetMapping("/upload-stats/conference/{conferenceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<DashboardUploadStats> getUploadStatsByConference(@PathVariable String conferenceId) {
        logger.info("GET /api/analytics/upload-stats/conference/{} - Retrieving upload stats", conferenceId);
        List<DashboardUploadStats> stats = analyticsService.getUploadStatsByConference(conferenceId);
        logger.info("GET /api/analytics/upload-stats/conference/{} - Retrieved {} upload stats", conferenceId, stats.size());
        return stats;
    }
    
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AdminActivityLog> getAllLogs() {
        logger.info("GET /api/analytics/logs - Retrieving all activity logs");
        List<AdminActivityLog> logs = analyticsService.getAllActivityLogs();
        logger.info("GET /api/analytics/logs - Retrieved {} activity logs", logs.size());
        return logs;
    }
    
    @GetMapping("/logs/admin/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<AdminActivityLog> getLogsByAdmin(@PathVariable String adminId) {
        logger.info("GET /api/analytics/logs/admin/{} - Retrieving activity logs", adminId);
        List<AdminActivityLog> logs = analyticsService.getActivityLogsByAdmin(adminId);
        logger.info("GET /api/analytics/logs/admin/{} - Retrieved {} activity logs", adminId, logs.size());
        return logs;
    }
}
