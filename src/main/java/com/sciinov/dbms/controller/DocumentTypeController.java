package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.DocumentTypeRequest;
import com.sciinov.dbms.dto.DocumentTypeResponse;
import com.sciinov.dbms.entity.DocumentTypeEntity;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/document-types")
@Tag(name = "Document Types", description = "SUPER_ADMIN: manage dynamic conference document types")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class DocumentTypeController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeController.class);

    @Autowired
    private DocumentTypeService documentTypeService;

    private UserDetailsImpl currentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC: GET active types (used by admin during file upload)
    // GET /api/document-types/active
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get active document types", description = "Returns all active document types available for file upload")
    public ResponseEntity<?> getActiveTypes() {
        List<DocumentTypeResponse> types = documentTypeService.getActiveDocumentTypes()
                .stream().map(DocumentTypeResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "total", types.size(),
            "data", types
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: GET all types (active + inactive)
    // GET /api/document-types
    // ─────────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all document types (SUPER_ADMIN)", description = "Returns all document types including inactive ones")
    public ResponseEntity<?> getAllTypes() {
        List<DocumentTypeResponse> types = documentTypeService.getAllDocumentTypes()
                .stream().map(DocumentTypeResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "total", types.size(),
            "data", types
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: GET by ID
    // GET /api/document-types/{id}
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get document type by ID")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return documentTypeService.findById(id)
                .map(e -> ResponseEntity.ok(Map.of("success", true, "data", new DocumentTypeResponse(e))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Document type not found")));
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: CREATE
    // POST /api/document-types
    // ─────────────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create a new document type")
    public ResponseEntity<?> createDocumentType(@Valid @RequestBody DocumentTypeRequest request) {
        try {
            UserDetailsImpl ud = currentUser();
            String userId   = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();

            DocumentTypeEntity created = documentTypeService.createDocumentType(request, userId, userName);
            logger.info("POST /api/document-types - Created type: {}", created.getDisplayName());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Document type created successfully",
                "data", new DocumentTypeResponse(created)
            ));
        } catch (RuntimeException e) {
            logger.warn("POST /api/document-types - Error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("POST /api/document-types - Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Unexpected error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: UPDATE
    // PUT /api/document-types/{id}
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update a document type", description = "Note: slug is not updatable to preserve existing document references")
    public ResponseEntity<?> updateDocumentType(@PathVariable String id,
                                                @Valid @RequestBody DocumentTypeRequest request) {
        try {
            UserDetailsImpl ud = currentUser();
            String userId   = ud.getId();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();

            DocumentTypeEntity updated = documentTypeService.updateDocumentType(id, request, userId, userName);
            logger.info("PUT /api/document-types/{} - Updated type: {}", id, updated.getDisplayName());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document type updated successfully",
                "data", new DocumentTypeResponse(updated)
            ));
        } catch (RuntimeException e) {
            logger.warn("PUT /api/document-types/{} - Error: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("PUT /api/document-types/{} - Unexpected error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Unexpected error: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: TOGGLE ACTIVE
    // PATCH /api/document-types/{id}/toggle-active
    // ─────────────────────────────────────────────────────────────────
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Toggle active/inactive status")
    public ResponseEntity<?> toggleActive(@PathVariable String id) {
        try {
            UserDetailsImpl ud = currentUser();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            DocumentTypeEntity updated = documentTypeService.toggleActive(id, userName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document type " + (updated.isActive() ? "activated" : "deactivated") + " successfully",
                "data", new DocumentTypeResponse(updated)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // SUPER_ADMIN: DELETE (soft-delete)
    // DELETE /api/document-types/{id}
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete (soft-delete) a document type",
        description = "Soft-deletes the type. Existing documents using this type are unaffected.")
    public ResponseEntity<?> deleteDocumentType(@PathVariable String id) {
        try {
            UserDetailsImpl ud = currentUser();
            String userName = ud.getUser().getFirstName() + " " + ud.getUser().getLastName();
            documentTypeService.deleteDocumentType(id, ud.getId(), userName);
            logger.info("DELETE /api/document-types/{} - Deleted by user: {}", id, userName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document type deleted successfully. Existing documents are unaffected."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("DELETE /api/document-types/{} - Error: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Unexpected error: " + e.getMessage()));
        }
    }
}

