package com.sciinov.dbms.service;

import com.sciinov.dbms.dto.ConferenceDocumentUpdateRequest;
import com.sciinov.dbms.dto.ConferenceDocumentFilterRequest;
import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.ConferenceDocument;
import com.sciinov.dbms.entity.ConferenceDocumentLog;
import com.sciinov.dbms.entity.DocumentTypeEntity;
import com.sciinov.dbms.entity.DashboardUploadStats;
import com.sciinov.dbms.repository.AdminActivityLogRepository;
import com.sciinov.dbms.repository.ConferenceRepository;
import com.sciinov.dbms.repository.ConferenceDocumentRepository;
import com.sciinov.dbms.repository.DashboardUploadStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConferenceDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(ConferenceDocumentService.class);

    @Autowired
    private ConferenceDocumentRepository conferenceDocumentRepository;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private GoogleCloudStorageService gcsService;

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private ConferenceDocumentLogService conferenceDocumentLogService;

    @Autowired
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    @Autowired
    private DocumentTypeService documentTypeService;

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Upload or replace conference document.
     * documentTypeSlug = slug from DocumentTypeEntity (e.g., "program", "book")
     */
    public ConferenceDocument uploadDocument(
            String conferenceId,
            Integer year,
            String documentTypeSlug,
            MultipartFile file,
            String userId,
            String userName,
            String ipAddress) throws IOException {

        // Validate conference exists
        Optional<Conference> conferenceOpt = conferenceRepository.findByIdAndDeletedFalse(conferenceId);
        if (conferenceOpt.isEmpty()) {
            throw new RuntimeException("Conference not found: " + conferenceId);
        }
        Conference conference = conferenceOpt.get();

        // Validate document type exists and is active
        DocumentTypeEntity docType = documentTypeService.findBySlug(documentTypeSlug)
                .orElseThrow(() -> new RuntimeException(
                        "Invalid document type: '" + documentTypeSlug + "'. Use GET /api/document-types/active to see available types."));
        if (!docType.isActive()) {
            throw new RuntimeException("Document type '" + docType.getDisplayName() + "' is currently inactive.");
        }

        // Check if document already exists for this conference/year/type — replace if so
        Optional<ConferenceDocument> existingDoc =
            conferenceDocumentRepository.findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
                conferenceId, year, documentTypeSlug);

        if (existingDoc.isPresent()) {
            ConferenceDocument oldDoc = existingDoc.get();
            try {
                String oldGcsKey = (oldDoc.getBlobName() != null && !oldDoc.getBlobName().isEmpty())
                        ? oldDoc.getBlobName() : oldDoc.getFilePath();
                if (oldGcsKey != null && !oldGcsKey.isEmpty()) {
                    gcsService.deleteFile(oldGcsKey);
                    logger.info("Deleted old file from GCS: {}", oldGcsKey);
                }
            } catch (Exception e) {
                logger.warn("Failed to delete old file from GCS: {}", e.getMessage());
            }
            conferenceDocumentRepository.deleteById(oldDoc.getId());
            logger.info("Hard-deleted old document from database - ID: {}", oldDoc.getId());
        }

        // Upload to GCS
        String folderPath = generateFolderPath(conference.getTitle(), year, docType.getFolderName());
        Map<String, String> uploadResult = gcsService.uploadFileAndGetBlobName(file, folderPath);
        String blobName = uploadResult.get("blobName");
        String signedUrl = uploadResult.get("signedUrl");

        // Save document record
        ConferenceDocument newDoc = new ConferenceDocument();
        newDoc.setConferenceId(conferenceId);
        newDoc.setConferenceName(conference.getTitle());
        newDoc.setYear(year);
        newDoc.setDocumentType(documentTypeSlug);                  // slug
        newDoc.setDocumentTypeDisplayName(docType.getDisplayName()); // display name snapshot
        newDoc.setFileName(file.getOriginalFilename());
        newDoc.setBlobName(blobName);
        newDoc.setFilePath(blobName);
        newDoc.setPublicUrl(signedUrl);
        newDoc.setFileSize(file.getSize());
        newDoc.setContentType(file.getContentType());
        newDoc.setUploadedByUserId(userId);
        newDoc.setUploadedByUserName(userName);
        newDoc.setUploadedAt(LocalDateTime.now());
        newDoc.setUpdatedAt(LocalDateTime.now());

        ConferenceDocument saved = conferenceDocumentRepository.save(newDoc);
        logger.info("Uploaded conference document - Conference: {}, Year: {}, Type: {}, File: {}",
            conference.getTitle(), year, documentTypeSlug, file.getOriginalFilename());

        conferenceDocumentLogService.log(userId, userName, ipAddress,
            ConferenceDocumentLog.ActionType.UPLOAD, saved);

        recordUploadStats(userId, conferenceId, null, file.getOriginalFilename());

        return saved;
    }

    /** Get all documents for a conference */
    public List<ConferenceDocument> getConferenceDocuments(String conferenceId) {
        return conferenceDocumentRepository.findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId);
    }

    /** Get documents by conference and year */
    public List<ConferenceDocument> getConferenceDocumentsByYear(String conferenceId, Integer year) {
        return conferenceDocumentRepository.findByConferenceIdAndYearAndDeletedFalseOrderByDocumentTypeAsc(conferenceId, year);
    }

    /** Get all documents for a year (all conferences) */
    public List<ConferenceDocument> getDocumentsByYear(Integer year) {
        return conferenceDocumentRepository.findByYearAndDeletedFalseOrderByConferenceNameAscUpdatedAtDesc(year);
    }

    /** Get documents by type slug */
    public List<ConferenceDocument> getDocumentsByType(String conferenceId, String documentTypeSlug) {
        return conferenceDocumentRepository.findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(
            conferenceId, documentTypeSlug);
    }

    /** Get document by ID */
    public Optional<ConferenceDocument> getDocumentById(String documentId) {
        return conferenceDocumentRepository.findById(documentId).filter(doc -> !doc.isDeleted());
    }

    /**
     * Search with filters — documentType is now a String slug
     */
    public Page<ConferenceDocument> getDocumentsWithFilter(
            String conferenceId,
            String conferenceName,
            Integer year,
            String documentTypeSlug,
            int pageNumber,
            int pageSize) {

        List<ConferenceDocument> docs = new ArrayList<>();

        if (conferenceId != null && !conferenceId.isEmpty()) {
            if (year != null && documentTypeSlug != null) {
                conferenceDocumentRepository
                    .findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(conferenceId, year, documentTypeSlug)
                    .ifPresent(docs::add);
            } else if (year != null) {
                docs.addAll(conferenceDocumentRepository
                    .findByConferenceIdAndYearAndDeletedFalseOrderByDocumentTypeAsc(conferenceId, year));
            } else if (documentTypeSlug != null) {
                docs.addAll(conferenceDocumentRepository
                    .findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(conferenceId, documentTypeSlug));
            } else {
                docs.addAll(conferenceDocumentRepository
                    .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId));
            }
        } else if (conferenceName != null && !conferenceName.isEmpty()) {
            docs.addAll(conferenceDocumentRepository
                .findByConferenceNameAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceName));
        } else if (year != null) {
            docs.addAll(conferenceDocumentRepository
                .findByYearAndDeletedFalseOrderByConferenceNameAscUpdatedAtDesc(year));
        } else if (documentTypeSlug != null) {
            docs.addAll(conferenceDocumentRepository
                .findByDocumentTypeAndDeletedFalseOrderByConferenceNameAscYearDesc(documentTypeSlug));
        } else {
            docs.addAll(conferenceDocumentRepository.findAll().stream()
                .filter(doc -> !doc.isDeleted()).collect(Collectors.toList()));
        }

        docs.sort((d1, d2) -> d2.getUpdatedAt().compareTo(d1.getUpdatedAt()));
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, docs.size());
        return new PageImpl<>(docs.subList(start, end), PageRequest.of(pageNumber, pageSize), docs.size());
    }

    /** Download file bytes */
    public byte[] downloadDocument(String documentId) throws IOException {
        ConferenceDocument doc = getDocumentById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        String gcsKey = (doc.getBlobName() != null && !doc.getBlobName().isEmpty())
                ? doc.getBlobName() : doc.getFilePath();
        if (gcsKey == null || gcsKey.isEmpty()) {
            throw new RuntimeException("Document has no valid GCS path: " + documentId);
        }
        return gcsService.downloadFile(gcsKey);
    }

    /** Download file bytes and log */
    public byte[] downloadDocument(String documentId, String userId, String userName, String ipAddress) throws IOException {
        byte[] content = downloadDocument(documentId);
        getDocumentById(documentId).ifPresent(doc ->
            conferenceDocumentLogService.log(userId, userName, ipAddress, ConferenceDocumentLog.ActionType.DOWNLOAD, doc));
        return content;
    }

    /** Hard-delete from GCS + DB */
    public void deleteDocument(String documentId) {
        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String gcsKey = (doc.getBlobName() != null && !doc.getBlobName().isEmpty())
                ? doc.getBlobName() : doc.getFilePath();
        if (gcsKey != null && !gcsKey.isEmpty()) {
            try {
                gcsService.deleteFile(gcsKey);
                logger.info("Deleted file from GCS: {}", gcsKey);
            } catch (Exception e) {
                logger.warn("Failed to delete from GCS (may already be gone): {}", e.getMessage());
            }
        }
        conferenceDocumentRepository.deleteById(documentId);
        logger.info("Hard-deleted document - ID: {}, File: {}", documentId, doc.getFileName());
    }

    /** Hard-delete and log */
    public void deleteDocument(String documentId, String userId, String userName, String ipAddress) {
        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        deleteDocument(documentId);
        conferenceDocumentLogService.log(userId, userName, ipAddress, ConferenceDocumentLog.ActionType.DELETE, doc);
    }

    /**
     * Update document metadata (year and/or documentType)
     * @param documentId ID of the document to update
     * @param newYear Optional new year for the document
     * @param newDocumentTypeSlug Optional new document type slug
     * @param userId ID of the user performing the update
     * @param userName Name of the user performing the update
     * @param ipAddress IP address of the request
     * @return Updated ConferenceDocument
     */
    public ConferenceDocument updateDocument(
            String documentId,
            Integer newYear,
            String newDocumentTypeSlug,
            String userId,
            String userName,
            String ipAddress) {

        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (doc.isDeleted()) {
            throw new RuntimeException("Cannot update a deleted document: " + documentId);
        }

        String conferenceId = doc.getConferenceId();
        boolean updated = false;

        // Update year if provided
        if (newYear != null) {
            int currentYear = java.time.Year.now().getValue();
            if (newYear < 2000 || newYear > currentYear + 10) {
                throw new RuntimeException("Year must be between 2000 and " + (currentYear + 10));
            }
            if (!newYear.equals(doc.getYear())) {
                doc.setYear(newYear);
                updated = true;
            }
        }

        // Update document type if provided
        if (newDocumentTypeSlug != null && !newDocumentTypeSlug.isEmpty()) {
            String typeSlug = newDocumentTypeSlug.trim();

            // Resolve if it's a MongoDB ObjectId
            if (isMongoObjectId(typeSlug)) {
                Optional<DocumentTypeEntity> docTypeOpt = documentTypeService.findById(typeSlug);
                if (docTypeOpt.isPresent()) {
                    typeSlug = docTypeOpt.get().getSlug();
                    logger.info("Resolved document type ID {} to slug: {}", newDocumentTypeSlug, typeSlug);
                } else {
                    throw new RuntimeException(
                        "Invalid document type ID: '" + newDocumentTypeSlug + "'. Use GET /api/document-types/active to see available types.");
                }
            } else {
                typeSlug = typeSlug.toLowerCase().replace(' ', '_').replace('-', '_');
            }

            // Validate that the new document type exists and is active
            final String finalTypeSlug = typeSlug;  // Make it effectively final for lambda
            DocumentTypeEntity newDocType = documentTypeService.findBySlug(finalTypeSlug)
                    .orElseThrow(() -> new RuntimeException(
                            "Invalid document type: '" + finalTypeSlug + "'. Use GET /api/document-types/active to see available types."));
            if (!newDocType.isActive()) {
                throw new RuntimeException("Document type '" + newDocType.getDisplayName() + "' is currently inactive.");
            }

            if (!finalTypeSlug.equals(doc.getDocumentType())) {
                doc.setDocumentType(finalTypeSlug);
                doc.setDocumentTypeDisplayName(newDocType.getDisplayName());
                updated = true;
            }
        }

        if (!updated) {
            throw new RuntimeException("No changes provided for update");
        }

        doc.setUpdatedAt(LocalDateTime.now());
        ConferenceDocument saved = conferenceDocumentRepository.save(doc);

        logger.info("Updated conference document - ID: {}, Conference: {}, Year: {}, Type: {}, Updated by: {}",
            documentId, conferenceId, doc.getYear(), doc.getDocumentType(), userName);

        // Log the update action
        conferenceDocumentLogService.log(userId, userName, ipAddress, ConferenceDocumentLog.ActionType.UPDATE, saved);

        return saved;
    }

    /** Get available years for a conference */
    public List<Integer> getAvailableYears(String conferenceId) {
        return conferenceDocumentRepository
            .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId)
            .stream()
            .map(ConferenceDocument::getYear)
            .distinct()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
    }

    /** Statistics for a single conference */
    public Map<String, Object> getDocumentStatistics(String conferenceId) {
        List<ConferenceDocument> docs = conferenceDocumentRepository
            .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", docs.size());
        stats.put("years", getAvailableYears(conferenceId));

        Map<Integer, Long> byYear = docs.stream()
            .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting()));
        stats.put("documentsByYear", byYear);

        Map<String, Long> byType = docs.stream()
            .collect(Collectors.groupingBy(
                d -> d.getDocumentTypeDisplayName() != null ? d.getDocumentTypeDisplayName() : d.getDocumentType(),
                Collectors.counting()));
        stats.put("documentsByType", byType);

        long totalSize = docs.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum();
        stats.put("totalFileSizeBytes", totalSize);

        docs.stream().map(ConferenceDocument::getUploadedAt).filter(Objects::nonNull)
            .max(Comparator.naturalOrder()).ifPresent(t -> stats.put("lastUploadedAt", t));

        return stats;
    }

    /** Global statistics across all conferences (SUPER_ADMIN) */
    public Map<String, Object> getGlobalDocumentStatistics() {
        List<ConferenceDocument> all = conferenceDocumentRepository.findAll()
            .stream().filter(d -> !d.isDeleted()).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", all.size());
        stats.put("totalConferences", all.stream().map(ConferenceDocument::getConferenceId).distinct().count());

        List<Integer> years = all.stream().map(ConferenceDocument::getYear).distinct()
            .sorted(Collections.reverseOrder()).collect(Collectors.toList());
        stats.put("years", years);

        long totalSize = all.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum();
        stats.put("totalFileSizeBytes", totalSize);

        Map<String, Long> byConference = all.stream().collect(Collectors.groupingBy(
            d -> d.getConferenceName() != null ? d.getConferenceName() : d.getConferenceId(), Collectors.counting()));
        stats.put("documentsByConference", byConference);

        Map<Integer, Long> byYear = all.stream()
            .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting()));
        stats.put("documentsByYear", byYear);

        Map<String, Long> byType = all.stream().collect(Collectors.groupingBy(
            d -> d.getDocumentTypeDisplayName() != null ? d.getDocumentTypeDisplayName() : d.getDocumentType(),
            Collectors.counting()));
        stats.put("documentsByType", byType);

        all.stream().map(ConferenceDocument::getUploadedAt).filter(Objects::nonNull)
            .max(Comparator.naturalOrder()).ifPresent(t -> stats.put("lastUploadedAt", t));

        return stats;
    }

    /**
     * Admin dashboard data — scoped to assigned conferences
     */
    public Map<String, Object> getAdminConferenceDocumentsData(List<String> conferenceIds) {
        Map<String, Object> result = new HashMap<>();

        if (conferenceIds == null || conferenceIds.isEmpty()) {
            result.put("totalConferences", 0);
            result.put("totalDocuments", 0);
            result.put("conferencesData", new ArrayList<>());
            return result;
        }

        List<ConferenceDocument> allDocs = conferenceDocumentRepository
            .findByConferenceIdInAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceIds);

        List<Map<String, Object>> conferencesData = new ArrayList<>();
        for (String confId : conferenceIds) {
            List<ConferenceDocument> docs = allDocs.stream()
                .filter(d -> confId.equals(d.getConferenceId()))
                .collect(Collectors.toList());
            if (docs.isEmpty()) continue;

            Map<String, Object> confData = new HashMap<>();
            conferenceRepository.findByIdAndDeletedFalse(confId).ifPresent(c -> {
                confData.put("conferenceId", c.getId());
                confData.put("conferenceName", c.getTitle());
                confData.put("conferenceStatus", c.getStatus());
            });

            confData.put("totalDocuments", docs.size());

            confData.put("documentsByYear", docs.stream()
                .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting())));

            confData.put("documentsByType", docs.stream().collect(Collectors.groupingBy(
                d -> d.getDocumentTypeDisplayName() != null ? d.getDocumentTypeDisplayName() : d.getDocumentType(),
                Collectors.counting())));

            long totalSize = docs.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum();
            confData.put("totalFileSizeBytes", totalSize);

            List<Map<String, Object>> recentUploads = docs.stream()
                .sorted(Comparator.comparing(d -> d.getUploadedAt() != null ? d.getUploadedAt() : LocalDateTime.MIN,
                    Comparator.reverseOrder()))
                .limit(5)
                .map(d -> {
                    Map<String, Object> e = new HashMap<>();
                    e.put("id", d.getId());
                    e.put("fileName", d.getFileName());
                    e.put("documentType", d.getDocumentTypeDisplayName() != null ? d.getDocumentTypeDisplayName() : d.getDocumentType());
                    e.put("documentTypeSlug", d.getDocumentType());
                    e.put("year", d.getYear());
                    e.put("fileSize", d.getFileSize() != null ? d.getFileSize() : 0L);
                    e.put("uploadedAt", d.getUploadedAt());
                    return e;
                })
                .collect(Collectors.toList());
            confData.put("recentUploads", recentUploads);

            confData.put("availableYears", docs.stream().map(ConferenceDocument::getYear).distinct()
                .sorted(Collections.reverseOrder()).collect(Collectors.toList()));

            conferencesData.add(confData);
        }

        Map<String, Object> globalStats = new HashMap<>();
        globalStats.put("totalConferences", conferencesData.size());
        globalStats.put("totalDocuments", allDocs.size());
        globalStats.put("totalFileSizeBytes",
            allDocs.stream().mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L).sum());
        globalStats.put("years", allDocs.stream().map(ConferenceDocument::getYear).distinct()
            .sorted(Collections.reverseOrder()).collect(Collectors.toList()));
        globalStats.put("documentsByType", allDocs.stream().collect(Collectors.groupingBy(
            d -> d.getDocumentTypeDisplayName() != null ? d.getDocumentTypeDisplayName() : d.getDocumentType(),
            Collectors.counting())));
        allDocs.stream().map(ConferenceDocument::getUploadedAt).filter(Objects::nonNull)
            .max(LocalDateTime::compareTo).ifPresent(t -> globalStats.put("lastUploadedAt", t));

        result.put("globalStats", globalStats);
        result.put("conferencesData", conferencesData);
        return result;
    }

    // ─── Private helpers ─────────────────────────────────────────────

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


    private String generateFolderPath(String conferenceName, Integer year, String folderName) {
        return String.format("conferences/%s/%d/%s/", sanitizePath(conferenceName), year, folderName);
    }

    private String sanitizePath(String path) {
        if (path == null) return "unknown";
        return path.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-").trim();
    }

    private void logActivity(String adminId, String adminName, String conferenceId,
                              String dashboardMasterId, AdminActivityLog.ActionType actionType,
                              String description, String ipAddress) {
        try {
            AdminActivityLog log = new AdminActivityLog();
            log.setAdminId(adminId); log.setAdminName(adminName);
            log.setConferenceId(conferenceId); log.setDashboardMasterId(dashboardMasterId);
            log.setActionType(actionType); log.setDescription(description);
            log.setIpAddress(ipAddress); log.setCreatedAt(LocalDateTime.now());
            analyticsService.saveAndPushLog(log);
        } catch (Exception e) {
            logger.error("Failed to log activity: {}", e.getMessage());
        }
    }

    private void recordUploadStats(String adminId, String conferenceId, String dashboardMasterId, String fileName) {
        try {
            DashboardUploadStats stats = new DashboardUploadStats();
            stats.setAdminId(adminId); stats.setConferenceId(conferenceId);
            stats.setDashboardMasterId(dashboardMasterId); stats.setFileName(fileName);
            stats.setUploadedAt(LocalDateTime.now());
            stats.setTotalRecordsInFile(1); stats.setNewRecordsAdded(1); stats.setDuplicateRecordsIgnored(0);
            dashboardUploadStatsRepository.save(stats);
        } catch (Exception e) {
            logger.error("Failed to record upload stats: {}", e.getMessage());
        }
    }

    /**
     * Get all conference documents across all conferences
     */
    public List<ConferenceDocument> getAllConferenceDocuments() {
        return conferenceDocumentRepository.findByDeletedFalseOrderByUpdatedAtDesc();
    }

    /**
     * Get total count of all conference documents
     */
    public long getTotalConferenceDocumentCount() {
        List<ConferenceDocument> all = conferenceDocumentRepository.findByDeletedFalse();
        return all.size();
    }

    /**
     * Update conference document metadata
     */
    public ConferenceDocument updateDocument(String documentId, ConferenceDocumentUpdateRequest request) {
        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        if (request.getYear() != null) {
            doc.setYear(request.getYear());
        }
        if (request.getFileName() != null && !request.getFileName().isEmpty()) {
            doc.setFileName(request.getFileName());
        }

        doc.setUpdatedAt(LocalDateTime.now());
        return conferenceDocumentRepository.save(doc);
    }

    /**
     * Soft-delete document (mark as deleted, don't remove from DB)
     */
    public ConferenceDocument softDeleteDocument(String documentId, String userId, String userName, String ipAddress) {
        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        doc.setDeleted(true);
        doc.setUpdatedAt(LocalDateTime.now());
        ConferenceDocument deleted = conferenceDocumentRepository.save(doc);

        logActivity(userId, userName, doc.getConferenceId(), null,
                AdminActivityLog.ActionType.DELETE, "Soft-deleted document: " + doc.getFileName(), ipAddress);

        return deleted;
    }

    /**
     * Hard-delete document (remove from DB and GCS)
     */
    public ConferenceDocument hardDeleteDocument(String documentId, String userId, String userName, String ipAddress) {
        ConferenceDocument doc = conferenceDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Delete from GCS
        try {
            String blobName = (doc.getBlobName() != null && !doc.getBlobName().isEmpty())
                    ? doc.getBlobName() : doc.getFilePath();
            if (blobName != null && !blobName.isEmpty()) {
                gcsService.deleteFile(blobName);
                logger.info("Deleted GCS file: {}", blobName);
            }
        } catch (Exception e) {
            logger.warn("Failed to delete GCS file: {}", e.getMessage());
        }

        // Delete from DB
        conferenceDocumentRepository.deleteById(documentId);

        logActivity(userId, userName, doc.getConferenceId(), null,
                AdminActivityLog.ActionType.DELETE, "Hard-deleted document: " + doc.getFileName(), ipAddress);

        return doc;
    }

    /**
     * Search documents with filter request
     */
    public List<ConferenceDocument> searchDocuments(ConferenceDocumentFilterRequest filterRequest) {
        List<ConferenceDocument> docs = conferenceDocumentRepository.findByDeletedFalse();

        if (filterRequest.getConferenceId() != null && !filterRequest.getConferenceId().isEmpty()) {
            docs = docs.stream()
                    .filter(d -> d.getConferenceId().equals(filterRequest.getConferenceId()))
                    .collect(Collectors.toList());
        }

        if (filterRequest.getConferenceName() != null && !filterRequest.getConferenceName().isEmpty()) {
            String searchName = filterRequest.getConferenceName().toLowerCase();
            docs = docs.stream()
                    .filter(d -> d.getConferenceName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
        }

        if (filterRequest.getYear() != null) {
            docs = docs.stream()
                    .filter(d -> d.getYear().equals(filterRequest.getYear()))
                    .collect(Collectors.toList());
        }

        if (filterRequest.getDocumentType() != null && !filterRequest.getDocumentType().isEmpty()) {
            String typeSlug = filterRequest.getDocumentType().trim().toLowerCase().replace(' ', '_').replace('-', '_');
            docs = docs.stream()
                    .filter(d -> d.getDocumentType().equalsIgnoreCase(typeSlug))
                    .collect(Collectors.toList());
        }

        return docs;
    }
}
