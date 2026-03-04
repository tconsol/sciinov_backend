package com.sciinov.dbms.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sciinov.dbms.dto.ExportFilterRequest;
import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.repository.AdminActivityLogRepository;
import com.sciinov.dbms.repository.DashboardDataRepository;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AnalyticsService analyticsService;

    // Original method - kept for backward compatibility
    public void exportToExcel(HttpServletResponse response, String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo) throws IOException {
        List<DashboardData> dataList = dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));

        ExportFilterRequest fr = new ExportFilterRequest();
        fr.setConferenceId(conferenceId);
        fr.setDashboardMasterId(dashboardMasterId);
        fr.setFromSerialNo(fromSerialNo);
        fr.setToSerialNo(toSerialNo);
        generateExcelFile(response, dataList, fr, AdminActivityLog.ActionType.DOWNLOAD_EXCEL);
    }

    // Original method - kept for backward compatibility
    public void exportToPdf(HttpServletResponse response, String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo) throws IOException {
        List<DashboardData> dataList = dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));

        ExportFilterRequest fr = new ExportFilterRequest();
        fr.setConferenceId(conferenceId);
        fr.setDashboardMasterId(dashboardMasterId);
        fr.setFromSerialNo(fromSerialNo);
        fr.setToSerialNo(toSerialNo);
        generatePdfFile(response, dataList, fr, AdminActivityLog.ActionType.DOWNLOAD_PDF);
    }

    /**
     * Advanced export to Excel with date and email domain filtering
     */
    public void exportToExcelWithFilters(HttpServletResponse response, ExportFilterRequest filterRequest) throws IOException {
        List<DashboardData> dataList = getFilteredData(filterRequest);
        generateExcelFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_EXCEL);
    }

    /**
     * Advanced export to PDF with date and email domain filtering
     */
    public void exportToPdfWithFilters(HttpServletResponse response, ExportFilterRequest filterRequest) throws IOException {
        List<DashboardData> dataList = getFilteredData(filterRequest);
        generatePdfFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_PDF);
    }

    /**
     * Export to Excel from a pre-fetched data list (used for domain-extension exports)
     */
    public void exportToExcelWithData(HttpServletResponse response, List<DashboardData> dataList,
                                      ExportFilterRequest filterRequest) throws IOException {
        generateExcelFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_EXCEL);
    }

    /**
     * Export to PDF from a pre-fetched data list (used for domain-extension exports)
     */
    public void exportToPdfWithData(HttpServletResponse response, List<DashboardData> dataList,
                                    ExportFilterRequest filterRequest) throws IOException {
        generatePdfFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_PDF);
    }

    /**
     * Get filtered data based on multiple criteria using dynamic query building.
     * NOTE: No sort to avoid MongoDB 32MB in-memory sort limit on large collections.
     */
    public List<DashboardData> getFilteredData(ExportFilterRequest filterRequest) {
        Query query = buildFilterQuery(filterRequest);
        // No sort — avoids exceeding MongoDB's 32MB in-memory sort limit
        return mongoTemplate.find(query, DashboardData.class);
    }

    /**
     * DB-level pagination — skip/limit pushed to MongoDB, never load all into memory.
     * Replaces the old pattern of: load all → subList in Java.
     */
    public List<DashboardData> getFilteredDataPaged(ExportFilterRequest filterRequest, int page, int size) {
        Query query = buildFilterQuery(filterRequest);
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "serialNo"));
        query.skip((long) page * size).limit(size);
        return mongoTemplate.find(query, DashboardData.class);
    }

    /**
     * Efficient count of filtered data using MongoDB count query (no data fetch).
     */
    public long countFilteredData(ExportFilterRequest filterRequest) {
        Query query = buildFilterQuery(filterRequest);
        return mongoTemplate.count(query, DashboardData.class);
    }

    /**
     * Build a reusable Criteria query from filter request.
     */
    private Query buildFilterQuery(ExportFilterRequest filterRequest) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(filterRequest.getConferenceId()));
        query.addCriteria(Criteria.where("dashboardMasterId").is(filterRequest.getDashboardMasterId()));
        query.addCriteria(Criteria.where("deleted").is(false));

        if (filterRequest.getFromSerialNo() != null && filterRequest.getToSerialNo() != null) {
            query.addCriteria(Criteria.where("serialNo")
                    .gte(filterRequest.getFromSerialNo())
                    .lte(filterRequest.getToSerialNo()));
        }

        if (filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
            query.addCriteria(Criteria.where("createdAt")
                    .gte(filterRequest.getStartDate().atStartOfDay())
                    .lte(filterRequest.getEndDate().atTime(LocalTime.MAX)));
        } else if (filterRequest.getStartDate() != null) {
            query.addCriteria(Criteria.where("createdAt").gte(filterRequest.getStartDate().atStartOfDay()));
        } else if (filterRequest.getEndDate() != null) {
            query.addCriteria(Criteria.where("createdAt").lte(filterRequest.getEndDate().atTime(LocalTime.MAX)));
        }

        if (filterRequest.getEmailDomain() != null && !filterRequest.getEmailDomain().isEmpty()) {
            String domainPattern = "@" + filterRequest.getEmailDomain().trim().toLowerCase();
            query.addCriteria(Criteria.where("email").regex(domainPattern, "i"));
        }
        return query;
    }


    /**
     * Get list of distinct email domains (e.g., "gmail.com") for a conference/dashboard
     */
    public List<String> getDistinctEmailDomains(String conferenceId, String dashboardMasterId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(Criteria.where("email").ne(null));

        List<String> emails = mongoTemplate.findDistinct(query, "email", DashboardData.class, String.class);
        return emails.stream()
                .filter(e -> e != null && e.contains("@"))
                .map(e -> e.substring(e.indexOf('@') + 1).toLowerCase())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get distinct domain EXTENSIONS only (e.g., "com", "edu", "org", "in")
     * from all emails in the conference/dashboard.
     * Example: @gmail.com → "com", @in.edu → "edu", @rs.edu → "edu"
     */
    public List<String> getDistinctDomainExtensions(String conferenceId, String dashboardMasterId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(Criteria.where("email").ne(null));

        List<String> emails = mongoTemplate.findDistinct(query, "email", DashboardData.class, String.class);
        return emails.stream()
                .filter(e -> e != null && e.contains("@") && e.contains("."))
                .map(e -> {
                    String domain = e.substring(e.indexOf('@') + 1).toLowerCase();
                    // Get the last part after the final dot (e.g., "gmail.com" → "com", "in.edu" → "edu")
                    return domain.substring(domain.lastIndexOf('.') + 1);
                })
                .filter(ext -> !ext.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all dashboard data where email ends with the given domain extension.
     * Example: extension "com" matches @gmail.com, @tcon.com, @yahoo.com
     * Example: extension "edu" matches @in.edu, @rs.edu, @university.edu
     * NOTE: No sort applied here to avoid MongoDB 32MB memory sort limit on large collections.
     */
    public List<DashboardData> getDataByDomainExtension(String conferenceId, String dashboardMasterId, String extension) {
        // Normalize extension (remove leading dot if present)
        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) {
            normalizedExt = normalizedExt.substring(1);
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        // Match emails that end with .<extension> — anchored at end with $
        query.addCriteria(Criteria.where("email").regex("\\." + normalizedExt + "$", "i"));
        // No sort — avoids exceeding MongoDB's 32MB in-memory sort limit on large datasets

        return mongoTemplate.find(query, DashboardData.class);
    }

    /**
     * Get dashboard data by domain extension with serial range filtering.
     * Supports smart pagination with max 1000 records per request.
     *
     * Features:
     * - Serial range filtering (fromSerialNo to toSerialNo)
     * - Max 1000 records per response
     * - Returns metadata about range coverage and next suggested range
     *
     * @param conferenceId Conference ID
     * @param dashboardMasterId Dashboard Master ID
     * @param extension Domain extension (e.g., "com", "edu")
     * @param fromSerialNo Starting serial number (inclusive, optional)
     * @param toSerialNo Ending serial number (inclusive, optional)
     * @return Map containing data, metadata, and next range suggestion
     */
    public Map<String, Object> getDataByDomainExtensionWithRange(
            String conferenceId, String dashboardMasterId, String extension,
            Long fromSerialNo, Long toSerialNo) {

        // Normalize extension
        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) {
            normalizedExt = normalizedExt.substring(1);
        }

        // ═══════════════════════════════════════════════════════════════════
        logger.info("╔═══════════════════════════════════════════════════════════════");
        logger.info("║ TLD RANGE FILTERING REQUEST");
        logger.info("╠═══════════════════════════════════════════════════════════════");
        logger.info("║ Extension: .{}", normalizedExt);
        logger.info("║ Conference ID: {}", conferenceId);
        logger.info("║ Dashboard ID: {}", dashboardMasterId);
        logger.info("║ Range: {}", buildRangeDescription(fromSerialNo, toSerialNo));
        if (fromSerialNo != null) logger.info("║ From Serial: {}", fromSerialNo);
        if (toSerialNo != null) logger.info("║ To Serial: {}", toSerialNo);
        logger.info("╚═══════════════════════════════════════════════════════════════");

        // Build base query for TLD matching
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(Criteria.where("email").regex("\\." + normalizedExt + "$", "i"));

        // Add serial range filtering if provided
        if (fromSerialNo != null && toSerialNo != null) {
            query.addCriteria(Criteria.where("serialNo").gte(fromSerialNo).lte(toSerialNo));
            logger.info("┌─────────────────────────────────────────────────────────────────");
            logger.info("│ [TLD-Range] FILTER: Serial range {} to {} applied", fromSerialNo, toSerialNo);
        } else if (fromSerialNo != null) {
            query.addCriteria(Criteria.where("serialNo").gte(fromSerialNo));
            logger.info("┌─────────────────────────────────────────────────────────────────");
            logger.info("│ [TLD-Range] FILTER: From serial {} onwards", fromSerialNo);
        } else if (toSerialNo != null) {
            query.addCriteria(Criteria.where("serialNo").lte(toSerialNo));
            logger.info("┌─────────────────────────────────────────────────────────────────");
            logger.info("│ [TLD-Range] FILTER: Up to serial {}", toSerialNo);
        } else {
            logger.info("┌─────────────────────────────────────────────────────────────────");
            logger.info("│ [TLD-Range] FILTER: No range specified, fetching first {} records", 1000);
        }

        // Sort by serialNo for consistent pagination
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "serialNo"));

        // Get total count matching the criteria (before limiting to 1000)
        long totalMatchingInRange = mongoTemplate.count(query, DashboardData.class);
        logger.info("┌─────────────────────────────────────────────────────────────────");
        logger.info("│ [TLD-Range] Total records matching TLD '.{}' in range: {}", normalizedExt, totalMatchingInRange);

        // Limit to 1000 records maximum
        int maxRecords = 1000;
        query.limit(maxRecords);

        // Fetch the data
        List<DashboardData> data = mongoTemplate.find(query, DashboardData.class);
        int actualRecordsReturned = data.size();

        logger.info("│ [TLD-Range] Records returned: {} (max allowed per request: {})", actualRecordsReturned, maxRecords);
        logger.info("│ [TLD-Range] Percentage fetched: {:.2f}%", (actualRecordsReturned * 100.0 / totalMatchingInRange));

        // Build response metadata
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("extension", "." + normalizedExt);
        response.put("requestedRange", buildRangeDescription(fromSerialNo, toSerialNo));
        response.put("totalMatchingInRange", totalMatchingInRange);
        response.put("recordsReturned", actualRecordsReturned);
        response.put("maxRecordsPerRequest", maxRecords);
        response.put("data", data);

        // Determine if there are more records beyond the 1000 limit
        boolean hasMoreRecords = totalMatchingInRange > maxRecords;
        response.put("hasMoreRecords", hasMoreRecords);

        // Calculate next range suggestion
        if (hasMoreRecords && !data.isEmpty()) {
            long lastSerialReturned = data.get(data.size() - 1).getSerialNo();
            long firstSerialReturned = data.get(0).getSerialNo();
            long suggestedNextFrom = lastSerialReturned + 1;
            long suggestedNextTo = (toSerialNo != null) ? toSerialNo : suggestedNextFrom + 999;

            Map<String, Object> nextRange = new LinkedHashMap<>();
            nextRange.put("fromSerialNo", suggestedNextFrom);
            nextRange.put("toSerialNo", suggestedNextTo);
            nextRange.put("message", String.format(
                    "You received the first %d records. To get the next batch, search from serial %d to %d",
                    maxRecords, suggestedNextFrom, suggestedNextTo));

            response.put("nextRangeSuggestion", nextRange);

            logger.info("│ [TLD-Range] ⚠️  MORE RECORDS AVAILABLE!");
            logger.info("│ [TLD-Range] Serial range returned: {} to {}", firstSerialReturned, lastSerialReturned);
            logger.info("│ [TLD-Range] Records remaining: {}", (totalMatchingInRange - actualRecordsReturned));
            logger.info("│ [TLD-Range] 💡 NEXT RANGE SUGGESTION:");
            logger.info("│ [TLD-Range]    fromSerialNo: {}", suggestedNextFrom);
            logger.info("│ [TLD-Range]    toSerialNo: {}", suggestedNextTo);
            logger.info("│ [TLD-Range]    Message: \"Search from serial {} to {}\"", suggestedNextFrom, suggestedNextTo);
        } else if (!data.isEmpty()) {
            long firstSerialReturned = data.get(0).getSerialNo();
            long lastSerialReturned = data.get(data.size() - 1).getSerialNo();
            logger.info("│ [TLD-Range] ✅ ALL RECORDS RETURNED");
            logger.info("│ [TLD-Range] Serial range: {} to {}", firstSerialReturned, lastSerialReturned);
            logger.info("│ [TLD-Range] No more records to fetch");
        }

        // Check if this range was already fully covered
        if (fromSerialNo != null && toSerialNo != null && actualRecordsReturned > 0) {
            long firstSerialReturned = data.get(0).getSerialNo();
            long lastSerialReturned = data.get(data.size() - 1).getSerialNo();

            boolean rangeFullyCovered = (firstSerialReturned == fromSerialNo || firstSerialReturned > fromSerialNo)
                    && lastSerialReturned <= toSerialNo
                    && !hasMoreRecords;

            if (rangeFullyCovered) {
                response.put("rangeCoverage", "complete");
                response.put("coverageMessage", String.format(
                        "Range %d to %d is fully covered. All %d matching records returned.",
                        fromSerialNo, toSerialNo, actualRecordsReturned));
                logger.info("│ [TLD-Range] 📊 COVERAGE STATUS: COMPLETE ✅");
                logger.info("│ [TLD-Range] Requested range: {} to {}", fromSerialNo, toSerialNo);
                logger.info("│ [TLD-Range] All {} matching records returned", actualRecordsReturned);
                logger.info("│ [TLD-Range] ✅ Range fully covered");
            } else if (hasMoreRecords) {
                response.put("rangeCoverage", "partial");
                response.put("coverageMessage", String.format(
                        "⚠️ Partial coverage: Returned records %d to %d out of requested range %d to %d. %d more records exist.",
                        firstSerialReturned, lastSerialReturned, fromSerialNo, toSerialNo,
                        (totalMatchingInRange - actualRecordsReturned)));
                logger.warn("│ [TLD-Range] 📊 COVERAGE STATUS: PARTIAL ⚠️");
                logger.warn("│ [TLD-Range] Requested range: {} to {}", fromSerialNo, toSerialNo);
                logger.warn("│ [TLD-Range] Returned range: {} to {}", firstSerialReturned, lastSerialReturned);
                logger.warn("│ [TLD-Range] Total in range: {}, Returned: {}, Remaining: {}",
                        totalMatchingInRange, actualRecordsReturned, (totalMatchingInRange - actualRecordsReturned));
            }
        }

        // Warn if trying to download already downloaded range
        if (fromSerialNo != null && toSerialNo != null && actualRecordsReturned > 0) {
            response.put("downloadWarning", String.format(
                    "⚠️ If you already downloaded this range (%d to %d), you may be getting duplicate data. " +
                    "Consider using the 'nextRangeSuggestion' to avoid duplicates.",
                    fromSerialNo, toSerialNo));
            logger.warn("│ [TLD-Range] ⚠️  DUPLICATE CHECK:");
            logger.warn("│ [TLD-Range] If range {} to {} was previously downloaded, this is a duplicate request",
                    fromSerialNo, toSerialNo);
            logger.warn("│ [TLD-Range] Use 'nextRangeSuggestion' from previous response to avoid duplicates");
        }

        // Save detailed TLD range filtering logs to database
        saveTldRangeLogToDatabase(conferenceId, dashboardMasterId, extension, fromSerialNo, toSerialNo, response, AdminActivityLog.ActionType.VIEW_TLD_FILTER);

        logger.info("└─────────────────────────────────────────────────────────────────");

        return response;
    }

    /**
     * Helper method to build a human-readable range description
     */
    private String buildRangeDescription(Long fromSerialNo, Long toSerialNo) {
        if (fromSerialNo != null && toSerialNo != null) {
            return String.format("%d to %d", fromSerialNo, toSerialNo);
        } else if (fromSerialNo != null) {
            return String.format("from %d onwards", fromSerialNo);
        } else if (toSerialNo != null) {
            return String.format("up to %d", toSerialNo);
        } else {
            return "all records";
        }
    }

    /**
     * Save TLD range filtering activity to database for admin visibility.
     * Stores all key metrics: range, records returned, suggestions, warnings, etc.
     */
    public void saveTldRangeLogToDatabase(
            String conferenceId, String dashboardMasterId, String extension,
            Long fromSerialNo, Long toSerialNo,
            Map<String, Object> result,
            AdminActivityLog.ActionType actionType) {

        try {
            // Get current user info
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            String adminId = userDetails.getUser().getId();
            String adminName = userDetails.getUser().getFirstName() + " " + userDetails.getUser().getLastName();

            // Create log entry
            AdminActivityLog log = new AdminActivityLog();
            log.setAdminId(adminId);
            log.setAdminName(adminName);
            log.setConferenceId(conferenceId);
            log.setDashboardMasterId(dashboardMasterId);
            log.setActionType(actionType);
            log.setCreatedAt(LocalDateTime.now());

            // Set TLD-specific fields
            log.setTldExtension(extension);
            log.setFromSerialNo(fromSerialNo);
            log.setToSerialNo(toSerialNo);

            // Set response metrics
            log.setTotalMatchingInRange((Long) result.get("totalMatchingInRange"));
            log.setRecordsReturned((Integer) result.get("recordsReturned"));
            log.setHasMoreRecords((Boolean) result.get("hasMoreRecords"));
            log.setRangeCoverage((String) result.get("rangeCoverage"));

            // Calculate and set percentage
            long total = (Long) result.get("totalMatchingInRange");
            int returned = (Integer) result.get("recordsReturned");
            double percentage = total > 0 ? (returned * 100.0 / total) : 0.0;
            log.setPercentageExported(percentage);

            // Set next range suggestion if available
            if (result.containsKey("nextRangeSuggestion")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nextRange = (Map<String, Object>) result.get("nextRangeSuggestion");
                log.setSuggestedNextFrom((Long) nextRange.get("fromSerialNo"));
                log.setSuggestedNextTo((Long) nextRange.get("toSerialNo"));
            }

            // Set warning message if any
            if (result.containsKey("downloadWarning")) {
                log.setWarningMessage((String) result.get("downloadWarning"));
            }

            // Build comprehensive description
            StringBuilder description = new StringBuilder();
            description.append(String.format("TLD Filter: .%s | Range: %s | ",
                extension,
                buildRangeDescription(fromSerialNo, toSerialNo)));
            description.append(String.format("Returned: %d/%d records (%.2f%%) | ",
                returned, total, percentage));
            description.append(result.get("hasMoreRecords").equals(true) ?
                "Has More Records ⚠️" : "Complete ✅");

            log.setDescription(description.toString());

            // Build filter summary
            String filterSummary = String.format(
                "TLD: .%s, Range: %s, Coverage: %s, Records: %d of %d",
                extension,
                buildRangeDescription(fromSerialNo, toSerialNo),
                result.getOrDefault("rangeCoverage", "N/A"),
                returned,
                total
            );
            log.setFilterSummary(filterSummary);

            // Save to database
            adminActivityLogRepository.save(log);

            logger.info("✅ TLD range log saved to database: {}", log.getDescription());

        } catch (Exception e) {
            logger.error("Failed to save TLD range log to database: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate Excel file from data list
     */
    private void generateExcelFile(HttpServletResponse response, List<DashboardData> dataList,
                                    ExportFilterRequest filterRequest, AdminActivityLog.ActionType actionType) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dashboard Data");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Serial No");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("Email");
        headerRow.createCell(3).setCellValue("Upload Date");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int rowIdx = 1;
        for (DashboardData data : dataList) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(data.getSerialNo() != null ? data.getSerialNo() : 0);
            row.createCell(1).setCellValue(data.getName() != null ? data.getName() : "");
            row.createCell(2).setCellValue(data.getEmail() != null ? data.getEmail() : "");
            row.createCell(3).setCellValue(data.getCreatedAt() != null ? data.getCreatedAt().format(formatter) : "");
        }

        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        logDetailedAction(filterRequest, actionType, (long) dataList.size());

        String fileName = "data_" + LocalDate.now().toString() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    /**
     * Generate PDF file from data list
     */
    private void generatePdfFile(HttpServletResponse response, List<DashboardData> dataList,
                                  ExportFilterRequest filterRequest, AdminActivityLog.ActionType actionType) throws IOException {
        Document document = new Document(PageSize.A4.rotate()); // Landscape for more columns
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(18);
        titleFont.setColor(java.awt.Color.BLUE);

        Paragraph p = new Paragraph("Dashboard Data Report", titleFont);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(p);

        // Add filter info
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA);
        infoFont.setSize(10);
        Paragraph info = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), infoFont);
        info.setAlignment(Paragraph.ALIGN_CENTER);
        info.setSpacingAfter(10);
        document.add(info);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] {1.0f, 2.5f, 4.0f, 2.0f});
        table.setSpacingBefore(10);

        writePdfHeader(table);
        writePdfData(table, dataList);

        document.add(table);

        logDetailedAction(filterRequest, actionType, (long) dataList.size());

        document.close();
    }

    private void writePdfHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(java.awt.Color.BLUE);
        cell.setPadding(5);

        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        font.setColor(java.awt.Color.WHITE);

        cell.setPhrase(new Phrase("S.No", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Name", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Email", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Country", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Upload Date", font));
        table.addCell(cell);
    }

    private void writePdfData(PdfPTable table, List<DashboardData> dataList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (DashboardData data : dataList) {
            table.addCell(String.valueOf(data.getSerialNo() != null ? data.getSerialNo() : ""));
            table.addCell(data.getName() != null ? data.getName() : "");
            table.addCell(data.getEmail() != null ? data.getEmail() : "");
            table.addCell(data.getCreatedAt() != null ? data.getCreatedAt().format(formatter) : "");
        }
    }

    private String buildFilterSummary(ExportFilterRequest fr) {
        StringBuilder sb = new StringBuilder();
        if (fr.getFromSerialNo() != null && fr.getToSerialNo() != null) {
            sb.append("Serial No: ").append(fr.getFromSerialNo()).append(" to ").append(fr.getToSerialNo()).append("; ");
        } else if (fr.getFromSerialNo() != null) {
            sb.append("Serial No from: ").append(fr.getFromSerialNo()).append("; ");
        } else if (fr.getToSerialNo() != null) {
            sb.append("Serial No to: ").append(fr.getToSerialNo()).append("; ");
        }
        if (fr.getStartDate() != null || fr.getEndDate() != null) {
            sb.append("Date: ").append(fr.getStartDate() != null ? fr.getStartDate() : "any")
              .append(" to ").append(fr.getEndDate() != null ? fr.getEndDate() : "any").append("; ");
        }
        if (fr.getEmailDomain() != null && !fr.getEmailDomain().isEmpty()) {
            sb.append("Email Domain: ").append(fr.getEmailDomain()).append("; ");
        }
        return sb.length() > 0 ? sb.toString().trim() : "No additional filters";
    }

    /**
     * Log detailed action with serial range, total records, email domain, and filter summary.
     */
    private void logDetailedAction(ExportFilterRequest fr, AdminActivityLog.ActionType actionType, Long totalRecords) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(userDetails.getId());
        userRepository.findById(userDetails.getId()).ifPresent(user ->
            log.setAdminName(user.getFirstName() + " " + user.getLastName())
        );

        log.setConferenceId(fr.getConferenceId());
        log.setDashboardMasterId(fr.getDashboardMasterId());
        log.setActionType(actionType);
        log.setFromSerialNo(fr.getFromSerialNo());
        log.setToSerialNo(fr.getToSerialNo());
        log.setTotalRecords(totalRecords);
        log.setEmailDomain(fr.getEmailDomain() != null && !fr.getEmailDomain().isEmpty() ? fr.getEmailDomain() : null);

        String filterSummary = buildFilterSummary(fr);
        log.setFilterSummary(filterSummary);

        // Build description
        String actionLabel = actionType == AdminActivityLog.ActionType.DOWNLOAD_EXCEL ? "Downloaded Excel"
                : actionType == AdminActivityLog.ActionType.DOWNLOAD_PDF ? "Downloaded PDF"
                : actionType == AdminActivityLog.ActionType.VIEW ? "Viewed data"
                : actionType.name();

        StringBuilder desc = new StringBuilder(actionLabel);
        if (fr.getFromSerialNo() != null && fr.getToSerialNo() != null) {
            desc.append(" | Serial No ").append(fr.getFromSerialNo()).append(" to ").append(fr.getToSerialNo());
        }
        if (totalRecords != null) {
            desc.append(" | Total records: ").append(totalRecords);
        }
        if (fr.getEmailDomain() != null && !fr.getEmailDomain().isEmpty()) {
            desc.append(" | Email Domain: ").append(fr.getEmailDomain());
        }
        log.setDescription(desc.toString());

        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        // Save + push to SSE subscribers in real-time
        analyticsService.saveAndPushLog(log);
    }

    /**
     * Public method to log a VIEW (data preview) action from DashboardDataController.
     */
    public void logViewAction(ExportFilterRequest fr, long totalRecords) {
        logDetailedAction(fr, AdminActivityLog.ActionType.VIEW, totalRecords);
    }
}
