package com.sciinov.dbms.controller;

import com.sciinov.dbms.service.ConferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * GET /api/conferences/{id}/documents/count → conference document count
 * GET /api/conferences/{id}/dashboard-data/count → dashboard data records count
 */
@RestController
@RequestMapping("/api/conferences")
@Tag(name = "Conference Counts", description = "APIs for conference statistics and counts")
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
    @Operation(summary = "Get total conference count", description = "Get count of all active conferences")
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
    @Operation(summary = "Get dashboard count for conference", description = "Get count of dashboards linked to a conference")
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

    /**
     * Get the count of conference documents (Program, Book, Positive Sheets) for a conference.
     *
     * GET /api/conferences/{id}/documents/count
     * Access: SUPER_ADMIN, ADMIN
     *
     * Response:
     * {
     *   "success": true,
     *   "conferenceId": "abc123",
     *   "totalDocuments": 45,
     *   "documentTypes": {
     *     "program": 15,
     *     "book": 20,
     *     "positive_sheets": 10
     *   }
     * }
     */
    @GetMapping("/{id}/documents/count")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Get conference document count", description = "Get count of all documents (Program, Book, Positive Sheets) for a conference")
    public ResponseEntity<Map<String, Object>> getConferenceDocumentCount(@PathVariable String id) {
        logger.info("GET /api/conferences/{}/documents/count - Fetching document count for conference", id);
        try {
            long totalCount = conferenceService.getTotalConferenceDocumentsForConference(id);
            Map<String, Long> documentTypes = conferenceService.getDocumentTypeCountForConference(id);

            logger.info("GET /api/conferences/{}/documents/count - Total documents: {}", id, totalCount);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "conferenceId", id,
                    "totalDocuments", totalCount,
                    "documentTypes", documentTypes
            ));
        } catch (RuntimeException e) {
            logger.warn("GET /api/conferences/{}/documents/count - Not found: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("GET /api/conferences/{}/documents/count - Error: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch document count: " + e.getMessage()
            ));
        }
    }

    /**
     * Get the count of dashboard data records linked to a particular conference.
     *
     * GET /api/conferences/{id}/dashboard-data/count
     * Access: SUPER_ADMIN, ADMIN
     *
     * Response:
     * {
     *   "success": true,
     *   "conferenceId": "abc123",
     *   "totalDataRecords": 100000,
     *   "dataByDashboard": {
     *     "Dashboard 1": 30000,
     *     "Dashboard 2": 50000,
     *     "Dashboard 3": 20000
     *   }
     * }
     */
    @GetMapping("/{id}/dashboard-data/count")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    @Operation(summary = "Get dashboard data record count", description = "Get count of all dashboard data records linked to a conference")
    public ResponseEntity<Map<String, Object>> getDashboardDataCount(@PathVariable String id) {
        logger.info("GET /api/conferences/{}/dashboard-data/count - Fetching dashboard data count for conference", id);
        try {
            long totalCount = conferenceService.getTotalDashboardDataRecordsForConference(id);
            Map<String, Long> dashboardDataCounts = conferenceService.getDashboardDataCountsForConference(id);

            logger.info("GET /api/conferences/{}/dashboard-data/count - Total records: {}", id, totalCount);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "conferenceId", id,
                    "totalDataRecords", totalCount,
                    "dataByDashboard", dashboardDataCounts
            ));
        } catch (RuntimeException e) {
            logger.warn("GET /api/conferences/{}/dashboard-data/count - Not found: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("GET /api/conferences/{}/dashboard-data/count - Error: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch dashboard data count: " + e.getMessage()
            ));
        }
    }
}

