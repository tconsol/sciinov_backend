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
import java.util.Map;

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

    /** Advanced Excel export — supports date range and emailDomain filters */
    @GetMapping("/excel/advanced")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcelAdvanced(HttpServletResponse response,
                                       @RequestParam String conferenceId,
                                       @RequestParam String dashboardMasterId,
                                       @RequestParam(required = false) Long fromSerialNo,
                                       @RequestParam(required = false) Long toSerialNo,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(required = false) String emailDomain) throws IOException {
        logger.info("GET /api/export/excel/advanced - conferenceId: {}", conferenceId);
        validateAccess(conferenceId);
        exportService.exportToExcelWithFilters(response,
                buildFilterRequest(conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, startDate, endDate, emailDomain));
        logger.info("GET /api/export/excel/advanced - completed");
    }

    /** Advanced PDF export — supports date range and emailDomain filters */
    @GetMapping("/pdf/advanced")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdfAdvanced(HttpServletResponse response,
                                     @RequestParam String conferenceId,
                                     @RequestParam String dashboardMasterId,
                                     @RequestParam(required = false) Long fromSerialNo,
                                     @RequestParam(required = false) Long toSerialNo,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     @RequestParam(required = false) String emailDomain) throws IOException {
        logger.info("GET /api/export/pdf/advanced - conferenceId: {}", conferenceId);
        validateAccess(conferenceId);
        exportService.exportToPdfWithFilters(response,
                buildFilterRequest(conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, startDate, endDate, emailDomain));
        logger.info("GET /api/export/pdf/advanced - completed");
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

    /** Preview filtered data before export */
    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DashboardData>> previewFilteredData(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String emailDomain) {
        logger.info("GET /api/export/preview - conferenceId: {}", conferenceId);
        validateAccess(conferenceId);
        List<DashboardData> data = exportService.getFilteredData(
                buildFilterRequest(conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, startDate, endDate, emailDomain));
        logger.info("GET /api/export/preview - count: {}", data.size());
        return ResponseEntity.ok(data);
    }

    /** Get distinct email domains for dropdown/filter options */
    @GetMapping("/email-domains")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<String>> getDistinctEmailDomains(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId) {
        logger.info("GET /api/export/email-domains - conferenceId: {}", conferenceId);
        validateAccess(conferenceId);
        List<String> domains = exportService.getDistinctEmailDomains(conferenceId, dashboardMasterId);
        logger.info("GET /api/export/email-domains - count: {}", domains.size());
        return ResponseEntity.ok(domains);
    }

    /**
     * Download Excel filtered by domain extension (e.g., .com, .edu, .org) with SERIAL RANGE support
     *
     * SMART PAGINATION (max 1000 records):
     *  - Request range 0-1000:     Downloads first 1000 matching records
     *  - Request range 0-1000:     ⚠️ Logs warning if already downloaded
     *  - Request range 10000-20000: Downloads first 1000 matching records in that range
     *  - Logs suggest next range (e.g., "search from 1001 to 2000")
     *
     * GET /api/export/excel/by-extension?conferenceId=X&dashboardMasterId=X&extension=com
     *     &fromSerialNo=1&toSerialNo=1000 (optional)
     */
    @GetMapping("/excel/by-extension")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToExcelByExtension(
            HttpServletResponse response,
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String extension,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo) throws IOException {

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ GET /api/export/excel/by-extension - DOWNLOAD REQUEST");
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Extension: {}", extension);
        logger.info("║ Range: {} to {}",
                (fromSerialNo != null ? fromSerialNo : "not specified"),
                (toSerialNo != null ? toSerialNo : "not specified"));
        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        validateAccess(conferenceId);

        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) normalizedExt = normalizedExt.substring(1);

        // Get data with range support
        Map<String, Object> result = exportService.getDataByDomainExtensionWithRange(
                conferenceId, dashboardMasterId, normalizedExt, fromSerialNo, toSerialNo);

        @SuppressWarnings("unchecked")
        List<DashboardData> data = (List<DashboardData>) result.get("data");
        int recordsReturned = (int) result.get("recordsReturned");
        long totalMatching = (long) result.get("totalMatchingInRange");

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ EXCEL EXPORT - TLD Filter .{}", normalizedExt);
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Total Matching in Range: {}", totalMatching);
        logger.info("║ Records Being Exported: {}", recordsReturned);
        logger.info("║ Percentage Exported: {:.2f}%", (recordsReturned * 100.0 / totalMatching));
        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        // Log warnings and suggestions
        if (Boolean.TRUE.equals(result.get("hasMoreRecords"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nextRange = (Map<String, Object>) result.get("nextRangeSuggestion");
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  MORE RECORDS AVAILABLE!");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ Exported: {} out of {} records", recordsReturned, totalMatching);
            logger.warn("║ Remaining: {} records not exported", (totalMatching - recordsReturned));
            logger.warn("║ Next Range: {} to {}", nextRange.get("fromSerialNo"), nextRange.get("toSerialNo"));
            logger.warn("║ {}", nextRange.get("message"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        if (result.containsKey("downloadWarning")) {
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  DUPLICATE DOWNLOAD WARNING");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ {}", result.get("downloadWarning"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        if (result.containsKey("coverageMessage")) {
            logger.info("╔══════════════════════════════════════════════════════════════════════════════");
            logger.info("║ 📊 RANGE COVERAGE");
            logger.info("╠══════════════════════════════════════════════════════════════════════════════");
            logger.info("║ {}", result.get("coverageMessage"));
            logger.info("╚══════════════════════════════════════════════════════════════════════════════");
        }

        // Export to Excel
        ExportFilterRequest fr = new ExportFilterRequest();
        fr.setConferenceId(conferenceId);
        fr.setDashboardMasterId(dashboardMasterId);
        fr.setEmailDomain("*." + normalizedExt);
        fr.setFromSerialNo(fromSerialNo);
        fr.setToSerialNo(toSerialNo);

        logger.info("📥 Generating Excel file with {} records...", recordsReturned);
        exportService.exportToExcelWithData(response, data, fr);
        logger.info("✅ Excel file generated and download started");
    }

    /**
     * Download PDF filtered by domain extension (e.g., .com, .edu, .org) with SERIAL RANGE support
     *
     * SMART PAGINATION (max 1000 records):
     *  - Request range 0-1000:     Downloads first 1000 matching records
     *  - Request range 0-1000:     ⚠️ Logs warning if already downloaded
     *  - Request range 10000-20000: Downloads first 1000 matching records in that range
     *  - Logs suggest next range (e.g., "search from 1001 to 2000")
     *
     * GET /api/export/pdf/by-extension?conferenceId=X&dashboardMasterId=X&extension=edu
     *     &fromSerialNo=1&toSerialNo=1000 (optional)
     */
    @GetMapping("/pdf/by-extension")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public void exportToPdfByExtension(
            HttpServletResponse response,
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String extension,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo) throws IOException {

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ GET /api/export/pdf/by-extension - DOWNLOAD REQUEST");
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Extension: {}", extension);
        logger.info("║ Range: {} to {}",
                (fromSerialNo != null ? fromSerialNo : "not specified"),
                (toSerialNo != null ? toSerialNo : "not specified"));
        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        validateAccess(conferenceId);

        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) normalizedExt = normalizedExt.substring(1);

        // Get data with range support
        Map<String, Object> result = exportService.getDataByDomainExtensionWithRange(
                conferenceId, dashboardMasterId, normalizedExt, fromSerialNo, toSerialNo);

        @SuppressWarnings("unchecked")
        List<DashboardData> data = (List<DashboardData>) result.get("data");
        int recordsReturned = (int) result.get("recordsReturned");
        long totalMatching = (long) result.get("totalMatchingInRange");

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ PDF EXPORT - TLD Filter .{}", normalizedExt);
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Total Matching in Range: {}", totalMatching);
        logger.info("║ Records Being Exported: {}", recordsReturned);
        logger.info("║ Percentage Exported: {:.2f}%", (recordsReturned * 100.0 / totalMatching));
        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        // Log warnings and suggestions
        if (Boolean.TRUE.equals(result.get("hasMoreRecords"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nextRange = (Map<String, Object>) result.get("nextRangeSuggestion");
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  MORE RECORDS AVAILABLE!");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ Exported: {} out of {} records", recordsReturned, totalMatching);
            logger.warn("║ Remaining: {} records not exported", (totalMatching - recordsReturned));
            logger.warn("║ Next Range: {} to {}", nextRange.get("fromSerialNo"), nextRange.get("toSerialNo"));
            logger.warn("║ {}", nextRange.get("message"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        if (result.containsKey("downloadWarning")) {
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  DUPLICATE DOWNLOAD WARNING");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ {}", result.get("downloadWarning"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        if (result.containsKey("coverageMessage")) {
            logger.info("╔══════════════════════════════════════════════════════════════════════════════");
            logger.info("║ 📊 RANGE COVERAGE");
            logger.info("╠══════════════════════════════════════════════════════════════════════════════");
            logger.info("║ {}", result.get("coverageMessage"));
            logger.info("╚══════════════════════════════════════════════════════════════════════════════");
        }

        // Export to PDF
        ExportFilterRequest fr = new ExportFilterRequest();
        fr.setConferenceId(conferenceId);
        fr.setDashboardMasterId(dashboardMasterId);
        fr.setEmailDomain("*." + normalizedExt);
        fr.setFromSerialNo(fromSerialNo);
        fr.setToSerialNo(toSerialNo);

        logger.info("📥 Generating PDF file with {} records...", recordsReturned);
        exportService.exportToPdfWithData(response, data, fr);
        logger.info("✅ PDF file generated and download started");
    }

    private ExportFilterRequest buildFilterRequest(String conferenceId, String dashboardMasterId,
                                                    Long fromSerialNo, Long toSerialNo,
                                                    LocalDate startDate, LocalDate endDate,
                                                    String emailDomain) {
        ExportFilterRequest request = new ExportFilterRequest();
        request.setConferenceId(conferenceId);
        request.setDashboardMasterId(dashboardMasterId);
        request.setFromSerialNo(fromSerialNo);
        request.setToSerialNo(toSerialNo);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setEmailDomain(emailDomain);
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
