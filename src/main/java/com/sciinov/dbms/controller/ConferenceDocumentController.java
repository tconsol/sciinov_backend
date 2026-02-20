package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ConferenceDocumentResponse;
import com.sciinov.dbms.entity.ConferenceDocument;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ConferenceDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conference-documents")
@Tag(name = "Conference Documents", description = "APIs for managing conference documents (Program, Book, Positive Sheets)")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ConferenceDocumentController {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceDocumentController.class);

    @Autowired
    private ConferenceDocumentService conferenceDocumentService;

    // ─────────────────────────────────────────────────────────────────
    // Helper: get authenticated user details
    // ─────────────────────────────────────────────────────────────────
    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Validate that an ADMIN is assigned to the given conferenceId.
     * SUPER_ADMIN is always allowed.
     */
    private void validateConferenceAccess(String conferenceId) {
        UserDetailsImpl ud = getCurrentUser();
        User user = ud.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (user.getConferenceIds() == null || !user.getConferenceIds().contains(conferenceId)) {
                throw new AccessDeniedException("Access Denied: You are not assigned to conference " + conferenceId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UPLOAD
    // POST /api/conference-documents/upload
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Upload conference document",
        description = "Upload Program, Book, or Positive Sheets. If file already exists for this conference/year/type, it will be replaced.")
    public ResponseEntity<?> uploadDocument(
            @RequestParam String conferenceId,
            @RequestParam Integer year,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Validate admin is assigned to this conference
            validateConferenceAccess(conferenceId);

            UserDetailsImpl ud = getCurrentUser();
            String userId   = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            String ipAddress = getClientIpAddress(xForwardedFor, request);

            logger.info("Uploading document - Conference: {}, Year: {}, Type: {}, User: {}, IP: {}",
                conferenceId, year, documentType, userName, ipAddress);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is empty"));
            }

            ConferenceDocument.DocumentType type;
            try {
                type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid document type. Must be: PROGRAM, BOOK, or POSITIVE_SHEETS",
                    "validTypes", List.of("PROGRAM", "BOOK", "POSITIVE_SHEETS")
                ));
            }

            int currentYear = java.time.Year.now().getValue();
            if (year < 2000 || year > currentYear + 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Year must be between 2000 and " + (currentYear + 10)
                ));
            }

            ConferenceDocument doc = conferenceDocumentService.uploadDocument(
                conferenceId, year, type, file, userId, userName, ipAddress);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document uploaded successfully. If a previous version existed, it has been replaced.",
                "data", new ConferenceDocumentResponse(doc)
            ));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to upload document: " + e.getMessage()));
        } catch (RuntimeException e) {
            logger.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Unexpected error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET all documents for a conference
    // GET /api/conference-documents?conferenceId={id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conference documents")
    public ResponseEntity<?> getConferenceDocuments(@RequestParam String conferenceId) {
        try {
            validateConferenceAccess(conferenceId);
            List<ConferenceDocument> docs = conferenceDocumentService.getConferenceDocuments(conferenceId);
            List<ConferenceDocumentResponse> responses = docs.stream().map(ConferenceDocumentResponse::new).toList();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalDocuments", responses.size(),
                "data", responses
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET documents by year for a conference
    // GET /api/conference-documents/year?conferenceId={id}&year={year}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/year")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get documents by year")
    public ResponseEntity<?> getConferenceDocumentsByYear(
            @RequestParam String conferenceId,
            @RequestParam Integer year) {
        try {
            validateConferenceAccess(conferenceId);
            List<ConferenceDocument> docs = conferenceDocumentService.getConferenceDocumentsByYear(conferenceId, year);
            List<ConferenceDocumentResponse> responses = docs.stream().map(ConferenceDocumentResponse::new).toList();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conference", conferenceId,
                "year", year,
                "totalDocuments", responses.size(),
                "data", responses
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching documents by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET documents by year across all conferences (SUPER_ADMIN only)
    // GET /api/conference-documents/year-all?year={year}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/year-all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get documents by year (all conferences)")
    public ResponseEntity<?> getDocumentsByYear(@RequestParam Integer year) {
        try {
            List<ConferenceDocument> docs = conferenceDocumentService.getDocumentsByYear(year);
            List<ConferenceDocumentResponse> responses = docs.stream().map(ConferenceDocumentResponse::new).toList();
            Map<String, List<ConferenceDocumentResponse>> grouped = responses.stream()
                .collect(Collectors.groupingBy(ConferenceDocumentResponse::getConferenceName));
            return ResponseEntity.ok(Map.of(
                "success", true,
                "year", year,
                "totalDocuments", responses.size(),
                "conferencesCount", grouped.size(),
                "dataByConference", grouped
            ));
        } catch (Exception e) {
            logger.error("Error fetching documents by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET documents by type
    // GET /api/conference-documents/type?conferenceId={id}&documentType={type}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/type")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get documents by type")
    public ResponseEntity<?> getDocumentsByType(
            @RequestParam String conferenceId,
            @RequestParam String documentType) {
        try {
            validateConferenceAccess(conferenceId);
            ConferenceDocument.DocumentType type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
            List<ConferenceDocument> docs = conferenceDocumentService.getDocumentsByType(conferenceId, type);
            List<ConferenceDocumentResponse> responses = docs.stream().map(ConferenceDocumentResponse::new).toList();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conferenceId", conferenceId,
                "documentType", type.getDisplayName(),
                "totalDocuments", responses.size(),
                "data", responses
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid document type. Must be: PROGRAM, BOOK, or POSITIVE_SHEETS"
            ));
        } catch (Exception e) {
            logger.error("Error fetching documents by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SEARCH with filters
    // POST /api/conference-documents/search
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search documents with filters")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false) String conferenceId,
            @RequestParam(required = false) String conferenceName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String documentType,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            if (conferenceId != null && !conferenceId.isEmpty()) {
                validateConferenceAccess(conferenceId);
            }

            ConferenceDocument.DocumentType type = null;
            if (documentType != null && !documentType.isEmpty()) {
                try {
                    type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid document type"));
                }
            }

            Page<ConferenceDocument> page = conferenceDocumentService.getDocumentsWithFilter(
                conferenceId, conferenceName, year, type, pageNumber, pageSize);
            List<ConferenceDocumentResponse> responses = page.getContent().stream()
                .map(ConferenceDocumentResponse::new).toList();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "pageNumber", pageNumber,
                "pageSize", pageSize,
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "data", responses
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error searching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to search documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET document by ID
    // GET /api/conference-documents/{id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<?> getDocumentById(@PathVariable String id) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Document not found"));
            }
            validateConferenceAccess(docOpt.get().getConferenceId());
            return ResponseEntity.ok(Map.of("success", true, "data", new ConferenceDocumentResponse(docOpt.get())));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch document: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DOWNLOAD document
    // GET /api/conference-documents/{id}/download
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download document file")
    public ResponseEntity<?> downloadDocument(
            @PathVariable String id,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Document not found"));
            }

            ConferenceDocument doc = docOpt.get();
            validateConferenceAccess(doc.getConferenceId());

            UserDetailsImpl ud = getCurrentUser();
            String userId   = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            String ipAddress = getClientIpAddress(xForwardedFor, request);

            // Download file and log activity
            byte[] fileContent = conferenceDocumentService.downloadDocument(id, userId, userName, ipAddress);

            if (fileContent == null || fileContent.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "File is empty or corrupted"));
            }

            String contentType = doc.getContentType() != null ? doc.getContentType() : getContentType(doc.getFileName());
            String fileName = sanitizeFileName(doc.getFileName());

            logger.info("Document downloaded - ID: {}, File: {}, Size: {} bytes, User: {}", id, doc.getFileName(), fileContent.length, userName);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(new org.springframework.core.io.InputStreamResource(new java.io.ByteArrayInputStream(fileContent)));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error downloading document {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to download document: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error downloading document {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Unexpected error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Alias download endpoint (proxy style)
    // GET /api/conference-documents/download/{id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/download/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download conference document (proxy)")
    public ResponseEntity<?> downloadDocumentProxy(
            @PathVariable String id,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        // Delegate to the main download endpoint logic
        return downloadDocument(id, xForwardedFor, request);
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE document — removes from GCS bucket AND database
    // DELETE /api/conference-documents/{id}
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete document",
        description = "Hard-deletes the document from both the GCS bucket and the database.")
    public ResponseEntity<?> deleteDocument(
            @PathVariable String id,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Document not found"));
            }

            validateConferenceAccess(docOpt.get().getConferenceId());

            UserDetailsImpl ud = getCurrentUser();
            String userId   = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            String ipAddress = getClientIpAddress(xForwardedFor, request);

            // Hard-delete from GCS + DB, log activity
            conferenceDocumentService.deleteDocument(id, userId, userName, ipAddress);

            logger.info("Document deleted - ID: {}, User: {}", id, userName);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document deleted successfully from bucket and database."
            ));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting document {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to delete document: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET available years for a conference
    // GET /api/conference-documents/available-years?conferenceId={id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/available-years")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get available years")
    public ResponseEntity<?> getAvailableYears(@RequestParam String conferenceId) {
        try {
            validateConferenceAccess(conferenceId);
            List<Integer> years = conferenceDocumentService.getAvailableYears(conferenceId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conferenceId", conferenceId,
                "availableYears", years,
                "count", years.size()
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching available years: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch available years: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET admin-specific documents data
    // GET /api/conference-documents/admin/dashboard
    // ADMIN only - scoped to their assigned conferences
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get admin's conference documents dashboard data",
        description = "Returns comprehensive document data for all admin's assigned conferences including stats, recent uploads, and breakdowns by year/type")
    public ResponseEntity<?> getAdminDocumentsDashboard() {
        try {
            UserDetailsImpl ud = getCurrentUser();
            List<String> conferenceIds = ud.getUser().getConferenceIds();

            if (conferenceIds == null || conferenceIds.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "No conferences assigned",
                    "totalConferences", 0,
                    "totalDocuments", 0,
                    "conferencesData", List.of()
                ));
            }

            Map<String, Object> data = conferenceDocumentService.getAdminConferenceDocumentsData(conferenceIds);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "adminId", ud.getId(),
                "adminName", ud.getUser().getFirstName() + " " + ud.getUser().getLastName(),
                "data", data
            ));

        } catch (Exception e) {
            logger.error("Error fetching admin documents dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch dashboard data: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET document statistics for a conference
    // GET /api/conference-documents/statistics?conferenceId={id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document statistics for a conference")
    public ResponseEntity<?> getStatistics(@RequestParam String conferenceId) {
        try {
            validateConferenceAccess(conferenceId);
            Map<String, Object> stats = conferenceDocumentService.getDocumentStatistics(conferenceId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conferenceId", conferenceId,
                "statistics", stats
            ));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch statistics: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET global document statistics across ALL conferences
    // GET /api/conference-documents/statistics/global
    // SUPER_ADMIN only
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/statistics/global")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get global document statistics across all conferences")
    public ResponseEntity<?> getGlobalStatistics() {
        try {
            Map<String, Object> stats = conferenceDocumentService.getGlobalDocumentStatistics();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "statistics", stats
            ));
        } catch (Exception e) {
            logger.error("Error fetching global statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch global statistics: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────
    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String n = fileName.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".doc")) return "application/msword";
        if (n.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (n.endsWith(".xls")) return "application/vnd.ms-excel";
        if (n.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (n.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (n.endsWith(".txt")) return "text/plain";
        if (n.endsWith(".csv")) return "text/csv";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    private String getClientIpAddress(String xForwardedFor, jakarta.servlet.http.HttpServletRequest request) {
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String ip = request.getHeader("X-Real-IP");
        return (ip == null || ip.isEmpty()) ? request.getRemoteAddr() : ip;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "document";
        return fileName.replace("\"", "\\\"").replace("\n", "").replace("\r", "");
    }
}

