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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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

        // Validate file format
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isSupportedExcelFormat(fileName)) {
            logger.warn("POST /api/dashboard-data/upload - Unsupported file format: {}", fileName);
            return ResponseEntity.badRequest().body(
                    "Unsupported file format. Please upload .xls, .xlsx, or .xlsm files."
            );
        }

        try {
            Map<String, Object> result = excelService.processExcelFile(file, conferenceId, dashboardMasterId);
            logger.info("POST /api/dashboard-data/upload - File processed: new={}, dup={}, time={}ms",
                    result.get("newRecordsAdded"), result.get("duplicateRecordsIgnored"), result.get("processingTimeMs"));
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("POST /api/dashboard-data/upload - Failed to process file: {} - {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to read Excel file: " + e.getMessage(),
                    "action", "Please check the file and try uploading again."
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Unexpected error during upload.";
            logger.error("POST /api/dashboard-data/upload - Upload failed: {}", msg);

            // If this is a DB insert failure (rollback already performed), return 500
            if (msg.contains("Upload failed during database insert")) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "status", "upload_failed",
                        "message", msg,
                        "action", "No data was saved. Please re-upload the file."
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", msg
            ));
        }
    }

    /**
     * Check if file is a supported Excel format
     */
    private boolean isSupportedExcelFormat(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".xlsx") ||    // Office Open XML
               lowerName.endsWith(".xls") ||     // Legacy Excel
               lowerName.endsWith(".xlsm");      // Macro-enabled XLSX
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
        long t0 = System.currentTimeMillis();
        validateAccess(conferenceId);

        int cappedSize = Math.min(size, 1000);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.ASC, "serialNo"));

        // Single DB call — fetch page
        List<DashboardData> data = dashboardDataRepository
                .findByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, pageable);

        // Only run count on first page (page=0) to avoid extra DB hit on every page turn
        long totalCount = (page == 0)
                ? dashboardDataRepository.countByConferenceIdAndDashboardMasterIdAndSerialNoBetweenAndDeletedFalse(
                        conferenceId, dashboardMasterId, fromSerialNo, toSerialNo)
                : -1; // frontend should cache the total from page=0

        logger.info("GET /api/dashboard-data - {}ms | records={} total={}",
                System.currentTimeMillis() - t0, data.size(), totalCount);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        if (totalCount >= 0) {
            response.put("totalRecords", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        }
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
        long t0 = System.currentTimeMillis();
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

        // Use DB-level pagination — do NOT load all records into memory
        List<DashboardData> pageData = exportService.getFilteredDataPaged(filterRequest, page, cappedSize);
        // Count only on first page to avoid extra DB hit on every page turn
        long totalCount = (page == 0) ? exportService.countFilteredData(filterRequest) : -1;

        logger.info("GET /api/dashboard-data/filter - {}ms | page={} records={}",
                System.currentTimeMillis() - t0, page, pageData.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageData);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        if (totalCount >= 0) {
            response.put("totalRecords", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        }
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
        long t0 = System.currentTimeMillis();
        validateAccess(conferenceId);

        int cappedSize = Math.min(size, 1000);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setStartDate(startDate);
        filterRequest.setEndDate(endDate);

        // DB-level pagination — no in-memory load
        List<DashboardData> pageData = exportService.getFilteredDataPaged(filterRequest, page, cappedSize);
        long totalCount = (page == 0) ? exportService.countFilteredData(filterRequest) : -1;

        logger.info("GET /api/dashboard-data/by-date - {}ms | records={}", System.currentTimeMillis() - t0, pageData.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", pageData);
        response.put("currentPage", page);
        response.put("pageSize", cappedSize);
        if (totalCount >= 0) {
            response.put("totalRecords", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / cappedSize));
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get ALL data by email domain extension (e.g., .com, .edu, .org) with SERIAL RANGE support
     *
     * How it works:
     *  - extension = "com"  → matches @gmail.com, @yahoo.com, @tcon.com
     *  - extension = "edu"  → matches @in.edu, @rs.edu, @university.edu
     *  - extension = ".org" → matches @example.org  (leading dot is auto-stripped)
     *
     * SMART PAGINATION (max 1000 records):
     *  - Request range 0-1000:     Returns first 1000 matching records
     *  - Request range 0-1000:     ⚠️ Warns if already downloaded
     *  - Request range 10000-20000: Returns first 1000 matching records in that range
     *  - Suggests next range automatically (e.g., "search from 1001 to 2000")
     *
     * GET /api/dashboard-data/by-domain-extension
     *   ?conferenceId=XXX
     *   &dashboardMasterId=XXX
     *   &extension=com              (or .com — both accepted)
     *   &fromSerialNo=1             (optional - starting serial number)
     *   &toSerialNo=1000            (optional - ending serial number)
     */
    @GetMapping("/by-domain-extension")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDataByDomainExtension(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String extension,
            @RequestParam(required = false) Long fromSerialNo,
            @RequestParam(required = false) Long toSerialNo) {

        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ GET /api/dashboard-data/by-domain-extension");
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Extension: {}", extension);
        logger.info("║ Conference: {}", conferenceId);
        logger.info("║ Dashboard: {}", dashboardMasterId);
        logger.info("║ Range: {} to {}",
                (fromSerialNo != null ? fromSerialNo : "not specified"),
                (toSerialNo != null ? toSerialNo : "not specified"));
        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        validateAccess(conferenceId);

        // Normalize — strip leading dot
        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) normalizedExt = normalizedExt.substring(1);

        // Use new range-aware method
        Map<String, Object> response = exportService.getDataByDomainExtensionWithRange(
                conferenceId, dashboardMasterId, normalizedExt, fromSerialNo, toSerialNo);

        // Log comprehensive summary
        logger.info("╔══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ RESPONSE SUMMARY - TLD Filter .{}", normalizedExt);
        logger.info("╠══════════════════════════════════════════════════════════════════════════════");
        logger.info("║ Requested Range: {}", response.get("requestedRange"));
        logger.info("║ Total Matching in Range: {}", response.get("totalMatchingInRange"));
        logger.info("║ Records Returned: {}", response.get("recordsReturned"));
        logger.info("║ Max Per Request: {}", response.get("maxRecordsPerRequest"));
        logger.info("║ Has More Records: {}", response.get("hasMoreRecords"));

        if (response.containsKey("rangeCoverage")) {
            logger.info("║ Range Coverage: {}", response.get("rangeCoverage"));
        }

        logger.info("╚══════════════════════════════════════════════════════════════════════════════");

        // Log warning if more records exist
        if (Boolean.TRUE.equals(response.get("hasMoreRecords"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nextRange = (Map<String, Object>) response.get("nextRangeSuggestion");
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  MORE RECORDS AVAILABLE!");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ Next Range Suggestion:");
            logger.warn("║   From Serial: {}", nextRange.get("fromSerialNo"));
            logger.warn("║   To Serial: {}", nextRange.get("toSerialNo"));
            logger.warn("║   {}", nextRange.get("message"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        // Log duplicate download warning if applicable
        if (response.containsKey("downloadWarning")) {
            logger.warn("╔══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ ⚠️  DUPLICATE DOWNLOAD WARNING");
            logger.warn("╠══════════════════════════════════════════════════════════════════════════════");
            logger.warn("║ {}", response.get("downloadWarning"));
            logger.warn("╚══════════════════════════════════════════════════════════════════════════════");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get the list of distinct domain extensions available in this conference/dashboard.
     * Use this to populate the dropdown before calling by-domain-extension.
     *
     * Example response: { "total": 3, "extensions": ["com", "edu", "org"] }
     *
     * GET /api/dashboard-data/domain-extensions
     *   ?conferenceId=XXX
     *   &dashboardMasterId=XXX
     */
    @GetMapping("/domain-extensions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Cacheable(value = "domainExtensions", key = "#conferenceId + ':' + #dashboardMasterId")
    public ResponseEntity<Map<String, Object>> getDistinctDomainExtensions(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId) {

        long t0 = System.currentTimeMillis();
        validateAccess(conferenceId);

        List<String> extensions = exportService.getDistinctDomainExtensions(conferenceId, dashboardMasterId);

        logger.info("GET /api/dashboard-data/domain-extensions - {}ms | found {} extensions",
                System.currentTimeMillis() - t0, extensions.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total",      extensions.size());
        response.put("extensions", extensions);
        return ResponseEntity.ok(response);
    }

    /**
     * Get ALL data by full email domain (e.g., emailDomain=gmail.com) — NO LIMIT
     */
    @GetMapping("/by-email-domain")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDataByEmailDomain(
            @RequestParam String conferenceId,
            @RequestParam String dashboardMasterId,
            @RequestParam String emailDomain) {
        logger.info("GET /api/dashboard-data/by-email-domain - Email domain filter: {}", emailDomain);
        validateAccess(conferenceId);

        ExportFilterRequest filterRequest = new ExportFilterRequest();
        filterRequest.setConferenceId(conferenceId);
        filterRequest.setDashboardMasterId(dashboardMasterId);
        filterRequest.setEmailDomain(emailDomain);

        // Returns ALL matching records — no limit
        List<DashboardData> allData = exportService.getFilteredData(filterRequest);

        logger.info("GET /api/dashboard-data/by-email-domain - Found {} records for domain '{}'", allData.size(), emailDomain);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("emailDomain",  emailDomain);
        response.put("totalRecords", allData.size());
        response.put("data",         allData);
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

    /**
     * Check upload progress in real-time
     * GET /api/dashboard-data/upload/progress/{uploadId}
     */
    @GetMapping("/upload/progress/{uploadId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUploadProgress(@PathVariable String uploadId) {
        logger.info("GET /api/dashboard-data/upload/progress - uploadId: {}", uploadId);
        Map<String, Object> progress = excelService.getUploadProgress(uploadId);
        return ResponseEntity.ok(progress);
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

