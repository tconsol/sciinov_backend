package com.subbarao.backend.controller;

import com.subbarao.backend.entity.DashboardData;
import com.subbarao.backend.entity.User;
import com.subbarao.backend.repository.DashboardDataRepository;
import com.subbarao.backend.security.UserDetailsImpl;
import com.subbarao.backend.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard-data")
public class DashboardDataController {
    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file,
                                         @RequestParam("conferenceId") String conferenceId,
                                         @RequestParam("dashboardMasterId") String dashboardMasterId) {
        try {
            // Access check is done inside ExcelService
            excelService.processExcelFile(file, conferenceId, dashboardMasterId);
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to process file: " + e.getMessage());
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public List<DashboardData> getDashboardData(@RequestParam String conferenceId,
                                                @RequestParam String dashboardMasterId,
                                                @RequestParam Long fromSerialNo,
                                                @RequestParam Long toSerialNo) {
        validateAccess(conferenceId);
        return dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));
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
