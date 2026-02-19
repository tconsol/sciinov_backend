package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ConferenceDocumentResponse;
import com.sciinov.dbms.entity.ConferenceDocument;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Upload or replace conference document
     * POST /api/conference-documents/upload
     *
     * If document already exists for conference/year/type, it will be replaced
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Upload conference document",
        description = "Upload Program, Book, or Positive Sheets. If file already exists for this conference/year/type, it will be replaced.")
    public ResponseEntity<?> uploadDocument(
            @RequestParam String conferenceId,
            @RequestParam Integer year,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Name") String userName,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // Get IP address from request
            String ipAddress = getClientIpAddress(xForwardedFor, request);

            logger.info("Uploading conference document - Conference: {}, Year: {}, Type: {}, User: {}, IP: {}",
                conferenceId, year, documentType, userName, ipAddress);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "File is empty");
                }});
            }

            // Validate document type
            ConferenceDocument.DocumentType type;
            try {
                type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Invalid document type. Must be: PROGRAM, BOOK, or POSITIVE_SHEETS");
                    put("validTypes", Arrays.asList("PROGRAM", "BOOK", "POSITIVE_SHEETS"));
                }});
            }

            // Validate year
            int currentYear = java.time.Year.now().getValue();
            if (year < 2000 || year > currentYear + 10) {
                return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Year must be between 2000 and " + (currentYear + 10));
                }});
            }

            // Upload document
            ConferenceDocument doc = conferenceDocumentService.uploadDocument(
                conferenceId, year, type, file, userId, userName, ipAddress);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Document uploaded successfully. If a previous version existed, it has been replaced.");
                put("data", new ConferenceDocumentResponse(doc));
            }});

        } catch (IOException e) {
            logger.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to upload document: " + e.getMessage());
            }});
        } catch (RuntimeException e) {
            logger.error("Error uploading document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", e.getMessage());
            }});
        } catch (Exception e) {
            logger.error("Unexpected error uploading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Unexpected error: " + e.getMessage());
            }});
        }
    }

    /**
     * Get all documents for a conference
     * GET /api/conference-documents?conferenceId={id}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conference documents", description = "Get all documents for a conference")
    public ResponseEntity<?> getConferenceDocuments(
            @RequestParam String conferenceId) {
        try {
            logger.info("Fetching documents for conference: {}", conferenceId);

            List<ConferenceDocument> docs = conferenceDocumentService.getConferenceDocuments(conferenceId);
            List<ConferenceDocumentResponse> responses = docs.stream()
                .map(ConferenceDocumentResponse::new)
                .toList();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("totalDocuments", responses.size());
                put("data", responses);
            }});
        } catch (Exception e) {
            logger.error("Error fetching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch documents: " + e.getMessage());
            }});
        }
    }

    /**
     * Get documents for specific conference and year
     * GET /api/conference-documents/year?conferenceId={id}&year={year}
     */
    @GetMapping("/year")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get documents by year",
        description = "Get all documents (Program, Book, Positive Sheets) for a conference in a specific year")
    public ResponseEntity<?> getConferenceDocumentsByYear(
            @RequestParam String conferenceId,
            @RequestParam Integer year) {
        try {
            logger.info("Fetching documents for conference: {}, Year: {}", conferenceId, year);

            List<ConferenceDocument> docs = conferenceDocumentService.getConferenceDocumentsByYear(conferenceId, year);
            List<ConferenceDocumentResponse> responses = docs.stream()
                .map(ConferenceDocumentResponse::new)
                .toList();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("conference", conferenceId);
                put("year", year);
                put("totalDocuments", responses.size());
                put("data", responses);
            }});
        } catch (Exception e) {
            logger.error("Error fetching documents by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch documents: " + e.getMessage());
            }});
        }
    }

    /**
     * Get documents by year (across all conferences)
     * GET /api/conference-documents/year-all?year={year}
     */
    @GetMapping("/year-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get documents by year (all conferences)",
        description = "Get all documents across all conferences for a specific year")
    public ResponseEntity<?> getDocumentsByYear(@RequestParam Integer year) {
        try {
            logger.info("Fetching all documents for year: {}", year);

            List<ConferenceDocument> docs = conferenceDocumentService.getDocumentsByYear(year);
            List<ConferenceDocumentResponse> responses = docs.stream()
                .map(ConferenceDocumentResponse::new)
                .toList();

            // Group by conference
            Map<String, List<ConferenceDocumentResponse>> groupedByConference = responses.stream()
                .collect(Collectors.groupingBy(ConferenceDocumentResponse::getConferenceName));

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("year", year);
                put("totalDocuments", responses.size());
                put("conferencesCount", groupedByConference.size());
                put("dataByConference", groupedByConference);
            }});
        } catch (Exception e) {
            logger.error("Error fetching documents by year: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch documents: " + e.getMessage());
            }});
        }
    }

    /**
     * Get documents by type
     * GET /api/conference-documents/type?conferenceId={id}&documentType={type}
     */
    @GetMapping("/type")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get documents by type",
        description = "Get all documents of a specific type (PROGRAM, BOOK, POSITIVE_SHEETS) for a conference")
    public ResponseEntity<?> getDocumentsByType(
            @RequestParam String conferenceId,
            @RequestParam String documentType) {
        try {
            ConferenceDocument.DocumentType type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
            logger.info("Fetching {} documents for conference: {}", documentType, conferenceId);

            List<ConferenceDocument> docs = conferenceDocumentService.getDocumentsByType(conferenceId, type);
            List<ConferenceDocumentResponse> responses = docs.stream()
                .map(ConferenceDocumentResponse::new)
                .toList();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("conferenceId", conferenceId);
                put("documentType", type.getDisplayName());
                put("totalDocuments", responses.size());
                put("data", responses);
            }});
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Invalid document type. Must be: PROGRAM, BOOK, or POSITIVE_SHEETS");
            }});
        } catch (Exception e) {
            logger.error("Error fetching documents by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch documents: " + e.getMessage());
            }});
        }
    }

    /**
     * Advanced filter for documents
     * POST /api/conference-documents/search
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search documents with filters",
        description = "Search documents with advanced filtering by conference, year, type")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false) String conferenceId,
            @RequestParam(required = false) String conferenceName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String documentType,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            logger.info("Searching documents - Conference: {}, Year: {}, Type: {}, Page: {}/{}",
                conferenceId, year, documentType, pageNumber, pageSize);

            ConferenceDocument.DocumentType type = null;
            if (documentType != null && !documentType.isEmpty()) {
                try {
                    type = ConferenceDocument.DocumentType.valueOf(documentType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Invalid document type");
                    }});
                }
            }

            Page<ConferenceDocument> page = conferenceDocumentService.getDocumentsWithFilter(
                conferenceId, conferenceName, year, type, pageNumber, pageSize);

            List<ConferenceDocumentResponse> responses = page.getContent().stream()
                .map(ConferenceDocumentResponse::new)
                .toList();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("pageNumber", pageNumber);
                put("pageSize", pageSize);
                put("totalElements", page.getTotalElements());
                put("totalPages", page.getTotalPages());
                put("data", responses);
            }});
        } catch (Exception e) {
            logger.error("Error searching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to search documents: " + e.getMessage());
            }});
        }
    }

    /**
     * Get document by ID
     * GET /api/conference-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document by ID", description = "Get detailed information about a specific document")
    public ResponseEntity<?> getDocumentById(@PathVariable String id) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Document not found");
                }});
            }

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("data", new ConferenceDocumentResponse(docOpt.get()));
            }});
        } catch (Exception e) {
            logger.error("Error fetching document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch document: " + e.getMessage());
            }});
        }
    }

    /**
     * Download document
     * GET /api/conference-documents/{id}/download
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download document file", description = "Download the actual document file")
    public ResponseEntity<?> downloadDocument(@PathVariable String id) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Document not found");
                }});
            }

            ConferenceDocument doc = docOpt.get();
            byte[] fileContent = conferenceDocumentService.downloadDocument(id);

            // Use the filename from the metadata directly (avoid charset conversion issues)
            String fileName = doc.getFileName();

            logger.info("Downloaded document - ID: {}, Name: {}", id, doc.getFileName());

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(fileContent);
        } catch (IOException e) {
            logger.error("Error downloading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to download document: " + e.getMessage());
            }});
        } catch (Exception e) {
            logger.error("Unexpected error downloading document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Unexpected error: " + e.getMessage());
            }});
        }
    }

    /**
     * Delete document
     * DELETE /api/conference-documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete document", description = "Delete a document (soft delete)")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        try {
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Document not found");
                }});
            }

            conferenceDocumentService.deleteDocument(id);
            logger.info("Deleted document - ID: {}", id);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Document deleted successfully");
            }});
        } catch (Exception e) {
            logger.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to delete document: " + e.getMessage());
            }});
        }
    }

    /**
     * Get available years for a conference
     * GET /api/conference-documents/available-years?conferenceId={id}
     */
    @GetMapping("/available-years")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get available years",
        description = "Get all years for which documents are available for a conference")
    public ResponseEntity<?> getAvailableYears(@RequestParam String conferenceId) {
        try {
            List<Integer> years = conferenceDocumentService.getAvailableYears(conferenceId);
            logger.info("Retrieved available years for conference: {}", conferenceId);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("conferenceId", conferenceId);
                put("availableYears", years);
                put("count", years.size());
            }});
        } catch (Exception e) {
            logger.error("Error fetching available years: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch available years: " + e.getMessage());
            }});
        }
    }

    /**
     * Download conference document through backend proxy
     * GET /api/conference-documents/download/{id}
     *
     * This endpoint streams the file from GCS to the client, avoiding CORS issues
     * The file is authenticated and access-controlled through this endpoint
     */
    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download conference document",
        description = "Download a conference document through backend proxy (avoids CORS issues)")
    public ResponseEntity<?> downloadDocument(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String ipAddress = getClientIpAddress(xForwardedFor, request);
            logger.info("Download document requested - DocumentId: {}, User: {}, IP: {}", id, userId, ipAddress);

            // Get document metadata
            Optional<ConferenceDocument> docOpt = conferenceDocumentService.getDocumentById(id);
            if (docOpt.isEmpty()) {
                logger.warn("Download attempt for non-existent document - DocumentId: {}, User: {}", id, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Document not found");
                }});
            }

            ConferenceDocument doc = docOpt.get();

            // Download file from GCS
            byte[] fileBytes = conferenceDocumentService.downloadDocument(id);

            if (fileBytes == null || fileBytes.length == 0) {
                logger.error("Failed to download document from GCS - DocumentId: {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                    put("success", false);
                    put("message", "Failed to download document");
                }});
            }

            // Determine content type
            String contentType = getContentType(doc.getFileName());

            logger.info("Document download initiated - DocumentId: {}, FileName: {}, Size: {} bytes, User: {}",
                id, doc.getFileName(), fileBytes.length, userId);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + doc.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileBytes.length))
                .body(new org.springframework.core.io.InputStreamResource(new java.io.ByteArrayInputStream(fileBytes)));

        } catch (Exception e) {
            logger.error("Error downloading document (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to download document: " + e.getMessage());
            }});
        }
    }

    /**
     * Get document statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get document statistics",
        description = "Get statistics about documents for a conference")
    public ResponseEntity<?> getStatistics(@RequestParam String conferenceId) {
        try {
            Map<String, Object> stats = conferenceDocumentService.getDocumentStatistics(conferenceId);
            logger.info("Retrieved statistics for conference: {}", conferenceId);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("conferenceId", conferenceId);
                put("statistics", stats);
            }});
        } catch (Exception e) {
            logger.error("Error fetching statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<String, Object>() {{
                put("success", false);
                put("message", "Failed to fetch statistics: " + e.getMessage());
            }});
        }
    }

    /**
     * Determine content type based on file extension
     */
    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) return "application/pdf";
        if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) return "application/msword";
        if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lowerFileName.endsWith(".ppt") || lowerFileName.endsWith(".pptx")) return "application/vnd.ms-powerpoint";
        if (lowerFileName.endsWith(".txt")) return "text/plain";
        if (lowerFileName.endsWith(".csv")) return "text/csv";
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerFileName.endsWith(".png")) return "image/png";
        if (lowerFileName.endsWith(".zip")) return "application/zip";

        return "application/octet-stream";
    }

    /**
     * Helper method to get client IP address from request
     */
    private String getClientIpAddress(String xForwardedFor, jakarta.servlet.http.HttpServletRequest request) {
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}

