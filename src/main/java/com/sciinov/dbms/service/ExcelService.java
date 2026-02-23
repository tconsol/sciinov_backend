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

        List<DashboardData> existingDocs = mongoTemplate.find(emailQuery, DashboardData.class);

        // Normalise every stored email exactly the same way we normalise incoming ones
        Set<String> seenEmails = existingDocs.stream()
                .map(DashboardData::getEmail)
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toCollection(HashSet::new));

        logger.info("[Upload] Existing emails loaded: {}", seenEmails.size());

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
        int newRecords  = 0;
        int duplicates  = 0;
        int skippedInvalid = 0;
        LocalDateTime now = LocalDateTime.now();

        for (String[] row : allRows) {
            String rawName  = row[0];
            String rawEmail = row[1];

            // Skip completely empty rows
            if (isBlank(rawName) && isBlank(rawEmail)) {
                skippedInvalid++;
                continue;
            }

            // Skip rows without a usable email
            if (isBlank(rawEmail)) {
                skippedInvalid++;
                logger.debug("[Upload] Skipped row — empty email, name='{}'", rawName);
                continue;
            }

            String emailNorm = rawEmail.toLowerCase(Locale.ROOT).trim();

            // Basic "@" check
            if (!emailNorm.contains("@")) {
                skippedInvalid++;
                logger.debug("[Upload] Skipped row — invalid email format: '{}'", emailNorm);
                continue;
            }

            // ── DUPLICATE CHECK — O(1) HashSet lookup ──
            if (seenEmails.contains(emailNorm)) {
                duplicates++;
                logger.debug("[Upload] Duplicate skipped: {}", emailNorm);
                continue;
            }

            // New record — add to HashSet immediately so later rows in the same file
            // that share this email are also caught as duplicates.
            seenEmails.add(emailNorm);

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

        logger.info("[Upload] Dedup complete — new={}, duplicates={}, invalid={}",
                newRecords, duplicates, skippedInvalid);

        // ── STEP 5: Bulk-insert in batches ──
        int batchCount = 0;
        if (!toInsert.isEmpty()) {
            for (int i = 0; i < toInsert.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, toInsert.size());
                mongoTemplate.insertAll(toInsert.subList(i, end));
                batchCount++;
                logger.info("[Upload] Inserted batch {}/{} ({} records)",
                        batchCount,
                        (int) Math.ceil((double) toInsert.size() / BATCH_SIZE),
                        end - i);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[Upload] DONE in {}ms — total={}, new={}, dup={}, invalid={}, batches={}",
                elapsed, totalRecords, newRecords, duplicates, skippedInvalid, batchCount);

        // ── STEP 6: Persist stats + activity log ──
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
     * Parse the first sheet of an xlsx/xls file.
     * Row 0 is the header — skipped.
     * Returns a list of [name, email] string pairs.
     */
    private List<String[]> parseExcelRows(byte[] fileBytes) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            // Use a FormulaEvaluator so FORMULA cells return their computed value
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.setIgnoreMissingWorkbooks(true);

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;

            for (Row row : sheet) {
                if (firstRow) {          // skip header
                    firstRow = false;
                    continue;
                }
                if (row == null) continue;

                String name  = getCellValueSafe(row.getCell(0), evaluator);
                String email = getCellValueSafe(row.getCell(1), evaluator);
                rows.add(new String[]{name, email});
            }
        }
        return rows;
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
