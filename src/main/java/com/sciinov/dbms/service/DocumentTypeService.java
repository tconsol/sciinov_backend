package com.sciinov.dbms.service;

import com.sciinov.dbms.dto.DocumentTypeRequest;
import com.sciinov.dbms.entity.DocumentTypeEntity;
import com.sciinov.dbms.repository.DocumentTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentTypeService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTypeService.class);

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    /**
     * Seed default document types if collection is empty.
     * Called at startup from MongoConfig.
     */
    public void seedDefaultTypes() {
        if (documentTypeRepository.findByDeletedFalseOrderBySortOrderAscDisplayNameAsc().isEmpty()) {
            String[][] defaults = {
                {"Program",        "Conference program / schedule document",          "1"},
                {"Book",           "Conference book / proceedings document",           "2"},
                {"Positive Sheets","Positive sheets / attendance confirmation sheets", "3"}
            };
            for (String[] d : defaults) {
                DocumentTypeEntity e = new DocumentTypeEntity();
                String slug = DocumentTypeEntity.toSlug(d[0]);
                e.setSlug(slug);
                e.setDisplayName(d[0]);
                e.setDescription(d[1]);
                e.setFolderName(DocumentTypeEntity.toFolderName(slug));
                e.setSortOrder(Integer.parseInt(d[2]));
                e.setActive(true);
                e.setCreatedAt(LocalDateTime.now());
                e.setUpdatedAt(LocalDateTime.now());
                documentTypeRepository.save(e);
                logger.info("Seeded default document type: {}", d[0]);
            }
        }
    }

    /**
     * Create a new document type.
     */
    public DocumentTypeEntity createDocumentType(DocumentTypeRequest request, String userId, String userName) {
        String slug = DocumentTypeEntity.toSlug(request.getDisplayName());

        if (documentTypeRepository.existsBySlugAndDeletedFalse(slug)) {
            throw new RuntimeException("A document type with name '" + request.getDisplayName() + "' already exists.");
        }
        if (documentTypeRepository.existsByDisplayNameIgnoreCaseAndDeletedFalse(request.getDisplayName())) {
            throw new RuntimeException("A document type with display name '" + request.getDisplayName() + "' already exists.");
        }

        // Determine sort order
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder()
                : documentTypeRepository.findByDeletedFalseOrderBySortOrderAscDisplayNameAsc().size() + 1;

        DocumentTypeEntity entity = new DocumentTypeEntity();
        entity.setSlug(slug);
        entity.setDisplayName(request.getDisplayName().trim());
        entity.setDescription(request.getDescription());
        entity.setFolderName(DocumentTypeEntity.toFolderName(slug));
        entity.setSortOrder(sortOrder);
        entity.setActive(request.getActive() != null ? request.getActive() : true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedByUserId(userId);
        entity.setCreatedByUserName(userName);

        DocumentTypeEntity saved = documentTypeRepository.save(entity);
        logger.info("Created document type: {} (slug={}), by user: {}", saved.getDisplayName(), saved.getSlug(), userName);
        return saved;
    }

    /**
     * Update an existing document type.
     * Slug is not updatable (it would break existing document references).
     */
    public DocumentTypeEntity updateDocumentType(String id, DocumentTypeRequest request, String userId, String userName) {
        DocumentTypeEntity entity = documentTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new RuntimeException("Document type not found: " + id));

        // Check if new displayName conflicts with another type
        if (!entity.getDisplayName().equalsIgnoreCase(request.getDisplayName())) {
            if (documentTypeRepository.existsByDisplayNameIgnoreCaseAndDeletedFalse(request.getDisplayName())) {
                throw new RuntimeException("A document type with display name '" + request.getDisplayName() + "' already exists.");
            }
        }

        entity.setDisplayName(request.getDisplayName().trim());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) entity.setActive(request.getActive());
        entity.setUpdatedAt(LocalDateTime.now());

        DocumentTypeEntity saved = documentTypeRepository.save(entity);
        logger.info("Updated document type: {} (id={}), by user: {}", saved.getDisplayName(), id, userName);
        return saved;
    }

    /**
     * Soft-delete a document type.
     */
    public void deleteDocumentType(String id, String userId, String userName) {
        DocumentTypeEntity entity = documentTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new RuntimeException("Document type not found: " + id));

        entity.setDeleted(true);
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        documentTypeRepository.save(entity);
        logger.info("Deleted document type: {} (id={}), by user: {}", entity.getDisplayName(), id, userName);
    }

    /**
     * Toggle active/inactive.
     */
    public DocumentTypeEntity toggleActive(String id, String userName) {
        DocumentTypeEntity entity = documentTypeRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new RuntimeException("Document type not found: " + id));

        entity.setActive(!entity.isActive());
        entity.setUpdatedAt(LocalDateTime.now());
        DocumentTypeEntity saved = documentTypeRepository.save(entity);
        logger.info("Toggled document type '{}' active={}, by user: {}", saved.getDisplayName(), saved.isActive(), userName);
        return saved;
    }

    /**
     * Get all document types (including inactive, excluding deleted). SUPER_ADMIN view.
     */
    public List<DocumentTypeEntity> getAllDocumentTypes() {
        return documentTypeRepository.findByDeletedFalseOrderBySortOrderAscDisplayNameAsc();
    }

    /**
     * Get only active document types. Used by admin when uploading files.
     */
    public List<DocumentTypeEntity> getActiveDocumentTypes() {
        return documentTypeRepository.findByActiveAndDeletedFalseOrderBySortOrderAscDisplayNameAsc(true);
    }

    /**
     * Get by slug — used internally when validating upload requests.
     */
    public Optional<DocumentTypeEntity> findBySlug(String slug) {
        return documentTypeRepository.findBySlugAndDeletedFalse(slug);
    }

    /**
     * Get by ID.
     */
    public Optional<DocumentTypeEntity> findById(String id) {
        return documentTypeRepository.findById(id).filter(e -> !e.isDeleted());
    }
}

