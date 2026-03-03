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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);

    /**
     * Number of documents to insert per MongoDB bulk-insert call.
     * 1000 is a sweet-spot: large enough to keep round-trips low,
     * small enough to avoid BSON document-size limits.
     */
    private static final int BATCH_SIZE = 1000;

    @Autowired private DashboardDataRepository dashboardDataRepository;
    @Autowired private DashboardUploadStatsRepository dashboardUploadStatsRepository;
    @Autowired private AdminActivityLogRepository adminActivityLogRepository;
    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserRepository userRepository;
    @Autowired private MongoTemplate mongoTemplate;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> processExcelFile(MultipartFile file,
                                                 String conferenceId,
                                                 String dashboardMasterId) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User admin = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() == User.Role.ADMIN &&
                (admin.getConferenceIds() == null || !admin.getConferenceIds().contains(conferenceId))) {
            throw new RuntimeException("Access Denied: You are not assigned to this conference.");
        }

        // Read bytes eagerly — MultipartFile is invalid after the HTTP request ends
        byte[] fileBytes = file.getBytes();
        String originalFileName = file.getOriginalFilename();

        return processBulkUpload(fileBytes, originalFileName, conferenceId, dashboardMasterId, admin);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORE PROCESSING — designed for 100 000+ rows in < 1 minute
    //
    //  Strategy (all O(n)):
    //  1. Load every existing email for this conference+dashboard in ONE query
    //     → put them into a HashSet<String>  (O(1) lookup)
    //  2. Parse all Excel rows into memory  (fast POI read)
    //  3. Walk rows: normalise email, check HashSet → skip or queue
    //     The HashSet also receives newly-queued emails so within-file
    //     duplicates are caught without a second DB round-trip.
    //  4. Bulk-insert queued records in batches of BATCH_SIZE
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> processBulkUpload(byte[] fileBytes, String fileName,
                                                   String conferenceId, String dashboardMasterId,
                                                   User admin) throws IOException {
        long startTime = System.currentTimeMillis();

        // ── STEP 1: Load ALL existing emails for this conference+dashboard (ONE query) ──
        logger.info("[Upload] Loading existing emails — conferenceId={} dashboardMasterId={}",
                conferenceId, dashboardMasterId);

        Query emailQuery = new Query(
                Criteria.where("conferenceId").is(conferenceId)
                        .and("dashboardMasterId").is(dashboardMasterId)
                        .and("deleted").is(false));
        emailQuery.fields().include("email"); // projection — only fetch the email field
        // Use case-insensitive collation to match the unique index
        emailQuery.collation(org.springframework.data.mongodb.core.query.Collation.of("en")
                .strength(org.springframework.data.mongodb.core.query.Collation.ComparisonLevel.secondary()));

        List<DashboardData> existingDocs = mongoTemplate.find(emailQuery, DashboardData.class);

        // Normalise every stored email exactly the same way we normalise incoming ones
        Set<String> seenEmails = new HashSet<>();
        for (DashboardData doc : existingDocs) {
            String email = doc.getEmail();
            if (email != null && !email.isBlank()) {
                seenEmails.add(normalizeEmail(email));
            }
        }

        logger.info("[Upload] Existing emails loaded: {} (from {} documents)", seenEmails.size(), existingDocs.size());

        // ── STEP 2: Parse Excel ──
        logger.info("[Upload] Parsing Excel file: {}", fileName);
        List<String[]> allRows = parseExcelRows(fileBytes);
        int totalRecords = allRows.size();
        logger.info("[Upload] Parsed {} data rows", totalRecords);

        // ── STEP 3: Determine next serial number (ONE query) ──
        long nextSerial = getNextSerialNo(conferenceId, dashboardMasterId);
        logger.info("[Upload] Next serial number: {}", nextSerial);

        // ── STEP 4: Walk rows, dedupe against seenEmails HashSet ──
        List<DashboardData> toInsert = new ArrayList<>(Math.min(totalRecords, 10_000));
        Set<String> fileEmails = new HashSet<>();  // Track emails in current file for within-file dedup
        int newRecords  = 0;
        int duplicates  = 0;
        int skippedInvalid = 0;
        int emptyEmailCount = 0;
        int noAtSignCount = 0;
        List<String[]> sampleInvalidRows = new ArrayList<>();  // First 5 invalid rows for diagnostics
        LocalDateTime now = LocalDateTime.now();

        for (String[] row : allRows) {
            String rawName  = row[0];
            String rawEmail = row[1];

            // Skip completely empty rows
            if (isBlank(rawName) && isBlank(rawEmail)) {
                skippedInvalid++;
                if (sampleInvalidRows.size() < 5) {
                    sampleInvalidRows.add(new String[]{"[BLANK]", "[BLANK]"});
                }
                continue;
            }

            // Skip rows without a usable email
            if (isBlank(rawEmail)) {
                skippedInvalid++;
                emptyEmailCount++;
                logger.debug("[Upload] Skipped row — empty email, name='{}'", rawName);
                if (sampleInvalidRows.size() < 5) {
                    sampleInvalidRows.add(new String[]{"[EMPTY_EMAIL]", rawName});
                }
                continue;
            }

            String emailNorm = normalizeEmail(rawEmail);

            // Basic "@" check
            if (!emailNorm.contains("@")) {
                skippedInvalid++;
                noAtSignCount++;
                logger.debug("[Upload] Skipped row — invalid email format: raw='{}', normalized='{}'", rawEmail, emailNorm);
                if (sampleInvalidRows.size() < 5) {
                    sampleInvalidRows.add(new String[]{rawEmail, rawName});
                }
                continue;
            }

            // ── DUPLICATE CHECK ──
            // First check: is it in the database already?
            if (seenEmails.contains(emailNorm)) {
                duplicates++;
                logger.debug("[Upload] Duplicate from DB skipped: {}", emailNorm);
                continue;
            }

            // Second check: is it already in this file?
            if (fileEmails.contains(emailNorm)) {
                duplicates++;
                logger.debug("[Upload] Duplicate within file skipped: {}", emailNorm);
                continue;
            }

            // New record — add to both sets
            seenEmails.add(emailNorm);
            fileEmails.add(emailNorm);

            DashboardData data = new DashboardData();
            data.setConferenceId(conferenceId);
            data.setDashboardMasterId(dashboardMasterId);
            data.setName(isBlank(rawName) ? "" : rawName.trim());
            data.setEmail(emailNorm);
            data.setStatus(true);
            data.setCreatedAt(now);
            data.setUpdatedAt(now);
            data.setSerialNo(nextSerial++);

            toInsert.add(data);
            newRecords++;
        }

        logger.info("[Upload] Dedup complete — new={}, duplicates={}, invalid={} (empty_email={}, no_at_sign={})",
                newRecords, duplicates, skippedInvalid, emptyEmailCount, noAtSignCount);

        // ── Log sample invalid rows for diagnosis ──
        if (!sampleInvalidRows.isEmpty()) {
            logger.info("[Upload] Sample invalid rows (first 5): {}",
                sampleInvalidRows.stream()
                    .map(arr -> String.format("email='%s', name='%s'", arr[0], arr[1]))
                    .collect(Collectors.joining("; ")));
        }

        // ── STEP 5: Bulk-insert in batches with rollback on failure ──
        int batchCount = 0;
        List<String> insertedIds = new ArrayList<>();  // track every inserted _id for rollback

        if (!toInsert.isEmpty()) {
            int totalBatches = (int) Math.ceil((double) toInsert.size() / BATCH_SIZE);
            try {
                for (int i = 0; i < toInsert.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, toInsert.size());
                    List<DashboardData> batch = toInsert.subList(i, end);

                    Collection<DashboardData> inserted = mongoTemplate.insertAll(batch);
                    inserted.forEach(doc -> {
                        if (doc.getId() != null) insertedIds.add(doc.getId());
                    });

                    batchCount++;
                    logger.info("[Upload] Inserted batch {}/{} ({} records)",
                            batchCount, totalBatches, end - i);
                }
            } catch (Exception insertEx) {
                // ── ROLLBACK: delete every record we successfully inserted ──
                if (!insertedIds.isEmpty()) {
                    logger.error("[Upload] Batch insert FAILED at batch {} — rolling back {} already-inserted records",
                            batchCount + 1, insertedIds.size());
                    try {
                        Query rollbackQuery = new Query(
                                Criteria.where("_id").in(insertedIds));
                        long deleted = mongoTemplate.remove(rollbackQuery, DashboardData.class).getDeletedCount();
                        logger.info("[Upload] Rollback complete — deleted {} records", deleted);
                    } catch (Exception rollbackEx) {
                        logger.error("[Upload] Rollback itself failed — manual cleanup may be needed. IDs: {}",
                                insertedIds, rollbackEx);
                    }
                }
                throw new RuntimeException(
                        "Upload failed during database insert. No data has been saved. " +
                        "Please try uploading the file again. (Cause: " + insertEx.getMessage() + ")",
                        insertEx);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[Upload] DONE in {}ms — total={}, new={}, dup={}, invalid={} (empty_email={}, no_at_sign={}), batches={}",
                elapsed, totalRecords, newRecords, duplicates, skippedInvalid, emptyEmailCount, noAtSignCount, batchCount);

        // ── STEP 6: Persist stats + activity log (only on full success) ──
        saveStats(admin, conferenceId, dashboardMasterId, fileName,
                totalRecords, newRecords, duplicates);
        logUploadActivity(admin, conferenceId, dashboardMasterId, fileName, newRecords, duplicates);

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

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL PARSING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse the first sheet of an Excel file (.xls, .xlsx, .xlsm).
     * Row 0 is the header — reads column names dynamically.
     * Returns a list of [name, email] string pairs.
     * Supports multiple Excel formats:
     * - .xlsx (Office Open XML) - Modern Excel format
     * - .xls (BIFF8) - Legacy Excel format
     * - .xlsm (Macro-enabled XLSX)
     *
     * IMPORTANT: Reads columns by header name (case-insensitive), so column order doesn't matter.
     * Supports headers: "Name", "Email" or any variation like "email", "EMAIL", "name", etc.
     */
    private List<String[]> parseExcelRows(byte[] fileBytes) throws IOException {
        List<String[]> rows = new ArrayList<>();

        // Create appropriate Workbook based on file format
        Workbook workbook = createWorkbook(fileBytes);

        try {
            // Use a FormulaEvaluator so FORMULA cells return their computed value
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.setIgnoreMissingWorkbooks(true);

            Sheet sheet = workbook.getSheetAt(0);

            // ── STEP 1: Read header row and build column index map ──
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel file has no header row. Please ensure Row 1 contains column headers (Name, Email).");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell != null) {
                    String headerName = getCellValueSafe(cell, evaluator).trim().toLowerCase();
                    if (!headerName.isEmpty()) {
                        headerMap.put(headerName, cell.getColumnIndex());
                    }
                }
            }

            logger.info("[Upload] Detected headers: {}", headerMap.keySet());

            // ── STEP 2: Find name and email columns ──
            Integer nameColIndex = headerMap.get("name");
            Integer emailColIndex = headerMap.get("email");

            if (nameColIndex == null && emailColIndex == null) {
                throw new IOException("Excel file must have 'Name' and 'Email' columns in the header row. " +
                        "Found headers: " + headerMap.keySet());
            }

            if (emailColIndex == null) {
                throw new IOException("Excel file missing required 'Email' column in header. " +
                        "Found headers: " + headerMap.keySet());
            }

            // Name column is optional - use -1 if not found
            int emailCol = emailColIndex;
            int nameCol = (nameColIndex != null) ? nameColIndex : -1;

            logger.info("[Upload] Reading data — Name column: {}, Email column: {}",
                    (nameCol >= 0 ? nameCol : "NOT FOUND"), emailCol);

            // ── STEP 3: Parse data rows (skip header row 0) ──
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = (nameCol >= 0) ? getCellValueSafe(row.getCell(nameCol), evaluator) : "";
                String email = getCellValueSafe(row.getCell(emailCol), evaluator);
                rows.add(new String[]{name, email});
            }
        } finally {
            workbook.close();
        }
        return rows;
    }

    /**
     * Create appropriate Workbook instance based on file format
     * Supports: .xlsx (OOXML), .xls (BIFF8), .xlsm (Macro-enabled XLSX)
     */
    private Workbook createWorkbook(byte[] fileBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

        try {
            // Try XLSX first (most common modern format)
            return new XSSFWorkbook(bais);
        } catch (Exception xlsxException) {
            // If XLSX fails, try XLS (legacy format)
            try {
                bais.reset();
                return new HSSFWorkbook(bais);
            } catch (Exception xlsException) {
                logger.error("[Upload] Failed to parse file as XLSX or XLS format");
                logger.error("[Upload] XLSX error: {}", xlsxException.getMessage());
                logger.error("[Upload] XLS error: {}", xlsException.getMessage());
                throw new IOException("Unsupported Excel file format. Please use .xls, .xlsx, or .xlsm files.", xlsxException);
            }
        }
    }

    /**
     * Extract a trimmed string value from a cell regardless of its type.
     * Passes a FormulaEvaluator so FORMULA cells are resolved correctly.
     */
    private String getCellValueSafe(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";

        CellType type = cell.getCellType();

        // Resolve formula to its actual type first
        if (type == CellType.FORMULA) {
            try {
                CellValue cv = evaluator.evaluate(cell);
                if (cv == null) return "";
                switch (cv.getCellType()) {
                    case STRING:  return cv.getStringValue().trim();
                    case NUMERIC: return formatNumeric(cv.getNumberValue());
                    case BOOLEAN: return String.valueOf(cv.getBooleanValue());
                    default:      return "";
                }
            } catch (Exception e) {
                // Fallback: try reading cached value
                try { return cell.getStringCellValue().trim(); } catch (Exception ex) { return ""; }
            }
        }

        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return "";   // dates are not emails/names
                return formatNumeric(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
            default:
                return "";
        }
    }

    /** Format a numeric cell as a plain integer string when there is no fractional part. */
    private String formatNumeric(double val) {
        return (val == Math.floor(val) && !Double.isInfinite(val))
                ? String.valueOf((long) val)
                : String.valueOf(val);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private synchronized long getNextSerialNo(String conferenceId, String dashboardMasterId) {
        return dashboardDataRepository
                .findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(conferenceId, dashboardMasterId)
                .map(d -> d.getSerialNo() + 1)
                .orElse(1L);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Normalize an email address for accurate duplicate detection:
     *  1. Trim leading/trailing whitespace from raw input
     *  2. Strip ALL internal whitespace (spaces, tabs, non-breaking spaces, zero-width chars)
     *  3. Convert to lowercase
     *
     * This handles hidden characters that Excel may embed in cells,
     * such as \u00A0 (non-breaking space), \u200B (zero-width space), etc.
     */
    private static String normalizeEmail(String raw) {
        if (raw == null) return "";
        // First trim leading/trailing whitespace, then remove ALL internal whitespace and special chars, finally lowercase
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
                                   String fileName, int added, int duplicates) {
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(admin.getId());
        log.setAdminName(admin.getFirstName() + " " + admin.getLastName());
        log.setConferenceId(conferenceId);
        log.setDashboardMasterId(dashboardMasterId);
        log.setActionType(AdminActivityLog.ActionType.UPLOAD_EXCEL);
        log.setDescription("Uploaded Excel: " + fileName
                + " | Added: " + added + " | Duplicates: " + duplicates);
        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        analyticsService.saveAndPushLog(log);
    }
}
