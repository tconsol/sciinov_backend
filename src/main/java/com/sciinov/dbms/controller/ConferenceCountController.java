package com.sciinov.dbms.controller;

import com.sciinov.dbms.service.ConferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to expose count-related endpoints for conferences and dashboards.
 *
 * GET /api/conferences/count               → overall conference records count
 * GET /api/conferences/{id}/dashboards/count → dashboard count linked to a specific conference
 */
@RestController
@RequestMapping("/api/conferences")
public class ConferenceCountController {

    private static final Logger logger = LoggerFactory.getLogger(ConferenceCountController.class);

    @Autowired
    private ConferenceService conferenceService;

    /**
     * Get the overall count of all active (non-deleted) conference records.
     *
     * GET /api/conferences/count
     * Access: SUPER_ADMIN, ADMIN
     *
     * Response:
     * {
     *   "success": true,
     *   "totalConferences": 12
     * }
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTotalConferenceCount() {
        logger.info("GET /api/conferences/count - Fetching overall conference count");
        try {
            long count = conferenceService.getTotalConferenceCount();
            logger.info("GET /api/conferences/count - Total conferences: {}", count);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalConferences", count
            ));
        } catch (Exception e) {
            logger.error("GET /api/conferences/count - Error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch conference count: " + e.getMessage()
            ));
        }
    }

    /**
     * Get the count of dashboards connected to a particular conference.
     *
     * GET /api/conferences/{id}/dashboards/count
     * Access: SUPER_ADMIN, ADMIN
     *
     * Response:
     * {
     *   "success": true,
     *   "conferenceId": "abc123",
     *   "dashboardCount": 5
     * }
     */
    @GetMapping("/{id}/dashboards/count")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardCountForConference(@PathVariable String id) {
        logger.info("GET /api/conferences/{}/dashboards/count - Fetching dashboard count for conference", id);
        try {
            long count = conferenceService.getDashboardCountForConference(id);
            logger.info("GET /api/conferences/{}/dashboards/count - Dashboard count: {}", id, count);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "conferenceId", id,
                    "dashboardCount", count
            ));
        } catch (RuntimeException e) {
            logger.warn("GET /api/conferences/{}/dashboards/count - Not found: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("GET /api/conferences/{}/dashboards/count - Error: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch dashboard count: " + e.getMessage()
            ));
        }
    }
}

