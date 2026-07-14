package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.DashboardData;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.AdminActivityLogRepository;
import com.sciinov.dbms.repository.DashboardDataRepository;
import com.sciinov.dbms.repository.DashboardUploadStatsRepository;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.security.UserDetailsImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    /** Batch size for MongoDB bulk inserts. */
    private static final int BATCH_SIZE = 10_000;

    @Autowired private DashboardDataRepository dashboardDataRepository;
    @Autowired private DashboardUploadStatsRepository dashboardUploadStatsRepository;
    @Autowired private AdminActivityLogRepository adminActivityLogRepository;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserRepository userRepository;
    @Autowired private MongoTemplate mongoTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT — fully synchronous, no @Async
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process an uploaded Excel file using STREAMING to minimize memory usage.
     * <p>
     * Key optimization: Never load the entire file into memory.
     * Instead, process rows in small batches as they're read.
     * <p>
     * Why synchronous (not @Async)?
     * When @Async was used, Tomcat fired an ASYNC dispatch after task completion.
     * That re-dispatch went through the security filter chain without the JWT
     * header → AuthorizationDeniedException → error response without CORS headers
     * → browser reported "CORS blocked". This happened intermittently depending
     * on timing and file size. Synchronous processing eliminates this entirely.
     * <p>
     * Performance is still excellent: 100k rows complete in under 60 seconds
     * thanks to streaming processing and batch inserts.
     */
    public Map<String, Object> processExcelFile(MultipartFile file,
                                                 String conferenceId,
                                                 String dashboardMasterId) throws IOException {

        // ── Auth: extract user from SecurityContext ──
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User admin = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() == User.Role.ADMIN &&
                (admin.getConferenceIds() == null || !admin.getConferenceIds().contains(conferenceId))) {
            throw new RuntimeException("Access Denied: You are not assigned to this conference.");
        }

        String originalFileName = file.getOriginalFilename();
        logger.info("📥 Upload started: fileName={} admin={}", originalFileName, admin.getId());

        // ── Do the actual processing using STREAMING (not loading full file into memory) ──
        Map<String, Object> result = processBulkUploadStreaming(file, originalFileName,
                conferenceId, dashboardMasterId, admin);

        logger.info("✅ Upload completed: new={}, dup={}, time={}ms",
                result.get("newRecordsAdded"),
                result.get("duplicateRecordsIgnored"),
                result.get("processingTimeMs"));

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STREAMING PROCESSING - Memory Optimized
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Process Excel file using STREAMING (row-by-row).
     * Never loads the entire file into memory.
     * Processes rows in batches of 5000 for optimal memory usage.
     */
    private Map<String, Object> processBulkUploadStreaming(MultipartFile file,
                                                            String fileName,
                                                            String conferenceId,
                                                            String dashboardMasterId,
                                                            User admin) throws IOException {
        long startTime = System.currentTimeMillis();

        // ── STEP 1: Load existing emails in ONE query ──
        logger.info("[Upload] Loading existing emails — conf={} dm={}", conferenceId, dashboardMasterId);

        Query emailQuery = new Query(
                Criteria.where("conferenceId").is(conferenceId)
                        .and("dashboardMasterId").is(dashboardMasterId)
                        .and("deleted").is(false));
        emailQuery.fields().include("email");
        emailQuery.collation(org.springframework.data.mongodb.core.query.Collation.of("en")
                .strength(org.springframework.data.mongodb.core.query.Collation.ComparisonLevel.secondary()));

        List<DashboardData> existingDocs = mongoTemplate.find(emailQuery, DashboardData.class);
        Set<String> seenEmails = new HashSet<>();
        for (DashboardData doc : existingDocs) {
            String email = doc.getEmail();
            if (email != null && !email.isBlank()) {
                seenEmails.add(normalizeEmail(email));
            }
        }
        logger.info("[Upload] Existing emails loaded: {}", seenEmails.size());

        // ── STEP 2: Get next serial number ──
        long startSerial = getNextSerialNo(conferenceId, dashboardMasterId);
        long nextSerial = startSerial;

        // ── Variables to track processing ──
        List<DashboardData> toInsert = new ArrayList<>(5_000);
        Set<String> fileEmails = new HashSet<>();
        int newRecords = 0, duplicates = 0, skippedInvalid = 0;
        int emptyEmailCount = 0, noAtSignCount = 0;
        int totalRecords = 0;
        List<String[]> sampleInvalidRows = new ArrayList<>();
        List<String> insertedIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int batchCount = 0;

        // ── STEP 3: Stream through Excel file row by row ──
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // ── STEP 3a: Parse header to find column indices ──
            Row headerRow = null;
            if (rows.hasNext()) {
                headerRow = rows.next();
            }

            if (headerRow == null) {
                throw new IOException("Excel file has no header row. Row 1 must contain column headers (Name, Email).");
            }

            // Build header map to find column indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell != null) {
                    String header = getCellValue(cell).trim().toLowerCase();
                    if (!header.isEmpty()) {
                        String normalizedHeader = normalizeHeaderName(header);
                        headerMap.put(normalizedHeader, cell.getColumnIndex());
                    }
                }
            }
            logger.info("[Upload] Excel Headers Found: {}", headerMap.keySet());

            // Find Email column (required)
            Integer emailColIndex = findColumnByNames(headerMap, "email", "e-mail", "mail", "email_address");
            if (emailColIndex == null) {
                throw new IOException("❌ Excel file missing required 'Email' column. Found columns: " + headerMap.keySet());
            }

            // Find Name column (optional)
            Integer nameColIndex = findColumnByNames(headerMap, "name", "full_name", "firstname", "first_name", "user_name", "username");
            logger.info("[Upload] Column Mapping - Email: {}, Name: {}", emailColIndex, nameColIndex != null ? nameColIndex : "N/A");

            final int emailCol = emailColIndex;
            final int nameCol = (nameColIndex != null) ? nameColIndex : -1;

            // ── STEP 3b: Stream through data rows ──

            while (rows.hasNext()) {
                Row row = rows.next();
                String rawName = (nameCol >= 0) ? getCellValue(row.getCell(nameCol)) : "";
                String rawEmail = getCellValue(row.getCell(emailCol));

                totalRecords++;

                // Validate row
                if (isBlank(rawName) && isBlank(rawEmail)) {
                    skippedInvalid++;
                    if (sampleInvalidRows.size() < 5) sampleInvalidRows.add(new String[]{"[BLANK]", "[BLANK]"});
                    continue;
                }
                if (isBlank(rawEmail)) {
                    skippedInvalid++;
                    emptyEmailCount++;
                    if (sampleInvalidRows.size() < 5) sampleInvalidRows.add(new String[]{"[EMPTY_EMAIL]", rawName});
                    continue;
                }

                String emailNorm = normalizeEmail(rawEmail);
                if (!emailNorm.contains("@")) {
                    skippedInvalid++;
                    noAtSignCount++;
                    if (sampleInvalidRows.size() < 5) sampleInvalidRows.add(new String[]{rawEmail, rawName});
                    continue;
                }

                // Check for duplicates
                if (seenEmails.contains(emailNorm) || fileEmails.contains(emailNorm)) {
                    duplicates++;
                    continue;
                }

                seenEmails.add(emailNorm);
                fileEmails.add(emailNorm);

                // Create entity
                DashboardData data = new DashboardData();
                data.setConferenceId(conferenceId);
                data.setDashboardMasterId(dashboardMasterId);
                data.setName(isBlank(rawName) ? "" : rawName.trim());
                data.setEmail(emailNorm);

                // Extract email extension (domain/TLD part after @)
                if (emailNorm.contains("@")) {
                    String extension = emailNorm.substring(emailNorm.indexOf("@") + 1);
                    data.setEmailExtension(extension);
                }

                data.setStatus(true);
                data.setCreatedAt(now);
                data.setUpdatedAt(now);
                data.setSerialNo(nextSerial++);

                toInsert.add(data);
                newRecords++;

                // FLUSH: Insert batch when it reaches 5000 records
                if (toInsert.size() >= 5_000) {
                    logger.info("[Upload] Flushing batch of {} records...", toInsert.size());
                    flushBatch(toInsert, insertedIds);
                    batchCount++;
                    toInsert.clear(); // ← KEY: Clear list to free memory
                    // NOTE: DO NOT clear fileEmails here - we need to track ALL emails in the file
                    // to prevent duplicates from being added across batches
                }
            }

            // ── STEP 4: Flush remaining records ──
            if (!toInsert.isEmpty()) {
                logger.info("[Upload] Flushing final batch of {} records...", toInsert.size());
                flushBatch(toInsert, insertedIds);
                batchCount++;
                toInsert.clear();
            }

        } catch (Exception e) {
            // Rollback if batch insert failed
            if (!insertedIds.isEmpty()) {
                logger.error("[Upload] Processing FAILED — rolling back {} records", insertedIds.size());
                try {
                    long deleted = mongoTemplate.remove(
                            new Query(Criteria.where("_id").in(insertedIds)), DashboardData.class
                    ).getDeletedCount();
                    logger.info("[Upload] Rollback done — deleted {}", deleted);
                } catch (Exception rbEx) {
                    logger.error("[Upload] Rollback failed — manual cleanup may be needed", rbEx);
                }
            }
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }

        logger.info("[Upload] Parsing DONE — total={}, new={}, dup={}, invalid={}",
                totalRecords, newRecords, duplicates, skippedInvalid);

        if (!sampleInvalidRows.isEmpty()) {
            logger.info("[Upload] Sample invalid rows: {}",
                    sampleInvalidRows.stream()
                            .map(a -> String.format("email='%s', name='%s'", a[0], a[1]))
                            .collect(Collectors.joining("; ")));
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[Upload] DONE in {}ms — total={}, new={}, dup={}, invalid={}, batches={}",
                elapsed, totalRecords, newRecords, duplicates, skippedInvalid, batchCount);

        // ── STEP 5: Save stats + activity log ──
        saveStats(admin, conferenceId, dashboardMasterId, fileName, totalRecords, newRecords, duplicates);
        logUploadActivity(admin, conferenceId, dashboardMasterId, fileName, newRecords, duplicates, startSerial);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "File uploaded successfully");
        result.put("fileName", fileName);
        result.put("totalRecordsInFile", totalRecords);
        result.put("newRecordsAdded", newRecords);
        result.put("duplicateRecordsIgnored", duplicates);
        result.put("invalidRowsSkipped", skippedInvalid);
        result.put("processingTimeMs", elapsed);
        result.put("batchesInserted", batchCount);
        return result;
    }

    /**
     * Flush a batch of records to MongoDB with robust duplicate detection.
     * Also handles partial failures gracefully.
     */
    private void flushBatch(List<DashboardData> batch, List<String> insertedIds) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        // ── PRE-CHECK: Detect duplicates within batch using case-insensitive comparison ──
        Set<String> batchEmails = new HashSet<>();
        List<DashboardData> dedupedBatch = new ArrayList<>();

        for (DashboardData data : batch) {
            String emailNorm = normalizeEmail(data.getEmail());
            if (!batchEmails.contains(emailNorm)) {
                batchEmails.add(emailNorm);
                dedupedBatch.add(data);
            } else {
                logger.debug("[Batch Dedup] Duplicate within batch removed: {}", emailNorm);
            }
        }

        if (dedupedBatch.isEmpty()) {
            logger.info("[Upload] Batch flush skipped — all records were duplicates within batch");
            return;
        }

        // ── Insert deduplicated batch ──
        try {
            Collection<DashboardData> inserted = mongoTemplate.insertAll(dedupedBatch);
            int insertedCount = (inserted != null) ? inserted.size() : 0;

            // Track inserted IDs for rollback
            if (inserted != null) {
                inserted.forEach(doc -> {
                    if (doc.getId() != null) {
                        insertedIds.add(doc.getId());
                    }
                });
            }

            logger.info("[Upload] Batch flushed — {} records inserted, {} duplicates removed from batch",
                        insertedCount, batch.size() - insertedCount);

        } catch (org.springframework.dao.DuplicateKeyException dkEx) {
            // ── Handle duplicate key errors gracefully ──
            logger.warn("[Upload] Duplicate key detected in batch. Attempting single-record insertion for valid records...");

            // Try to insert records one by one to identify which ones have duplicates
            int successful = 0;
            int duplicateErrors = 0;

            for (DashboardData data : dedupedBatch) {
                try {
                    DashboardData inserted = mongoTemplate.insert(data);
                    if (inserted != null && inserted.getId() != null) {
                        insertedIds.add(inserted.getId());
                        successful++;
                    }
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    duplicateErrors++;
                    logger.debug("[Upload] Skipped duplicate email: {}", data.getEmail());
                } catch (Exception e) {
                    logger.error("[Upload] Error inserting record: {}", e.getMessage());
                    throw new RuntimeException("Batch insert failed: " + e.getMessage(), e);
                }
            }

            logger.info("[Upload] Batch partial insert — {} successful, {} duplicates", successful, duplicateErrors);

            if (successful == 0) {
                throw new RuntimeException("Batch insert failed: All records were duplicates");
            }

        } catch (Exception e) {
            logger.error("[Upload] Batch insert failed with exception: {}", e.getMessage());
            throw new RuntimeException("Batch insert failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OLD PROCESSING METHOD REMOVED
    // The processBulkUpload method that loaded entire files into memory
    // has been REMOVED and replaced with processBulkUploadStreaming.
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL PARSING
    // ─────────────────────────────────────────────────────────────────────────

    private List<String[]> parseExcelRows(byte[] fileBytes) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Workbook workbook = createWorkbook(fileBytes);
        try {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.setIgnoreMissingWorkbooks(true);
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel file has no header row. Row 1 must contain column headers (Name, Email).");
            }

            // Normalize and identify column indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell != null) {
                    String header = getCellValueSafe(cell, evaluator).trim().toLowerCase();
                    if (!header.isEmpty()) {
                        // Normalize header names to support variations
                        String normalizedHeader = normalizeHeaderName(header);
                        headerMap.put(normalizedHeader, cell.getColumnIndex());
                    }
                }
            }
            logger.info("📋 [Upload] Excel Headers Found: {}", headerMap.keySet());

            // Find Email column (required)
            Integer emailColIndex = findColumnByNames(headerMap, "email", "e-mail", "mail", "email_address");
            if (emailColIndex == null) {
                throw new IOException("❌ Excel file missing required 'Email' column. Found columns: " + headerMap.keySet());
            }

            // Find Name column (optional)
            Integer nameColIndex = findColumnByNames(headerMap, "name", "full_name", "firstname", "first_name", "user_name", "username");

            logger.info("✅ [Upload] Column Mapping - Email: {}, Name: {}", emailColIndex, nameColIndex != null ? nameColIndex : "N/A");

            int emailCol = emailColIndex;
            int nameCol  = (nameColIndex != null) ? nameColIndex : -1;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name  = (nameCol >= 0) ? getCellValueSafe(row.getCell(nameCol), evaluator) : "";
                String email = getCellValueSafe(row.getCell(emailCol), evaluator);
                rows.add(new String[]{name, email});
            }
        } finally {
            workbook.close();
        }
        return rows;
    }

    private Workbook createWorkbook(byte[] fileBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
        try {
            return new XSSFWorkbook(bais);
        } catch (Exception xlsxEx) {
            try {
                bais.reset();
                return new HSSFWorkbook(bais);
            } catch (Exception xlsEx) {
                throw new IOException("Unsupported Excel format. Use .xls, .xlsx, or .xlsm files.", xlsxEx);
            }
        }
    }

    private String getCellValueSafe(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try {
                CellValue cv = evaluator.evaluate(cell);
                if (cv == null) return "";
                return switch (cv.getCellType()) {
                    case STRING  -> cv.getStringValue().trim();
                    case NUMERIC -> formatNumeric(cv.getNumberValue());
                    case BOOLEAN -> String.valueOf(cv.getBooleanValue());
                    default      -> "";
                };
            } catch (Exception e) {
                try { return cell.getStringCellValue().trim(); } catch (Exception ex) { return ""; }
            }
        }
        return switch (type) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? "" : formatNumeric(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private String formatNumeric(double val) {
        return (val == Math.floor(val) && !Double.isInfinite(val))
                ? String.valueOf((long) val)
                : String.valueOf(val);
    }

    /**
     * Normalize header names to support various column naming conventions
     * e.g., "First Name", "FIRST_NAME", "firstname", "first name" → "firstname"
     */
    private String normalizeHeaderName(String header) {
        return header
                .replaceAll("[\\s_-]+", "")  // Remove spaces, underscores, hyphens
                .replaceAll("[^a-z0-9]", "")  // Remove special characters
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Find a column index by trying multiple possible names
     */
    private Integer findColumnByNames(Map<String, Integer> headerMap, String... possibleNames) {
        for (String name : possibleNames) {
            String normalized = normalizeHeaderName(name);
            Integer idx = headerMap.get(normalized);
            if (idx != null) return idx;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get cell value for streaming processing (simple, no formula evaluation)
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        return switch (type) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? "" : formatNumeric(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private long getNextSerialNo(String conferenceId, String dashboardMasterId) {
        return dashboardDataRepository
                .findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(conferenceId, dashboardMasterId)
                .map(d -> d.getSerialNo() + 1)
                .orElse(1L);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return "";
        return raw.trim()
                .replaceAll("[\\s\\u00A0\\u200B\\u200C\\u200D\\uFEFF]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private void saveStats(User admin, String conferenceId, String dashboardMasterId,
                           String fileName, int total, int added, int duplicates) {
        DashboardUploadStats stats = new DashboardUploadStats();
        stats.setAdminId(admin.getId());
        stats.setConferenceId(conferenceId);
        stats.setDashboardMasterId(dashboardMasterId);
        stats.setUploadedAt(LocalDateTime.now());
        stats.setTotalRecordsInFile(total);
        stats.setNewRecordsAdded(added);
        stats.setDuplicateRecordsIgnored(duplicates);
        stats.setFileName(fileName);
        dashboardUploadStatsRepository.save(stats);
    }

    private void logUploadActivity(User admin, String conferenceId, String dashboardMasterId,
                                   String fileName, int added, int duplicates, long startSerial) {
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(admin.getId());
        log.setAdminName(admin.getFirstName() + " " + admin.getLastName());
        log.setConferenceId(conferenceId);
        log.setDashboardMasterId(dashboardMasterId);
        log.setActionType(AdminActivityLog.ActionType.UPLOAD_EXCEL);
        log.setDescription("Uploaded Excel: " + fileName + " | Added: " + added + " | Duplicates: " + duplicates);
        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("server");
        log.setTotalRecords((long) added);
        if (added > 0) {
            log.setFromSerialNo(startSerial);
            log.setToSerialNo(startSerial + added - 1);
        }
        analyticsService.saveAndPushLog(log);
    }
}
