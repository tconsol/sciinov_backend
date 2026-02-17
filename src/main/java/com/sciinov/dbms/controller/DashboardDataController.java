package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ExportFilterRequest;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.DashboardDataRepository;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ExcelService;
import com.sciinov.dbms.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard-data")
public class DashboardDataController {
    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @Autowired
    private ExportService exportService;

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

    /**
     * Get dashboard data with advanced filtering options
     * Supports date range, region, and country filters
     */
    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> getFilteredDashboardData(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String country) {

        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setFromSerialNo(fromSerialNo);
        filterRequest.setToSerialNo(toSerialNo);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);
        filterRequest.setRegion(region);
        filterRequest.setCountry(country);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        return ResponseEntity.ok(data);
    }

    /**
     * Get data by date range (upload date)
     * Example: Get all data uploaded in January 2026
     */
    @GetMapping("/by-date")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> getDataByDateRange(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        return ResponseEntity.ok(data);
    }

    /**
     * Get data by region
     * Example: Get all data related to USA
     */
    @GetMapping("/by-region")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> getDataByRegion(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String region) {

        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setRegion(region);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        return ResponseEntity.ok(data);
    }

    /**
     * Get data by country
     * Example: Get all data related to USA
     */
    @GetMapping("/by-country")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> getDataByCountry(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String country) {

        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setCountry(country);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        return ResponseEntity.ok(data);
    }

    /**
     * Get count of filtered data
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> getFilteredDataCount(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String country) {

        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setFromSerialNo(fromSerialNo);
        filterRequest.setToSerialNo(toSerialNo);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);
        filterRequest.setRegion(region);
        filterRequest.setCountry(country);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        return ResponseEntity.ok((long) data.size());
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
