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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            Map<String, Object> result = excelService.processExcelFile(file, conferenceId, dashboardMasterId);
            logger.info("POST /api/dashboard-data/upload - File processed: new={}, dup={}, time={}ms",
                    result.get("newRecordsAdded"), result.get("duplicateRecordsIgnored"), result.get("processingTimeMs"));
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("POST /api/dashboard-data/upload - Failed to process file: {} - {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body("Failed to process file: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("POST /api/dashboard-data/upload - Runtime error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get data by serial number range (paginated response).
     * Supports optional page/size for frontend table pagination.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam Long fromSerialNo,
            @RequestParam Long toSerialNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        logger.info("GET /api/dashboard-data - conference: {} range: {}-{} page: {} size: {}",
                    conferenceId, fromSerialNo, toSerialNo, page, size);
        validateAccess(conferenceId);

        // Cap page size to prevent memory issues
        int cappedSize = Math.min(size, 1000);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "serialNo"));

        List<DashboardData> data = dashboardDataRepository
                .findByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, pageable);

        long totalCount = dashboardDataRepository
                .countByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);

        logger.info("GET /api/dashboard-data - Retrieved {} / {} records", data.size(), totalCount);

        // Log VIEW action
        ExportFilterRequest fr = new ExportFilterRequest();
        fr.setConferenceId(conferenceId);
        fr.setDashboardMasterId(dashboardMasterId);
        fr.setFromSerialNo(fromSerialNo);
        fr.setToSerialNo(toSerialNo);
        exportService.logViewAction(fr, data.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        response.put("totalRecords", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        response.put("fromSerialNo", fromSerialNo);
        response.put("toSerialNo", toSerialNo);
        return ResponseEntity.ok(response);
    }

    /**
     * Get dashboard data with advanced filtering options (paginated)
     */
    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFilteredDashboardData(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String emailDomain,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        logger.info("GET /api/dashboard-data/filter - conference: {} dashboard: {} page: {}", conferenceId, dashboardMasterId, page);
        validateAccess(conferenceId);

        int cappedSize = Math.min(size, 1000);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setFromSerialNo(fromSerialNo);
        filterRequest.setToSerialNo(toSerialNo);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);
        filterRequest.setEmailDomain(emailDomain);

        List<DashboardData> allData = exportService.getFilteredData(filterRequest);
        long totalCount = allData.size();

        // Apply pagination in memory for filtered results
        int fromIndex = page * cappedSize;
        int toIndex = Math.min(fromIndex + cappedSize, (int) totalCount);
        List<DashboardData> pageData = fromIndex < totalCount ? allData.subList(fromIndex, toIndex) : List.of();

        logger.info("GET /api/dashboard-data/filter - Total: {}, Page: {} ({} records)", totalCount, page, pageData.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageData);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        response.put("totalRecords", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        return ResponseEntity.ok(response);
    }

    /**
     * Get data by date range (upload date)
     */
    @GetMapping("/by-date")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDataByDateRange(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        logger.info("GET /api/dashboard-data/by-date - Date range filter: {} to {}", startDate, endDate);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);

        List<DashboardData> allData = exportService.getFilteredData(filterRequest);
        long totalCount = allData.size();
        int cappedSize = Math.min(size, 1000);
        int fromIndex = page * cappedSize;
        int toIndex = Math.min(fromIndex + cappedSize, (int) totalCount);
        List<DashboardData> pageData = fromIndex < totalCount ? allData.subList(fromIndex, toIndex) : List.of();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageData);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        response.put("totalRecords", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        return ResponseEntity.ok(response);
    }

    /**
     * Get data by email domain (e.g., emailDomain=gmail.com)
     */
    @GetMapping("/by-email-domain")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDataByEmailDomain(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String emailDomain,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        logger.info("GET /api/dashboard-data/by-email-domain - Email domain filter: {}", emailDomain);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setEmailDomain(emailDomain);

        List<DashboardData> allData = exportService.getFilteredData(filterRequest);
        long totalCount = allData.size();
        int cappedSize = Math.min(size, 1000);
        int fromIndex = page * cappedSize;
        int toIndex = Math.min(fromIndex + cappedSize, (int) totalCount);
        List<DashboardData> pageData = fromIndex < totalCount ? allData.subList(fromIndex, toIndex) : List.of();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageData);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        response.put("totalRecords", totalCount);
        response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        return ResponseEntity.ok(response);
    }

    /**
     * Get count of records (efficient DB count query, no data fetch)
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getFilteredDataCount(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String emailDomain) {
        logger.info("GET /api/dashboard-data/count - Counting filtered records");
        validateAccess(conferenceId);

        // Use efficient count query via MongoTemplate in ExportService
        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setFromSerialNo(fromSerialNo);
        filterRequest.setToSerialNo(toSerialNo);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);
        filterRequest.setEmailDomain(emailDomain);

        long count = exportService.countFilteredData(filterRequest);
        logger.info("GET /api/dashboard-data/count - Total count: {}", count);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", count);
        response.put("conferenceId", conferenceId);
        response.put("dashboardMasterId", dashboardMasterId);
        return ResponseEntity.ok(response);
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

