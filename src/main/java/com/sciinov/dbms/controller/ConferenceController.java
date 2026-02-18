package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ConferenceDashboardRequest;
import com.sciinov.dbms.dto.ConferenceDashboardResponse;
import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.DashboardMaster;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ConferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/conferences")
public class ConferenceController {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceController.class);

    @Autowired
    private ConferenceService conferenceService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<Conference> getAllConferences() {
        logger.info("GET /api/conferences - Retrieving all conferences");
        List<Conference> conferences = conferenceService.getAllConferences();
        logger.info("GET /api/conferences - Retrieved {} conferences", conferences.size());
        return conferences;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Conference> getMyConferences() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getId();
        logger.info("GET /api/conferences/me - Retrieving conferences for admin {}", userId);
        
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<String> conferenceIds = user.getConferenceIds();
        
        if (conferenceIds == null || conferenceIds.isEmpty()) {
            logger.info("GET /api/conferences/me - No conferences assigned to admin {}", userId);
            return new ArrayList<>();
        }
        
        List<Conference> conferences = conferenceService.getConferencesByIds(conferenceIds);
        logger.info("GET /api/conferences/me - Retrieved {} conferences for admin {}", conferences.size(), userId);
        return conferences;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Conference> getConferenceById(@PathVariable String id) {
        logger.info("GET /api/conferences/{} - Retrieving conference by ID", id);
        return conferenceService.getConferenceById(id)
                .map(conference -> {
                    logger.info("GET /api/conferences/{} - Conference found: {}", id, conference.getTitle());
                    return ResponseEntity.ok(conference);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/conferences/{} - Conference not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Conference createConference(@RequestBody Conference conference) {
        logger.info("POST /api/conferences - Creating new conference: {}", conference.getTitle());
        Conference created = conferenceService.createConference(conference);
        logger.info("POST /api/conferences - Conference created successfully with ID: {}", created.getId());
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Conference> updateConference(@PathVariable String id, @RequestBody Conference conference) {
        logger.info("PUT /api/conferences/{} - Updating conference: {}", id, conference.getTitle());
        Conference updated = conferenceService.updateConference(id, conference);
        logger.info("PUT /api/conferences/{} - Conference updated successfully", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteConference(@PathVariable String id) {
        logger.info("DELETE /api/conferences/{} - Deleting conference", id);
        conferenceService.deleteConference(id);
        logger.info("DELETE /api/conferences/{} - Conference deleted successfully", id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all dashboards attached to a conference
     */
    @GetMapping("/{id}/dashboards")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ConferenceDashboardResponse> getConferenceDashboards(@PathVariable String id) {
        logger.info("GET /api/conferences/{}/dashboards - Retrieving dashboards for conference", id);
        try {
            Conference conference = conferenceService.getConferenceById(id)
                    .orElseThrow(() -> new RuntimeException("Conference not found"));

            List<DashboardMaster> dashboards = conferenceService.getDashboardsForConference(id);

            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    conference.getId(),
                    conference.getTitle(),
                    dashboards,
                    "Dashboards retrieved successfully",
                    true
            );

            logger.info("GET /api/conferences/{}/dashboards - Retrieved {} dashboards", id, dashboards.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("GET /api/conferences/{}/dashboards - Error: {}", id, e.getMessage());
            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    null, null, null, e.getMessage(), false
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Attach dashboards to a conference
     */
    @PostMapping("/{id}/dashboards/attach")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ConferenceDashboardResponse> attachDashboards(
            @PathVariable String id,
            @RequestBody ConferenceDashboardRequest request) {
        logger.info("POST /api/conferences/{}/dashboards/attach - Attaching {} dashboards", id, request.getDashboardMasterIds().size());
        try {
            Conference conference = conferenceService.attachDashboards(id, request.getDashboardMasterIds());
            List<DashboardMaster> dashboards = conferenceService.getDashboardsForConference(id);

            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    conference.getId(),
                    conference.getTitle(),
                    dashboards,
                    "Dashboards attached successfully",
                    true
            );

            logger.info("POST /api/conferences/{}/dashboards/attach - Successfully attached dashboards", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("POST /api/conferences/{}/dashboards/attach - Error: {}", id, e.getMessage());
            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    null, null, null, e.getMessage(), false
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Detach dashboards from a conference
     */
    @PostMapping("/{id}/dashboards/detach")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ConferenceDashboardResponse> detachDashboards(
            @PathVariable String id,
            @RequestBody ConferenceDashboardRequest request) {
        logger.info("POST /api/conferences/{}/dashboards/detach - Detaching {} dashboards", id, request.getDashboardMasterIds().size());
        try {
            Conference conference = conferenceService.detachDashboards(id, request.getDashboardMasterIds());
            List<DashboardMaster> dashboards = conferenceService.getDashboardsForConference(id);

            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    conference.getId(),
                    conference.getTitle(),
                    dashboards,
                    "Dashboards detached successfully",
                    true
            );

            logger.info("POST /api/conferences/{}/dashboards/detach - Successfully detached dashboards", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("POST /api/conferences/{}/dashboards/detach - Error: {}", id, e.getMessage());
            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    null, null, null, e.getMessage(), false
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Set/Replace all dashboards for a conference
     */
    @PutMapping("/{id}/dashboards")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ConferenceDashboardResponse> setDashboards(
            @PathVariable String id,
            @RequestBody ConferenceDashboardRequest request) {
        logger.info("PUT /api/conferences/{}/dashboards - Setting {} dashboards", id,
                    request.getDashboardMasterIds() != null ? request.getDashboardMasterIds().size() : 0);
        try {
            Conference conference = conferenceService.setDashboards(id, request.getDashboardMasterIds());
            List<DashboardMaster> dashboards = conferenceService.getDashboardsForConference(id);

            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    conference.getId(),
                    conference.getTitle(),
                    dashboards,
                    "Dashboards set successfully",
                    true
            );

            logger.info("PUT /api/conferences/{}/dashboards - Successfully set dashboards", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("PUT /api/conferences/{}/dashboards - Error: {}", id, e.getMessage());
            ConferenceDashboardResponse response = new ConferenceDashboardResponse(
                    null, null, null, e.getMessage(), false
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}
