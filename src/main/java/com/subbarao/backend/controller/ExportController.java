package com.subbarao.backend.controller;

import com.subbarao.backend.entity.User;
import com.subbarao.backend.security.UserDetailsImpl;
import com.subbarao.backend.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/export")
public class ExportController {
    @Autowired
    private ExportService exportService;

    @GetMapping("/excel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcel(HttpServletResponse response,
                              @RequestParam String conferenceId,
                              @RequestParam String dashboardMasterId,
                              @RequestParam Long fromSerialNo,
                              @RequestParam Long toSerialNo) throws IOException {
        validateAccess(conferenceId);
        exportService.exportToExcel(response, conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdf(HttpServletResponse response,
                            @RequestParam String conferenceId,
                            @RequestParam String dashboardMasterId,
                            @RequestParam Long fromSerialNo,
                            @RequestParam Long toSerialNo) throws IOException {
        validateAccess(conferenceId);
        exportService.exportToPdf(response, conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);
    }

    private void validateAccess(String conferenceId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDetails.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (user.getConferenceIds() == null || !user.getConferenceIds().contains(conferenceId)) {
                throw new AccessDeniedException("Access Denied: You are not assigned to this conference.");
            }
        }
    }
}
