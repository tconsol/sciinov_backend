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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    private static final int BATCH_SIZE = 500;

    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Synchronous version for backward compatibility.
     * Reads the file bytes eagerly then delegates to async bulk processor.
     */
    public Map<String, Object> processExcelFile(MultipartFile file, String conferenceId, String dashboardMasterId) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User admin = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() == User.Role.ADMIN &&
                (admin.getConferenceIds() == null || !admin.getConferenceIds().contains(conferenceId))) {
            throw new RuntimeException("Access Denied: You are not assigned to this conference.");
        }

        // Eagerly read file bytes before passing to async method (MultipartFile becomes invalid after request ends)
        byte[] fileBytes = file.getBytes();
        String originalFileName = file.getOriginalFilename();

        return processBulkUpload(fileBytes, originalFileName, conferenceId, dashboardMasterId, admin);
    }

    /**
     * Core bulk-upload logic:
     * 1. Load all existing emails in 1 query → HashSet (O(1) dup checks)
     * 2. Parse all rows in memory
     * 3. Pre-allocate serial numbers from current max (1 query)
     * 4. Bulk-insert in batches of BATCH_SIZE
     */
    private Map<String, Object> processBulkUpload(byte[] fileBytes, String fileName,
                                                    String conferenceId, String dashboardMasterId,
                                                    User admin) throws IOException {

        long startTime = System.currentTimeMillis();

        // ── Step 1: Load existing emails into a HashSet (single query, projection) ──
        logger.info("Loading existing emails for conferenceId={} dashboardMasterId={}", conferenceId, dashboardMasterId);
        Query emailQuery = new Query(Criteria.where("conferenceId").is(conferenceId)
                .and("dashboardMasterId").is(dashboardMasterId)
                .and("deleted").is(false));
        emailQuery.fields().include("email");
        List<DashboardData> existing = mongoTemplate.find(emailQuery, DashboardData.class);
        Set<String> existingEmails = existing.stream()
                .map(d -> d.getEmail() != null ? d.getEmail().toLowerCase().trim() : "")
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
        logger.info("Loaded {} existing emails", existingEmails.size());

        // ── Step 2: Parse Excel rows ──
        List<String[]> rows = parseExcelRows(fileBytes);
        int totalRecords = rows.size();
        logger.info("Parsed {} data rows from file: {}", totalRecords, fileName);

        // ── Step 3: Pre-allocate serial numbers ──
        long nextSerial = getNextSerialNoAtomic(conferenceId, dashboardMasterId);

        // ── Step 4: Build new records list ──
        List<DashboardData> newDataList = new ArrayList<>();
        int duplicates = 0;
        LocalDateTime now = LocalDateTime.now();

        for (String[] row : rows) {
            String name = row[0];
            String email = row[1];
            if (name == null || name.isEmpty() || email == null || email.isEmpty()) continue;

            String emailKey = email.toLowerCase().trim();
            if (existingEmails.contains(emailKey)) {
                duplicates++;
            } else {
                existingEmails.add(emailKey); // prevent in-batch duplicates
                DashboardData data = new DashboardData();
                data.setConferenceId(conferenceId);
                data.setDashboardMasterId(dashboardMasterId);
                data.setName(name.trim());
                data.setEmail(emailKey);
                data.setStatus(true);
                data.setCreatedAt(now);
                data.setUpdatedAt(now);
                data.setSerialNo(nextSerial++);
                newDataList.add(data);
            }
        }

        int newRecords = newDataList.size();
        logger.info("New records to insert: {}, duplicates skipped: {}", newRecords, duplicates);

        // ── Step 5: Bulk insert in batches ──
        if (!newDataList.isEmpty()) {
            int batchCount = 0;
            for (int i = 0; i < newDataList.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, newDataList.size());
                List<DashboardData> batch = newDataList.subList(i, end);
                mongoTemplate.insertAll(batch);
                batchCount++;
                logger.debug("Inserted batch {}: records {}-{}", batchCount, i + 1, end);
            }
            logger.info("Bulk insert complete: {} batches, {} records", batchCount, newRecords);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("Upload complete in {}ms: total={}, new={}, dup={}", elapsed, totalRecords, newRecords, duplicates);

        // ── Step 6: Save stats & activity log ──
        saveStats(admin, conferenceId, dashboardMasterId, fileName, totalRecords, newRecords, duplicates);
        logUploadActivity(admin, conferenceId, dashboardMasterId, fileName, newRecords, duplicates);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "File uploaded successfully");
        result.put("fileName", fileName);
        result.put("totalRecordsInFile", totalRecords);
        result.put("newRecordsAdded", newRecords);
        result.put("duplicateRecordsIgnored", duplicates);
        result.put("processingTimeMs", elapsed);
        return result;
    }

    /**
     * Parse Excel file using streaming-friendly POI API.
     * Returns list of [name, email] arrays.
     */
    private List<String[]> parseExcelRows(byte[] fileBytes) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIter = sheet.iterator();
            if (rowIter.hasNext()) rowIter.next(); // skip header

            while (rowIter.hasNext()) {
                Row row = rowIter.next();
                String name = getCellValue(row.getCell(0));
                String email = getCellValue(row.getCell(1));
                rows.add(new String[]{name, email});
            }
        }
        return rows;
    }

    /**
     * Get next serial number atomically (single DB query).
     */
    private synchronized long getNextSerialNoAtomic(String conferenceId, String dashboardMasterId) {
        return dashboardDataRepository
                .findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(conferenceId, dashboardMasterId)
                .map(d -> d.getSerialNo() + 1)
                .orElse(1L);
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
        log.setDescription("Uploaded Excel: " + fileName + ". Added: " + added + ", Duplicates: " + duplicates);
        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        // Save + push to SSE in real-time
        analyticsService.saveAndPushLog(log);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                // Return as integer string if it's a whole number
                double val = cell.getNumericCellValue();
                return val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue().trim(); } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default: return "";
        }
    }
}
