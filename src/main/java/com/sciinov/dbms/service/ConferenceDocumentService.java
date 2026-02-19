package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.ConferenceDocument;
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
    private DashboardUploadStatsRepository dashboardUploadStatsRepository;

    /**
     * Upload or replace conference document
     * If document already exists for this conference/year/type, replace it
     * Also logs activity and updates dashboard upload stats
     */
    public ConferenceDocument uploadDocument(
            String conferenceId,
            Integer year,
            ConferenceDocument.DocumentType documentType,
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

        // Check if document already exists
        Optional<ConferenceDocument> existingDoc =
            conferenceDocumentRepository.findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
                conferenceId, year, documentType);

        // If exists, delete old file from GCS
        if (existingDoc.isPresent()) {
            ConferenceDocument oldDoc = existingDoc.get();
            try {
                if (oldDoc.getFilePath() != null) {
                    gcsService.deleteFile(oldDoc.getFilePath());
                    logger.info("Deleted old file from GCS: {}", oldDoc.getFilePath());
                }
            } catch (Exception e) {
                logger.warn("Failed to delete old file from GCS: {}", e.getMessage());
            }
            // Delete from database
            oldDoc.setDeleted(true);
            conferenceDocumentRepository.save(oldDoc);
        }

        // Upload new file to GCS
        String folderPath = generateFolderPath(conference.getTitle(), year, documentType);
        String gcsPath = gcsService.uploadFile(file, folderPath);

        // Create new document entry
        ConferenceDocument newDoc = new ConferenceDocument();
        newDoc.setConferenceId(conferenceId);
        newDoc.setConferenceName(conference.getTitle());
        newDoc.setYear(year);
        newDoc.setDocumentType(documentType);
        newDoc.setFileName(file.getOriginalFilename());
        newDoc.setFilePath(folderPath + getFileName(file.getOriginalFilename()));
        newDoc.setPublicUrl(gcsPath);
        newDoc.setFileSize(file.getSize());
        newDoc.setContentType(file.getContentType());
        newDoc.setUploadedByUserId(userId);
        newDoc.setUploadedByUserName(userName);
        newDoc.setUploadedAt(LocalDateTime.now());
        newDoc.setUpdatedAt(LocalDateTime.now());

        ConferenceDocument saved = conferenceDocumentRepository.save(newDoc);
        logger.info("Uploaded conference document - Conference: {}, Year: {}, Type: {}, File: {}",
            conference.getTitle(), year, documentType, file.getOriginalFilename());

        // Log activity
        logActivity(userId, userName, conferenceId, null,
            AdminActivityLog.ActionType.UPLOAD_EXCEL,
            "Uploaded " + documentType.getDisplayName() + " document for year " + year,
            ipAddress);

        // Record upload stats
        recordUploadStats(userId, conferenceId, null, file.getOriginalFilename());

        return saved;
    }

    /**
     * Get all documents for a specific conference
     */
    public List<ConferenceDocument> getConferenceDocuments(String conferenceId) {
        return conferenceDocumentRepository.findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId);
    }

    /**
     * Get all documents for a specific conference and year
     */
    public List<ConferenceDocument> getConferenceDocumentsByYear(String conferenceId, Integer year) {
        return conferenceDocumentRepository.findByConferenceIdAndYearAndDeletedFalseOrderByDocumentTypeAsc(
            conferenceId, year);
    }

    /**
     * Get all documents for a specific year (across all conferences)
     */
    public List<ConferenceDocument> getDocumentsByYear(Integer year) {
        return conferenceDocumentRepository.findByYearAndDeletedFalseOrderByConferenceNameAscUpdatedAtDesc(year);
    }

    /**
     * Get documents of a specific type for a conference
     */
    public List<ConferenceDocument> getDocumentsByType(String conferenceId, ConferenceDocument.DocumentType documentType) {
        return conferenceDocumentRepository.findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(
            conferenceId, documentType);
    }

    /**
     * Get a specific document by ID
     */
    public Optional<ConferenceDocument> getDocumentById(String documentId) {
        return conferenceDocumentRepository.findById(documentId)
            .filter(doc -> !doc.isDeleted());
    }

    /**
     * Get documents with advanced filtering
     */
    public Page<ConferenceDocument> getDocumentsWithFilter(
            String conferenceId,
            String conferenceName,
            Integer year,
            ConferenceDocument.DocumentType documentType,
            int pageNumber,
            int pageSize) {

        // Get all matching documents
        List<ConferenceDocument> docs = new ArrayList<>();

        if (conferenceId != null && !conferenceId.isEmpty()) {
            if (year != null && documentType != null) {
                // Filter by conference, year, and type
                Optional<ConferenceDocument> doc =
                    conferenceDocumentRepository.findByConferenceIdAndYearAndDocumentTypeAndDeletedFalse(
                        conferenceId, year, documentType);
                doc.ifPresent(docs::add);
            } else if (year != null) {
                // Filter by conference and year
                docs.addAll(conferenceDocumentRepository.findByConferenceIdAndYearAndDeletedFalseOrderByDocumentTypeAsc(
                    conferenceId, year));
            } else if (documentType != null) {
                // Filter by conference and type
                docs.addAll(conferenceDocumentRepository.findByConferenceIdAndDocumentTypeAndDeletedFalseOrderByYearDesc(
                    conferenceId, documentType));
            } else {
                // All documents for conference
                docs.addAll(conferenceDocumentRepository.findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(
                    conferenceId));
            }
        } else if (conferenceName != null && !conferenceName.isEmpty()) {
            // Filter by conference name
            docs.addAll(conferenceDocumentRepository.findByConferenceNameAndDeletedFalseOrderByYearDescUpdatedAtDesc(
                conferenceName));
        } else if (year != null) {
            // Filter by year only
            docs.addAll(conferenceDocumentRepository.findByYearAndDeletedFalseOrderByConferenceNameAscUpdatedAtDesc(year));
        } else if (documentType != null) {
            // Filter by type only
            docs.addAll(conferenceDocumentRepository.findByDocumentTypeAndDeletedFalseOrderByConferenceNameAscYearDesc(
                documentType));
        } else {
            // All documents
            docs.addAll(conferenceDocumentRepository.findAll().stream()
                .filter(doc -> !doc.isDeleted())
                .collect(Collectors.toList()));
        }

        // Sort
        docs.sort((d1, d2) -> d2.getUpdatedAt().compareTo(d1.getUpdatedAt()));

        // Pagination
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, docs.size());
        List<ConferenceDocument> pageContent = docs.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(pageNumber, pageSize), docs.size());
    }

    /**
     * Download document file
     */
    public byte[] downloadDocument(String documentId) throws IOException {
        Optional<ConferenceDocument> docOpt = getDocumentById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        ConferenceDocument doc = docOpt.get();
        byte[] fileContent = gcsService.downloadFile(doc.getFilePath());
        logger.info("Downloaded document - ID: {}, Name: {}", documentId, doc.getFileName());
        return fileContent;
    }

    /**
     * Delete document
     */
    public void deleteDocument(String documentId) {
        Optional<ConferenceDocument> docOpt = conferenceDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        ConferenceDocument doc = docOpt.get();

        // Delete from GCS
        try {
            gcsService.deleteFile(doc.getFilePath());
            logger.info("Deleted file from GCS: {}", doc.getFilePath());
        } catch (Exception e) {
            logger.warn("Failed to delete file from GCS: {}", e.getMessage());
        }

        // Mark as deleted in database
        doc.setDeleted(true);
        conferenceDocumentRepository.save(doc);
        logger.info("Marked document as deleted - ID: {}", documentId);
    }

    /**
     * Get available years for a conference
     */
    public List<Integer> getAvailableYears(String conferenceId) {
        List<ConferenceDocument> docs = conferenceDocumentRepository
            .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId);

        return docs.stream()
            .map(ConferenceDocument::getYear)
            .distinct()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
    }

    /**
     * Get document statistics
     */
    public Map<String, Object> getDocumentStatistics(String conferenceId) {
        Map<String, Object> stats = new HashMap<>();

        List<ConferenceDocument> docs = conferenceDocumentRepository
            .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(conferenceId);

        stats.put("totalDocuments", docs.size());
        stats.put("years", getAvailableYears(conferenceId));
        stats.put("documentTypes", Arrays.asList(
            ConferenceDocument.DocumentType.PROGRAM,
            ConferenceDocument.DocumentType.BOOK,
            ConferenceDocument.DocumentType.POSITIVE_SHEETS
        ));

        // Group by year
        Map<Integer, Long> docsByYear = docs.stream()
            .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting()));
        stats.put("documentsByYear", docsByYear);

        // Group by type
        Map<String, Long> docsByType = docs.stream()
            .collect(Collectors.groupingBy(
                doc -> doc.getDocumentType().getDisplayName(),
                Collectors.counting()
            ));
        stats.put("documentsByType", docsByType);

        return stats;
    }

    /**
     * Generate GCS folder path for conference documents
     * Format: conferences/{conferenceName}/{year}/{documentType}/
     */
    private String generateFolderPath(String conferenceName, Integer year, ConferenceDocument.DocumentType documentType) {
        String sanitizedConferenceName = sanitizePath(conferenceName);
        String documentTypeFolder = documentType.getFolderName();
        return String.format("conferences/%s/%d/%s/", sanitizedConferenceName, year, documentTypeFolder);
    }

    /**
     * Generate file name from original file name
     */
    private String getFileName(String originalFileName) {
        if (originalFileName == null) return "document";
        return originalFileName.toLowerCase().replaceAll("[^a-z0-9.]", "-");
    }

    /**
     * Sanitize path (remove special characters)
     */
    private String sanitizePath(String path) {
        if (path == null) return "unknown";
        return path.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    /**
     * Log admin activity
     */
    private void logActivity(String adminId, String adminName, String conferenceId,
                            String dashboardMasterId, AdminActivityLog.ActionType actionType,
                            String description, String ipAddress) {
        try {
            AdminActivityLog log = new AdminActivityLog();
            log.setAdminId(adminId);
            log.setAdminName(adminName);
            log.setConferenceId(conferenceId);
            log.setDashboardMasterId(dashboardMasterId);
            log.setActionType(actionType);
            log.setDescription(description);
            log.setIpAddress(ipAddress);
            log.setCreatedAt(LocalDateTime.now());

            adminActivityLogRepository.save(log);
            logger.info("Activity logged - Admin: {}, Action: {}, Conference: {}, IP: {}",
                adminName, actionType, conferenceId, ipAddress);
        } catch (Exception e) {
            logger.error("Failed to log activity: {}", e.getMessage());
        }
    }

    /**
     * Record upload statistics
     */
    private void recordUploadStats(String adminId, String conferenceId,
                                   String dashboardMasterId, String fileName) {
        try {
            DashboardUploadStats stats = new DashboardUploadStats();
            stats.setAdminId(adminId);
            stats.setConferenceId(conferenceId);
            stats.setDashboardMasterId(dashboardMasterId);
            stats.setFileName(fileName);
            stats.setUploadedAt(LocalDateTime.now());
            stats.setTotalRecordsInFile(1); // Document count
            stats.setNewRecordsAdded(1);
            stats.setDuplicateRecordsIgnored(0);

            dashboardUploadStatsRepository.save(stats);
            logger.info("Upload stats recorded - Admin: {}, Conference: {}, File: {}",
                adminId, conferenceId, fileName);
        } catch (Exception e) {
            logger.error("Failed to record upload stats: {}", e.getMessage());
        }
    }
}

