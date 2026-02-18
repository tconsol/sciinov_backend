package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.DashboardMaster;
import com.sciinov.dbms.repository.ConferenceRepository;
import com.sciinov.dbms.repository.DashboardMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ConferenceService {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceService.class);

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private DashboardMasterRepository dashboardMasterRepository;

    public List<Conference> getAllConferences() {
        return conferenceRepository.findByDeletedFalse();
    }

    public Optional<Conference> getConferenceById(String id) {
        return conferenceRepository.findByIdAndDeletedFalse(id);
    }

    public List<Conference> getConferencesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Iterable<Conference> conferences = conferenceRepository.findAllById(ids);
        return StreamSupport.stream(conferences.spliterator(), false)
                .filter(c -> !c.isDeleted())
                .collect(Collectors.toList());
    }

    public Conference createConference(Conference conference) {
        conference.setCreatedAt(LocalDateTime.now());
        conference.setUpdatedAt(LocalDateTime.now());
        return conferenceRepository.save(conference);
    }

    public Conference updateConference(String id, Conference conferenceDetails) {
        Conference conference = conferenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        conference.setTitle(conferenceDetails.getTitle());
        conference.setImageUrl(conferenceDetails.getImageUrl());
        conference.setStatus(conferenceDetails.getStatus());

        // Update dashboard associations if provided
        if (conferenceDetails.getDashboardMasterIds() != null) {
            conference.setDashboardMasterIds(conferenceDetails.getDashboardMasterIds());
        }

        conference.setUpdatedAt(LocalDateTime.now());

        return conferenceRepository.save(conference);
    }

    public void deleteConference(String id) {
        Conference conference = conferenceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        conference.setDeleted(true);
        conference.setUpdatedAt(LocalDateTime.now());
        conferenceRepository.save(conference);
    }

    /**
     * Attach dashboards to a conference
     */
    public Conference attachDashboards(String conferenceId, List<String> dashboardMasterIds) {
        logger.info("Attaching {} dashboards to conference: {}", dashboardMasterIds.size(), conferenceId);

        Conference conference = conferenceRepository.findByIdAndDeletedFalse(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Validate that all dashboard masters exist
        List<DashboardMaster> dashboards = dashboardMasterRepository.findAllById(dashboardMasterIds);
        if (dashboards.size() != dashboardMasterIds.size()) {
            throw new RuntimeException("One or more dashboard masters not found");
        }

        // Add new dashboard IDs (avoid duplicates)
        List<String> currentDashboards = conference.getDashboardMasterIds();
        if (currentDashboards == null) {
            currentDashboards = new ArrayList<>();
        }

        for (String dashboardId : dashboardMasterIds) {
            if (!currentDashboards.contains(dashboardId)) {
                currentDashboards.add(dashboardId);
            }
        }

        conference.setDashboardMasterIds(currentDashboards);
        conference.setUpdatedAt(LocalDateTime.now());

        Conference updated = conferenceRepository.save(conference);
        logger.info("Successfully attached dashboards to conference: {}", conferenceId);

        return updated;
    }

    /**
     * Detach dashboards from a conference
     */
    public Conference detachDashboards(String conferenceId, List<String> dashboardMasterIds) {
        logger.info("Detaching {} dashboards from conference: {}", dashboardMasterIds.size(), conferenceId);

        Conference conference = conferenceRepository.findByIdAndDeletedFalse(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        List<String> currentDashboards = conference.getDashboardMasterIds();
        if (currentDashboards != null) {
            currentDashboards.removeAll(dashboardMasterIds);
            conference.setDashboardMasterIds(currentDashboards);
            conference.setUpdatedAt(LocalDateTime.now());
        }

        Conference updated = conferenceRepository.save(conference);
        logger.info("Successfully detached dashboards from conference: {}", conferenceId);

        return updated;
    }

    /**
     * Replace all dashboards for a conference
     */
    public Conference setDashboards(String conferenceId, List<String> dashboardMasterIds) {
        logger.info("Setting {} dashboards for conference: {}", dashboardMasterIds.size(), conferenceId);

        Conference conference = conferenceRepository.findByIdAndDeletedFalse(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        // Validate that all dashboard masters exist
        if (dashboardMasterIds != null && !dashboardMasterIds.isEmpty()) {
            List<DashboardMaster> dashboards = dashboardMasterRepository.findAllById(dashboardMasterIds);
            if (dashboards.size() != dashboardMasterIds.size()) {
                throw new RuntimeException("One or more dashboard masters not found");
            }
        }

        conference.setDashboardMasterIds(dashboardMasterIds != null ? dashboardMasterIds : new ArrayList<>());
        conference.setUpdatedAt(LocalDateTime.now());

        Conference updated = conferenceRepository.save(conference);
        logger.info("Successfully set dashboards for conference: {}", conferenceId);

        return updated;
    }

    /**
     * Get all dashboards for a conference
     */
    public List<DashboardMaster> getDashboardsForConference(String conferenceId) {
        logger.info("Getting dashboards for conference: {}", conferenceId);

        Conference conference = conferenceRepository.findByIdAndDeletedFalse(conferenceId)
                .orElseThrow(() -> new RuntimeException("Conference not found"));

        List<String> dashboardIds = conference.getDashboardMasterIds();
        if (dashboardIds == null || dashboardIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<DashboardMaster> dashboards = dashboardMasterRepository.findAllById(dashboardIds)
                .stream()
                .filter(dm -> !dm.isDeleted())
                .collect(Collectors.toList());

        logger.info("Found {} dashboards for conference: {}", dashboards.size(), conferenceId);

        return dashboards;
    }
}
