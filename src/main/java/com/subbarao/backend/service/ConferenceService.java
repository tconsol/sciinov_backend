package com.subbarao.backend.service;

import com.subbarao.backend.entity.Conference;
import com.subbarao.backend.repository.ConferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConferenceService {
    @Autowired
    private ConferenceRepository conferenceRepository;

    public List<Conference> getAllConferences() {
        return conferenceRepository.findByDeletedFalse();
    }

    public Optional<Conference> getConferenceById(String id) {
        return conferenceRepository.findByIdAndDeletedFalse(id);
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
}
