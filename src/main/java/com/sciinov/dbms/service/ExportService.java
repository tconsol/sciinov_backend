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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportService {
    @Autowired
    private DashboardDataRepository dashboardDataRepository;
    
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

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
     * Advanced export to Excel with date and region filtering
     */
    public void exportToExcelWithFilters(HttpServletResponse response, ExportFilterRequest filterRequest) throws IOException {
        List<DashboardData> dataList = getFilteredData(filterRequest);
        generateExcelFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_EXCEL);
    }

    /**
     * Advanced export to PDF with date and region filtering
     */
    public void exportToPdfWithFilters(HttpServletResponse response, ExportFilterRequest filterRequest) throws IOException {
        List<DashboardData> dataList = getFilteredData(filterRequest);
        generatePdfFile(response, dataList, filterRequest, AdminActivityLog.ActionType.DOWNLOAD_PDF);
    }

    /**
     * Get filtered data based on multiple criteria using dynamic query building
     */
    public List<DashboardData> getFilteredData(ExportFilterRequest filterRequest) {
        Query query = new Query();

        // Required filters
        query.addCriteria(Criteria.where("conferenceId").is(filterRequest.getConferenceId()));
        query.addCriteria(Criteria.where("dashboardMasterId").is(filterRequest.getDashboardMasterId()));
        query.addCriteria(Criteria.where("deleted").is(false));

        // Serial number range filter
        if (filterRequest.getFromSerialNo() != null && filterRequest.getToSerialNo() != null) {
            query.addCriteria(Criteria.where("serialNo")
                    .gte(filterRequest.getFromSerialNo())
                    .lte(filterRequest.getToSerialNo()));
        }

        // Date range filter (for upload/created date)
        if (filterRequest.getStartDate() != null && filterRequest.getEndDate() != null) {
            LocalDateTime startDateTime = filterRequest.getStartDate().atStartOfDay();
            LocalDateTime endDateTime = filterRequest.getEndDate().atTime(LocalTime.MAX);
            query.addCriteria(Criteria.where("createdAt")
                    .gte(startDateTime)
                    .lte(endDateTime));
        } else if (filterRequest.getStartDate() != null) {
            LocalDateTime startDateTime = filterRequest.getStartDate().atStartOfDay();
            query.addCriteria(Criteria.where("createdAt").gte(startDateTime));
        } else if (filterRequest.getEndDate() != null) {
            LocalDateTime endDateTime = filterRequest.getEndDate().atTime(LocalTime.MAX);
            query.addCriteria(Criteria.where("createdAt").lte(endDateTime));
        }

        // Region filter (case-insensitive)
        if (filterRequest.getRegion() != null && !filterRequest.getRegion().isEmpty()) {
            query.addCriteria(Criteria.where("region").regex(filterRequest.getRegion(), "i"));
        }

        // Country filter (case-insensitive)
        if (filterRequest.getCountry() != null && !filterRequest.getCountry().isEmpty()) {
            query.addCriteria(Criteria.where("country").regex(filterRequest.getCountry(), "i"));
        }

        // Email domain filter (e.g., "gmail.com" matches "user@gmail.com")
        if (filterRequest.getEmailDomain() != null && !filterRequest.getEmailDomain().isEmpty()) {
            String domainPattern = "@" + filterRequest.getEmailDomain().trim().toLowerCase();
            query.addCriteria(Criteria.where("email").regex(domainPattern, "i"));
        }

        // Sort by serial number
        query.with(Sort.by(Sort.Direction.ASC, "serialNo"));

        return mongoTemplate.find(query, DashboardData.class);
    }

    /**
     * Get list of distinct regions for a conference/dashboard
     */
    public List<String> getDistinctRegions(String conferenceId, String dashboardMasterId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(Criteria.where("region").ne(null));

        return mongoTemplate.findDistinct(query, "region", DashboardData.class, String.class)
                .stream()
                .filter(r -> r != null && !r.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Get list of distinct countries for a conference/dashboard
     */
    public List<String> getDistinctCountries(String conferenceId, String dashboardMasterId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conferenceId").is(conferenceId));
        query.addCriteria(Criteria.where("dashboardMasterId").is(dashboardMasterId));
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(Criteria.where("country").ne(null));

        return mongoTemplate.findDistinct(query, "country", DashboardData.class, String.class)
                .stream()
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toList());
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
        headerRow.createCell(3).setCellValue("Region");
        headerRow.createCell(4).setCellValue("Country");
        headerRow.createCell(5).setCellValue("Upload Date");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int rowIdx = 1;
        for (DashboardData data : dataList) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(data.getSerialNo() != null ? data.getSerialNo() : 0);
            row.createCell(1).setCellValue(data.getName() != null ? data.getName() : "");
            row.createCell(2).setCellValue(data.getEmail() != null ? data.getEmail() : "");
            row.createCell(3).setCellValue(data.getRegion() != null ? data.getRegion() : "");
            row.createCell(4).setCellValue(data.getCountry() != null ? data.getCountry() : "");
            row.createCell(5).setCellValue(data.getCreatedAt() != null ? data.getCreatedAt().format(formatter) : "");
        }

        // Auto-size columns
        for (int i = 0; i < 6; i++) {
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

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] {1.0f, 2.5f, 3.0f, 1.5f, 1.5f, 2.0f});
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

        cell.setPhrase(new Phrase("Region", font));
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
            table.addCell(data.getRegion() != null ? data.getRegion() : "");
            table.addCell(data.getCountry() != null ? data.getCountry() : "");
            table.addCell(data.getCreatedAt() != null ? data.getCreatedAt().format(formatter) : "");
        }
    }
    
    /**
     * Build a human-readable filter summary from the filter request.
     */
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
        if (fr.getRegion() != null && !fr.getRegion().isEmpty()) {
            sb.append("Region: ").append(fr.getRegion()).append("; ");
        }
        if (fr.getCountry() != null && !fr.getCountry().isEmpty()) {
            sb.append("Country: ").append(fr.getCountry()).append("; ");
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
        if (fr.getRegion() != null && !fr.getRegion().isEmpty()) {
            desc.append(" | Region: ").append(fr.getRegion());
        }
        if (fr.getCountry() != null && !fr.getCountry().isEmpty()) {
            desc.append(" | Country: ").append(fr.getCountry());
        }
        log.setDescription(desc.toString());

        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        adminActivityLogRepository.save(log);
    }

    /**
     * Public method to log a VIEW (data preview) action from DashboardDataController.
     */
    public void logViewAction(ExportFilterRequest fr, long totalRecords) {
        logDetailedAction(fr, AdminActivityLog.ActionType.VIEW, totalRecords);
    }
}
