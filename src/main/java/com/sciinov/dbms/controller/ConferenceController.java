package com.sciinov.dbms.controller;

import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.service.ConferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/conferences")
public class ConferenceController {
    @Autowired
    private ConferenceService conferenceService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public List<Conference> getAllConferences() {
        return conferenceService.getAllConferences();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Conference> getConferenceById(@PathVariable String id) {
        return conferenceService.getConferenceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Conference createConference(@RequestBody Conference conference) {
        return conferenceService.createConference(conference);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Conference> updateConference(@PathVariable String id, @RequestBody Conference conference) {
        return ResponseEntity.ok(conferenceService.updateConference(id, conference));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteConference(@PathVariable String id) {
        conferenceService.deleteConference(id);
        return ResponseEntity.ok().build();
    }
}
