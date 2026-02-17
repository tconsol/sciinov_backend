package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ExportFilterRequest;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/export")
public class ExportController {
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    @Autowired
    private ExportService exportService;

    // Original endpoints - kept for backward compatibility
    @GetMapping("/excel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcel(HttpServletResponse response,
                              @RequestParam String conferenceId,
                              @RequestParam String dashboardMasterId,
                              @RequestParam Long fromSerialNo,
                              @RequestParam Long toSerialNo) throws IOException {
        logger.info("GET /api/export/excel - Exporting to Excel: range {}-{}", fromSerialNo, toSerialNo);
        validateAccess(conferenceId);
        exportService.exportToExcel(response, conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);
        logger.info("GET /api/export/excel - Excel export completed");
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdf(HttpServletResponse response,
                            @RequestParam String conferenceId,
                            @RequestParam String dashboardMasterId,
                            @RequestParam Long fromSerialNo,
                            @RequestParam Long toSerialNo) throws IOException {
        logger.info("GET /api/export/pdf - Exporting to PDF: range {}-{}", fromSerialNo, toSerialNo);
        validateAccess(conferenceId);
        exportService.exportToPdf(response, conferenceId, dashboardMasterId, fromSerialNo, toSerialNo);
        logger.info("GET /api/export/pdf - PDF export completed");
    }

    /**
     * Advanced Excel export with date and region filtering
     * Supports: date range, region, country filters
     */
    @GetMapping("/excel/advanced")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcelAdvanced(HttpServletResponse response,
                                       @RequestParam String conferenceId,
                                       @RequestParam String dashboardMasterId,
                                       @RequestParam(required = false) Long fromSerialNo,
                                       @RequestParam(required = false) Long toSerialNo,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(required = false) String region,
                                       @RequestParam(required = false) String country) throws IOException {
        logger.info("GET /api/export/excel/advanced - Exporting to Excel with filters: {}", conferenceId);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = buildFilterRequest(conferenceId, dashboardMasterId,
                fromSerialNo, toSerialNo, startDate, endDate, region, country);

        exportService.exportToExcelWithFilters(response, filterRequest);
        logger.info("GET /api/export/excel/advanced - Excel export with filters completed");
    }

    /**
     * Advanced PDF export with date and region filtering
     * Supports: date range, region, country filters
     */
    @GetMapping("/pdf/advanced")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdfAdvanced(HttpServletResponse response,
                                     @RequestParam String conferenceId,
                                     @RequestParam String dashboardMasterId,
                                     @RequestParam(required = false) Long fromSerialNo,
                                     @RequestParam(required = false) Long toSerialNo,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     @RequestParam(required = false) String region,
                                     @RequestParam(required = false) String country) throws IOException {
        logger.info("GET /api/export/pdf/advanced - Exporting to PDF with filters: {}", conferenceId);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = buildFilterRequest(conferenceId, dashboardMasterId,
                fromSerialNo, toSerialNo, startDate, endDate, region, country);

        exportService.exportToPdfWithFilters(response, filterRequest);
        logger.info("GET /api/export/pdf/advanced - PDF export with filters completed");
    }

    /**
     * POST endpoint for advanced Excel export with filters in request body
     */
    @PostMapping("/excel/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcelWithFilter(HttpServletResponse response,
                                         @RequestBody ExportFilterRequest filterRequest) throws IOException {
        logger.info("POST /api/export/excel/filter - Exporting to Excel with filter: {}", filterRequest);
        validateAccess(filterRequest.getConferenceId());
        exportService.exportToExcelWithFilters(response, filterRequest);
        logger.info("POST /api/export/excel/filter - Excel export with filter completed");
    }

    /**
     * POST endpoint for advanced PDF export with filters in request body
     */
    @PostMapping("/pdf/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdfWithFilter(HttpServletResponse response,
                                       @RequestBody ExportFilterRequest filterRequest) throws IOException {
        logger.info("POST /api/export/pdf/filter - Exporting to PDF with filter: {}", filterRequest);
        validateAccess(filterRequest.getConferenceId());
        exportService.exportToPdfWithFilters(response, filterRequest);
        logger.info("POST /api/export/pdf/filter - PDF export with filter completed");
    }

    /**
     * Preview filtered data before export
     */
    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> previewFilteredData(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String country) {
        logger.info("GET /api/export/preview - Previewing filtered data: {}", conferenceId);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = buildFilterRequest(conferenceId, dashboardMasterId,
                fromSerialNo, toSerialNo, startDate, endDate, region, country);

        List<DashboardData> data = exportService.getFilteredData(filterRequest);
        logger.info("GET /api/export/preview - Previewed data count: {}", data.size());
        return ResponseEntity.ok(data);
    }

    /**
     * Get distinct regions for dropdown/filter options
     */
    @GetMapping("/regions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<String>> getDistinctRegions(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId) {
        logger.info("GET /api/export/regions - Fetching distinct regions: {}", conferenceId);
        validateAccess(conferenceId);
        List<String> regions = exportService.getDistinctRegions(conferenceId, dashboardMasterId);
        logger.info("GET /api/export/regions - Distinct regions count: {}", regions.size());
        return ResponseEntity.ok(regions);
    }

    /**
     * Get distinct countries for dropdown/filter options
     */
    @GetMapping("/countries")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<String>> getDistinctCountries(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId) {
        logger.info("GET /api/export/countries - Fetching distinct countries: {}", conferenceId);
        validateAccess(conferenceId);
        List<String> countries = exportService.getDistinctCountries(conferenceId, dashboardMasterId);
        logger.info("GET /api/export/countries - Distinct countries count: {}", countries.size());
        return ResponseEntity.ok(countries);
    }

    private ExportFilterRequest buildFilterRequest(String conferenceId, String dashboardMasterId,
                                                    Long fromSerialNo, Long toSerialNo,
                                                    LocalDate startDate, LocalDate endDate,
                                                    String region, String country) {
        ExportFilterRequest request = new ExportFilterRequest();
        request.setConferenceId(conferenceId);
        request.setDashboardMasterId(dashboardMasterId);
        request.setFromSerialNo(fromSerialNo);
        request.setToSerialNo(toSerialNo);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setRegion(region);
        request.setCountry(country);
        return request;
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
