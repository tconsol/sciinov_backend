package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ExportFilterRequest;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.DashboardDataRepository;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ExcelService;
import com.sciinov.dbms.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(DashboardDataController.class);

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
        logger.info("POST /api/dashboard-data/upload - Uploading file: {} for conference: {} dashboard: {}",
                    file.getOriginalFilename(), conferenceId, dashboardMasterId);
        try {
            // Access check is done inside ExcelService
            excelService.processExcelFile(file, conferenceId, dashboardMasterId);
            logger.info("POST /api/dashboard-data/upload - File uploaded successfully: {}", file.getOriginalFilename());
            return ResponseEntity.ok("File uploaded successfully!");
        } catch (IOException e) {
            logger.error("POST /api/dashboard-data/upload - Failed to process file: {} - {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body("Failed to process file: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("POST /api/dashboard-data/upload - Runtime error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public List<DashboardData> getDashboardData(@RequestParam String conferenceId,
                                                @RequestParam String dashboardMasterId,
                                                @RequestParam Long fromSerialNo,
                                                @RequestParam Long toSerialNo) {
        logger.info("GET /api/dashboard-data - Retrieving data for conference: {} dashboard: {} range: {}-{}",
                    conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);
        validateAccess(conferenceId);
        List<DashboardData> data = dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));
        logger.info("GET /api/dashboard-data - Retrieved {} records", data.size());
        return data;
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
        logger.info("GET /api/dashboard-data/filter - Filtering data for conference: {} dashboard: {}", conferenceId, dashboardMasterId);
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
        logger.info("GET /api/dashboard-data/filter - Retrieved {} filtered records", data.size());
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
        logger.info("GET /api/dashboard-data/by-date - Date range filter: {} to {}", startDate, endDate);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        logger.info("GET /api/dashboard-data/by-date - Retrieved {} records", data.size());
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
        logger.info("GET /api/dashboard-data/by-region - Region filter: {}", region);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setRegion(region);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        logger.info("GET /api/dashboard-data/by-region - Retrieved {} records", data.size());
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
        logger.info("GET /api/dashboard-data/by-country - Country filter: {}", country);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setCountry(country);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        logger.info("GET /api/dashboard-data/by-country - Retrieved {} records", data.size());
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
        logger.info("GET /api/dashboard-data/count - Counting filtered records");
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
        long count = data.size();
        logger.info("GET /api/dashboard-data/count - Total count: {}", count);
        return ResponseEntity.ok(count);
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
