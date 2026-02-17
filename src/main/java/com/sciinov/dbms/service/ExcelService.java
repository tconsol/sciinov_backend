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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class ExcelService {
    @Autowired
    private DashboardDataRepository dashboardDataRepository;

    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    public void processExcelFile(MultipartFile file, String conferenceId, String dashboardMasterId) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User admin = userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate if admin has access to this conference
        if (admin.getRole() == User.Role.ADMIN && (admin.getConferenceIds() == null || !admin.getConferenceIds().contains(conferenceId))) {
             throw new RuntimeException("Access Denied: You are not assigned to this conference.");
        }

        List<DashboardData> newDataList = new ArrayList<>();
        int totalRecords = 0;
        int newRecords = 0;
        int duplicates = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                totalRecords++;

                String name = getCellValue(currentRow.getCell(0));
                String email = getCellValue(currentRow.getCell(1));
                String region = getCellValue(currentRow.getCell(2));  // Optional region column
                String country = getCellValue(currentRow.getCell(3)); // Optional country column

                if (name != null && email != null && !name.isEmpty() && !email.isEmpty()) {
                    Optional<DashboardData> existingData = dashboardDataRepository
                            .findByConferenceIdAndDashboardMasterIdAndEmail(conferenceId, dashboardMasterId, email);

                    if (existingData.isPresent()) {
                        duplicates++;
                    } else {
                        DashboardData data = new DashboardData();
                        data.setConferenceId(conferenceId);
                        data.setDashboardMasterId(dashboardMasterId);
                        data.setName(name);
                        data.setEmail(email);
                        data.setRegion(region != null && !region.isEmpty() ? region : null);
                        data.setCountry(country != null && !country.isEmpty() ? country : null);
                        data.setStatus(true);
                        data.setCreatedAt(LocalDateTime.now());
                        data.setUpdatedAt(LocalDateTime.now());
                        
                        // Generate Serial No
                        long nextSerialNo = getNextSerialNo(conferenceId, dashboardMasterId);
                        data.setSerialNo(nextSerialNo);

                        newDataList.add(data);
                        newRecords++;
                        
                        // Save immediately to ensure serial number consistency if processing large files sequentially
                        // For bulk performance, we might want to optimize this, but for correctness with serial numbers, sequential save or pre-allocation is safer.
                        // Here we save one by one to keep it simple and correct.
                        dashboardDataRepository.save(data);
                    }
                }
            }
        }

        // Save Stats
        DashboardUploadStats stats = new DashboardUploadStats();
        stats.setAdminId(admin.getId());
        stats.setConferenceId(conferenceId);
        stats.setDashboardMasterId(dashboardMasterId);
        stats.setUploadedAt(LocalDateTime.now());
        stats.setTotalRecordsInFile(totalRecords);
        stats.setNewRecordsAdded(newRecords);
        stats.setDuplicateRecordsIgnored(duplicates);
        stats.setFileName(file.getOriginalFilename());
        dashboardUploadStatsRepository.save(stats);

        // Log Activity
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(admin.getId());
        log.setConferenceId(conferenceId);
        log.setDashboardMasterId(dashboardMasterId);
        log.setActionType(AdminActivityLog.ActionType.UPLOAD_EXCEL);
        log.setDescription("Uploaded Excel: " + file.getOriginalFilename() + ". Added: " + newRecords + ", Duplicates: " + duplicates);
        log.setCreatedAt(LocalDateTime.now());
        // IP Address would typically come from request context, simplified here
        log.setIpAddress("127.0.0.1"); 
        adminActivityLogRepository.save(log);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
    
    private synchronized long getNextSerialNo(String conferenceId, String dashboardMasterId) {
        Optional<DashboardData> lastData = dashboardDataRepository.findTopByConferenceIdAndDashboardMasterIdOrderBySerialNoDesc(conferenceId, dashboardMasterId);
        return lastData.map(data -> data.getSerialNo() + 1).orElse(1L);
    }
}
