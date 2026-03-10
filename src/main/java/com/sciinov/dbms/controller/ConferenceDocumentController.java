package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.ConferenceDocumentResponse;
import com.sciinov.dbms.dto.ConferenceDocumentUpdateRequest;
import com.sciinov.dbms.dto.ConferenceDocumentFilterRequest;
import com.sciinov.dbms.entity.ConferenceDocument;
import com.sciinov.dbms.entity.ConferenceDocumentLog;
import com.sciinov.dbms.entity.DocumentTypeEntity;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.ConferenceDocumentLogService;
import com.sciinov.dbms.service.ConferenceDocumentService;
import com.sciinov.dbms.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conference-documents")
@Tag(name = "Conference Documents", description = "APIs for managing conference documents (Program, Book, Positive Sheets)")
public class ConferenceDocumentController {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceDocumentController.class);

    @Autowired
    private ConferenceDocumentService conferenceDocumentService;

    @Autowired
    private ConferenceDocumentLogService conferenceDocumentLogService;

    @Autowired
    private DocumentTypeService documentTypeService;

    // ─────────────────────────────────────────────────────────────────
    // Helper: get authenticated user details
    // ─────────────────────────────────────────────────────────────────
    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Validate that an ADMIN is assigned to the given conferenceId.
     * SUPER_ADMIN is always allowed (no check needed).
     */
    private void validateConferenceAccess(String conferenceId) {
        UserDetailsImpl ud = getCurrentUser();
        User user = ud.getUser();
        // SUPER_ADMIN has access to all conferences
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return;
        }
        // ADMIN must be assigned to the conference
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
        description = "Upload Program, Book, or Positive Sheets. Supports multiple Excel formats (.xls, .xlsx, .xlsm) and other documents. Multiple files for the same conference/year/type are allowed — existing files are never replaced.")
    public ResponseEntity<?> uploadDocument(
            @RequestParam String conferenceId,
            @RequestParam Integer year,
            @RequestParam String documentType,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            HttpServletRequest request) {
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

            // Resolve documentType: could be ID or slug
            // If it looks like a MongoDB ObjectId (24 hex chars), try to look it up
            String typeSlug = documentType.trim();
            if (isMongoObjectId(typeSlug)) {
                // Frontend sent the document type ID, look up the slug
                Optional<DocumentTypeEntity> docTypeOpt = documentTypeService.findById(typeSlug);
                if (docTypeOpt.isPresent()) {
                    typeSlug = docTypeOpt.get().getSlug();
                    logger.info("Resolved document type ID {} to slug: {}", documentType, typeSlug);
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid document type ID: '" + documentType + "'. Use GET /api/document-types/active to see available types."
                    ));
                }
            } else {
                // Frontend sent the slug, normalize it to lowercase
                typeSlug = typeSlug.toLowerCase().replace(' ', '_').replace('-', '_');
            }

            int currentYear = java.time.Year.now().getValue();
            if (year < 2000 || year > currentYear + 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Year must be between 2000 and " + (currentYear + 10)
                ));
            }

            ConferenceDocument doc = conferenceDocumentService.uploadDocument(
                conferenceId, year, typeSlug, file, userId, userName, ipAddress);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document uploaded successfully.",
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
            String typeSlug = documentType.trim().toLowerCase().replace(' ', '_').replace('-', '_');
            List<ConferenceDocument> docs = conferenceDocumentService.getDocumentsByType(conferenceId, typeSlug);
            List<ConferenceDocumentResponse> responses = docs.stream().map(ConferenceDocumentResponse::new).toList();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conferenceId", conferenceId,
                "documentType", typeSlug,
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

            String typeSlug = (documentType != null && !documentType.isEmpty())
                ? documentType.trim().toLowerCase().replace(' ', '_').replace('-', '_') : null;

            Page<ConferenceDocument> page = conferenceDocumentService.getDocumentsWithFilter(
                conferenceId, conferenceName, year, typeSlug, pageNumber, pageSize);
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
    // SUPER_ADMIN ENDPOINTS
    // ─────────────────────────────────────────────────────────────────

    /**
     * SUPER_ADMIN: Get all conference documents (across all conferences) with optional filters
     * GET /api/conference-documents/admin/all?year={year}&documentType={type}&conferenceId={id}
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all conference documents (Super Admin)",
        description = "SUPER_ADMIN only: Get all conference documents across all conferences with optional filters")
    public ResponseEntity<?> getAllConferenceDocuments(
            @RequestParam(required = false) String conferenceId,
            @RequestParam(required = false) String conferenceName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String documentType,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            List<ConferenceDocument> docs;

            if (conferenceId != null && !conferenceId.isEmpty()) {
                docs = conferenceDocumentService.getConferenceDocuments(conferenceId);
            } else if (year != null) {
                docs = conferenceDocumentService.getDocumentsByYear(year);
            } else {
                // Get all documents
                docs = conferenceDocumentService.getAllConferenceDocuments();
            }

            // Apply filters if provided
            if (conferenceName != null && !conferenceName.isEmpty()) {
                String searchName = conferenceName.toLowerCase();
                docs = docs.stream()
                    .filter(d -> d.getConferenceName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
            }

            if (documentType != null && !documentType.isEmpty()) {
                String typeSlug = documentType.trim().toLowerCase().replace(' ', '_').replace('-', '_');
                docs = docs.stream()
                    .filter(d -> d.getDocumentType().equalsIgnoreCase(typeSlug))
                    .collect(Collectors.toList());
            }

            // Paginate
            int start = pageNumber * pageSize;
            int end = Math.min(start + pageSize, docs.size());
            List<ConferenceDocument> paginated = docs.subList(start, Math.min(end, docs.size()));

            List<ConferenceDocumentResponse> responses = paginated.stream()
                .map(ConferenceDocumentResponse::new).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalRecords", docs.size(),
                "pageNumber", pageNumber,
                "pageSize", pageSize,
                "totalPages", (int) Math.ceil((double) docs.size() / pageSize),
                "data", responses
            ));
        } catch (Exception e) {
            logger.error("Error fetching all conference documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch documents: " + e.getMessage()));
        }
    }

    /**
     * SUPER_ADMIN: Get count of all conference documents
     * GET /api/conference-documents/admin/count
     */
    @GetMapping("/admin/count")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get total conference document count (Super Admin)",
        description = "SUPER_ADMIN only: Get total count of all conference documents across all conferences")
    public ResponseEntity<?> getTotalDocumentCount() {
        try {
            long totalCount = conferenceDocumentService.getTotalConferenceDocumentCount();
            logger.info("Total conference documents: {}", totalCount);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalDocumentCount", totalCount
            ));
        } catch (Exception e) {
            logger.error("Error fetching document count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to fetch count: " + e.getMessage()));
        }
    }

    /**
     * SUPER_ADMIN: Get document by ID and update it
     * PUT /api/conference-documents/admin/{documentId}
     */
    @PutMapping("/admin/{documentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update conference document (Super Admin)",
        description = "SUPER_ADMIN only: Update document metadata")
    public ResponseEntity<?> updateConferenceDocument(
            @PathVariable String documentId,
            @RequestBody ConferenceDocumentUpdateRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            HttpServletRequest httpRequest) {
        try {
            UserDetailsImpl ud = getCurrentUser();
            String userId = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            String ipAddress = getClientIpAddress(xForwardedFor, httpRequest);

            logger.info("SUPER_ADMIN updating document: {} by user: {}", documentId, userName);

            ConferenceDocument updated = conferenceDocumentService.updateDocument(documentId, request);

            // Log the update
            conferenceDocumentLogService.log(userId, userName, ipAddress,
                ConferenceDocumentLog.ActionType.UPDATE, updated);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document updated successfully",
                "data", new ConferenceDocumentResponse(updated)
            ));
        } catch (RuntimeException e) {
            logger.error("Error updating document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to update document: " + e.getMessage()));
        }
    }

    /**
     * SUPER_ADMIN: Delete conference document by ID
     * DELETE /api/conference-documents/admin/{documentId}
     */
    @DeleteMapping("/admin/{documentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete conference document (Super Admin)",
        description = "SUPER_ADMIN only: Soft-delete or hard-delete a conference document")
    public ResponseEntity<?> deleteConferenceDocument(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "soft") String deleteType,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            HttpServletRequest httpRequest) {
        try {
            UserDetailsImpl ud = getCurrentUser();
            String userId = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            String ipAddress = getClientIpAddress(xForwardedFor, httpRequest);

            logger.info("SUPER_ADMIN deleting document: {} ({}Delete) by user: {}",
                documentId, deleteType.substring(0, 1).toUpperCase() + deleteType.substring(1), userName);

            ConferenceDocument deleted;
            if ("hard".equalsIgnoreCase(deleteType)) {
                deleted = conferenceDocumentService.hardDeleteDocument(documentId, userId, userName, ipAddress);
            } else {
                deleted = conferenceDocumentService.softDeleteDocument(documentId, userId, userName, ipAddress);
            }

            // Log the deletion
            conferenceDocumentLogService.log(userId, userName, ipAddress,
                ConferenceDocumentLog.ActionType.DELETE, deleted);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document " + deleteType + "-deleted successfully",
                "data", new ConferenceDocumentResponse(deleted)
            ));
        } catch (RuntimeException e) {
            logger.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to delete document: " + e.getMessage()));
        }
    }

    /**
     * SUPER_ADMIN: Search/Filter all conference documents with advanced filters
     * POST /api/conference-documents/admin/search
     */
    @PostMapping("/admin/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search all conference documents (Super Admin)",
        description = "SUPER_ADMIN only: Search all documents with advanced filtering options")
    public ResponseEntity<?> searchAllDocuments(
            @RequestBody ConferenceDocumentFilterRequest filterRequest) {
        try {
            List<ConferenceDocument> docs = conferenceDocumentService.searchDocuments(filterRequest);
            List<ConferenceDocumentResponse> responses = docs.stream()
                .map(ConferenceDocumentResponse::new).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalRecords", responses.size(),
                "filters", Map.of(
                    "conferenceId", filterRequest.getConferenceId() != null ? filterRequest.getConferenceId() : "any",
                    "year", filterRequest.getYear() != null ? filterRequest.getYear() : "any",
                    "documentType", filterRequest.getDocumentType() != null ? filterRequest.getDocumentType() : "any"
                ),
                "data", responses
            ));
        } catch (Exception e) {
            logger.error("Error searching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to search documents: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: Utility Method - Get all documents
    // ─────────────────────────────────────────────────────────────────

    /**
     * Helper method to get all conference documents (across all conferences)
     */
    private List<ConferenceDocument> getAllDocumentsHelper() {
        return conferenceDocumentService.getAllConferenceDocuments();
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
        if (n.endsWith(".xlsm")) return "application/vnd.ms-excel.sheet.macroEnabled.12";  // Macro-enabled Excel
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

    private String getClientIpAddress(String xForwardedFor, HttpServletRequest request) {
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

    /**
     * Check if a string is a valid MongoDB ObjectId (24 hexadecimal characters).
     * MongoDB ObjectIds are exactly 24 hex characters long (0-9, a-f, A-F).
     */
    private boolean isMongoObjectId(String str) {
        if (str == null || str.length() != 24) {
            return false;
        }
        // Check if all characters are valid hexadecimal (0-9, a-f, A-F)
        return str.matches("[0-9a-fA-F]{24}");
    }
}

