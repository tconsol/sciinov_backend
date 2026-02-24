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
