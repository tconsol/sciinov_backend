package com.subbarao.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.subbarao.backend.entity.AdminActivityLog;
import com.subbarao.backend.entity.DashboardData;
import com.subbarao.backend.entity.User;
import com.subbarao.backend.repository.AdminActivityLogRepository;
import com.subbarao.backend.repository.DashboardDataRepository;
import com.subbarao.backend.repository.UserRepository;
import com.subbarao.backend.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExportService {
    @Autowired
    private DashboardDataRepository dashboardDataRepository;
    
    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    public void exportToExcel(HttpServletResponse response, String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo) throws IOException {
        List<DashboardData> dataList = dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dashboard Data");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Serial No");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("Email");

        int rowIdx = 1;
        for (DashboardData data : dataList) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(data.getSerialNo());
            row.createCell(1).setCellValue(data.getName());
            row.createCell(2).setCellValue(data.getEmail());
        }

        logExportAction(conferenceId, dashboardMasterId, AdminActivityLog.ActionType.DOWNLOAD_EXCEL);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    public void exportToPdf(HttpServletResponse response, String conferenceId, String dashboardMasterId, Long fromSerialNo, Long toSerialNo) throws IOException {
        List<DashboardData> dataList = dashboardDataRepository.findByConferenceIdAndDashboardMasterIdAndSerialNoBetween(
                conferenceId, dashboardMasterId, fromSerialNo, toSerialNo, Sort.by(Sort.Direction.ASC, "serialNo"));

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setSize(18);
        font.setColor(java.awt.Color.BLUE);

        Paragraph p = new Paragraph("Dashboard Data Report", font);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(p);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] {1.5f, 3.5f, 3.0f});
        table.setSpacingBefore(10);

        writePdfHeader(table);
        writePdfData(table, dataList);

        document.add(table);
        
        logExportAction(conferenceId, dashboardMasterId, AdminActivityLog.ActionType.DOWNLOAD_PDF);
        
        document.close();
    }

    private void writePdfHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(java.awt.Color.BLUE);
        cell.setPadding(5);

        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        font.setColor(java.awt.Color.WHITE);

        cell.setPhrase(new Phrase("Serial No", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Name", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Email", font));
        table.addCell(cell);
    }

    private void writePdfData(PdfPTable table, List<DashboardData> dataList) {
        for (DashboardData data : dataList) {
            table.addCell(String.valueOf(data.getSerialNo()));
            table.addCell(data.getName());
            table.addCell(data.getEmail());
        }
    }
    
    private void logExportAction(String conferenceId, String dashboardMasterId, AdminActivityLog.ActionType actionType) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(userDetails.getId());
        log.setConferenceId(conferenceId);
        log.setDashboardMasterId(dashboardMasterId);
        log.setActionType(actionType);
        log.setDescription("Exported data");
        log.setCreatedAt(LocalDateTime.now());
        log.setIpAddress("127.0.0.1"); // Simplified
        adminActivityLogRepository.save(log);
    }
}
