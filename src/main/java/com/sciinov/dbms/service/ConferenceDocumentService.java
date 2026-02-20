package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.AdminActivityLog;
import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.ConferenceDocument;
import com.sciinov.dbms.entity.ConferenceDocumentLog;
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

        // If exists, delete old file from GCS and hard-delete from DB
        if (existingDoc.isPresent()) {
            ConferenceDocument oldDoc = existingDoc.get();
            try {
                // Use blobName first; fall back to filePath for legacy records
                String oldGcsKey = (oldDoc.getBlobName() != null && !oldDoc.getBlobName().isEmpty())
                        ? oldDoc.getBlobName() : oldDoc.getFilePath();
                if (oldGcsKey != null && !oldGcsKey.isEmpty()) {
                    gcsService.deleteFile(oldGcsKey);
                    logger.info("Deleted old file from GCS: {}", oldGcsKey);
                }
            } catch (Exception e) {
                logger.warn("Failed to delete old file from GCS: {}", e.getMessage());
            }
            // Hard-delete old record from database
            conferenceDocumentRepository.deleteById(oldDoc.getId());
            logger.info("Hard-deleted old document from database - ID: {}", oldDoc.getId());
        }

        // Upload new file to GCS - get back the actual blobName and signed URL
        String folderPath = generateFolderPath(conference.getTitle(), year, documentType);
        Map<String, String> uploadResult = gcsService.uploadFileAndGetBlobName(file, folderPath);
        String blobName = uploadResult.get("blobName");
        String signedUrl = uploadResult.get("signedUrl");

        // Create new document entry
        ConferenceDocument newDoc = new ConferenceDocument();
        newDoc.setConferenceId(conferenceId);
        newDoc.setConferenceName(conference.getTitle());
        newDoc.setYear(year);
        newDoc.setDocumentType(documentType);
        newDoc.setFileName(file.getOriginalFilename());
        newDoc.setBlobName(blobName);           // Store actual GCS blob name for download/delete
        newDoc.setFilePath(blobName);           // Keep filePath in sync with blobName
        newDoc.setPublicUrl(signedUrl);
        newDoc.setFileSize(file.getSize());
        newDoc.setContentType(file.getContentType());
        newDoc.setUploadedByUserId(userId);
        newDoc.setUploadedByUserName(userName);
        newDoc.setUploadedAt(LocalDateTime.now());
        newDoc.setUpdatedAt(LocalDateTime.now());

        ConferenceDocument saved = conferenceDocumentRepository.save(newDoc);
        logger.info("Uploaded conference document - Conference: {}, Year: {}, Type: {}, File: {}",
            conference.getTitle(), year, documentType, file.getOriginalFilename());

        // Log to conference document logs (separate from data logs)
        conferenceDocumentLogService.log(userId, userName, ipAddress,
            ConferenceDocumentLog.ActionType.UPLOAD, saved);

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
        // Use blobName first; fall back to filePath for legacy records
        String gcsKey = (doc.getBlobName() != null && !doc.getBlobName().isEmpty())
                ? doc.getBlobName() : doc.getFilePath();
        if (gcsKey == null || gcsKey.isEmpty()) {
            throw new RuntimeException("Document has no valid GCS path stored: " + documentId);
        }
        byte[] fileContent = gcsService.downloadFile(gcsKey);
        logger.info("Downloaded document - ID: {}, Name: {}, GCS key: {}", documentId, doc.getFileName(), gcsKey);
        return fileContent;
    }

    /**
     * Download document file and log the activity
     */
    public byte[] downloadDocument(String documentId, String userId, String userName, String ipAddress) throws IOException {
        byte[] content = downloadDocument(documentId);
        Optional<ConferenceDocument> docOpt = getDocumentById(documentId);
        docOpt.ifPresent(doc -> conferenceDocumentLogService.log(userId, userName, ipAddress,
            ConferenceDocumentLog.ActionType.DOWNLOAD, doc));
        return content;
    }

    /**
     * Delete document - removes from GCS bucket and hard-deletes from database
     */
    public void deleteDocument(String documentId) {
        Optional<ConferenceDocument> docOpt = conferenceDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        ConferenceDocument doc = docOpt.get();

        // Delete from GCS bucket using blobName (fall back to filePath for legacy records)
        String gcsKey = (doc.getBlobName() != null && !doc.getBlobName().isEmpty())
                ? doc.getBlobName() : doc.getFilePath();
        if (gcsKey != null && !gcsKey.isEmpty()) {
            try {
                gcsService.deleteFile(gcsKey);
                logger.info("Deleted file from GCS bucket - blobName: {}", gcsKey);
            } catch (Exception e) {
                logger.warn("Failed to delete file from GCS (may already be gone): {}", e.getMessage());
            }
        } else {
            logger.warn("No GCS key found for document ID: {}, skipping GCS deletion", documentId);
        }

        // Hard-delete from database
        conferenceDocumentRepository.deleteById(documentId);
        logger.info("Hard-deleted document from database - ID: {}, File: {}", documentId, doc.getFileName());
    }

    /**
     * Delete document and log the activity
     */
    public void deleteDocument(String documentId, String userId, String userName, String ipAddress) {
        // Fetch doc info before deletion for logging
        Optional<ConferenceDocument> docOpt = conferenceDocumentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        ConferenceDocument doc = docOpt.get();
        deleteDocument(documentId);
        // Log to conference document logs (separate from data logs)
        conferenceDocumentLogService.log(userId, userName, ipAddress,
            ConferenceDocumentLog.ActionType.DELETE, doc);
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
     * Get document statistics for a single conference
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

        // Total file size in bytes
        long totalSize = docs.stream()
            .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
            .sum();
        stats.put("totalFileSizeBytes", totalSize);

        // Latest upload timestamp
        docs.stream()
            .map(ConferenceDocument::getUploadedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .ifPresent(t -> stats.put("lastUploadedAt", t));

        return stats;
    }

    /**
     * Get global document statistics across ALL conferences (SUPER_ADMIN use)
     */
    public Map<String, Object> getGlobalDocumentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<ConferenceDocument> all = conferenceDocumentRepository.findAll()
            .stream()
            .filter(d -> !d.isDeleted())
            .collect(Collectors.toList());

        stats.put("totalDocuments", all.size());

        // Unique conferences that have documents
        long totalConferences = all.stream()
            .map(ConferenceDocument::getConferenceId)
            .distinct()
            .count();
        stats.put("totalConferences", totalConferences);

        // Unique years across all conferences
        List<Integer> years = all.stream()
            .map(ConferenceDocument::getYear)
            .distinct()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        stats.put("years", years);

        // Total file size
        long totalSize = all.stream()
            .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
            .sum();
        stats.put("totalFileSizeBytes", totalSize);

        // Documents grouped by conference name
        Map<String, Long> byConference = all.stream()
            .collect(Collectors.groupingBy(
                d -> d.getConferenceName() != null ? d.getConferenceName() : d.getConferenceId(),
                Collectors.counting()
            ));
        stats.put("documentsByConference", byConference);

        // Documents grouped by year
        Map<Integer, Long> byYear = all.stream()
            .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting()));
        stats.put("documentsByYear", byYear);

        // Documents grouped by type
        Map<String, Long> byType = all.stream()
            .collect(Collectors.groupingBy(
                d -> d.getDocumentType().getDisplayName(),
                Collectors.counting()
            ));
        stats.put("documentsByType", byType);

        // Latest upload timestamp
        all.stream()
            .map(ConferenceDocument::getUploadedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .ifPresent(t -> stats.put("lastUploadedAt", t));

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

    /**
     * Get all document data for an admin (scoped to their assigned conferences)
     * Returns: documents grouped by conference, stats per conference, and total stats
     */
    public Map<String, Object> getAdminConferenceDocumentsData(List<String> conferenceIds) {
        Map<String, Object> result = new HashMap<>();

        if (conferenceIds == null || conferenceIds.isEmpty()) {
            result.put("totalConferences", 0);
            result.put("totalDocuments", 0);
            result.put("conferencesData", new ArrayList<>());
            return result;
        }

        // Get all documents for all admin's conferences
        List<ConferenceDocument> allDocs = new ArrayList<>();
        for (String confId : conferenceIds) {
            List<ConferenceDocument> docs = conferenceDocumentRepository
                .findByConferenceIdAndDeletedFalseOrderByYearDescUpdatedAtDesc(confId);
            allDocs.addAll(docs);
        }

        // Build per-conference data
        List<Map<String, Object>> conferencesData = new ArrayList<>();
        for (String confId : conferenceIds) {
            List<ConferenceDocument> docs = allDocs.stream()
                .filter(d -> confId.equals(d.getConferenceId()))
                .collect(Collectors.toList());

            if (docs.isEmpty()) {
                continue; // Skip conferences with no documents
            }

            Map<String, Object> confData = new HashMap<>();

            // Conference info
            Optional<Conference> conf = conferenceRepository.findByIdAndDeletedFalse(confId);
            conf.ifPresent(c -> {
                confData.put("conferenceId", c.getId());
                confData.put("conferenceName", c.getTitle());
                confData.put("conferenceStatus", c.getStatus());
            });

            // Documents count
            confData.put("totalDocuments", docs.size());

            // Documents grouped by year
            Map<Integer, Long> byYear = docs.stream()
                .collect(Collectors.groupingBy(ConferenceDocument::getYear, Collectors.counting()));
            confData.put("documentsByYear", byYear);

            // Documents grouped by type
            Map<String, Long> byType = docs.stream()
                .collect(Collectors.groupingBy(
                    d -> d.getDocumentType().getDisplayName(),
                    Collectors.counting()
                ));
            confData.put("documentsByType", byType);

            // File size stats
            long totalSize = docs.stream()
                .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
                .sum();
            confData.put("totalFileSizeBytes", totalSize);

            // Recent uploads (last 5)
            List<Map<String, Object>> recentUploads = docs.stream()
                .sorted((d1, d2) -> {
                    LocalDateTime t1 = d1.getUploadedAt() != null ? d1.getUploadedAt() : LocalDateTime.MIN;
                    LocalDateTime t2 = d2.getUploadedAt() != null ? d2.getUploadedAt() : LocalDateTime.MIN;
                    return t2.compareTo(t1);
                })
                .limit(5)
                .map(d -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", d.getId());
                    entry.put("fileName", d.getFileName());
                    entry.put("documentType", d.getDocumentType().getDisplayName());
                    entry.put("year", d.getYear());
                    entry.put("fileSize", d.getFileSize() != null ? d.getFileSize() : 0L);
                    entry.put("uploadedAt", d.getUploadedAt()); // HashMap allows null values, unlike Map.of()
                    return entry;
                })
                .collect(Collectors.toList());
            confData.put("recentUploads", recentUploads);

            // Available years
            List<Integer> years = docs.stream()
                .map(ConferenceDocument::getYear)
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
            confData.put("availableYears", years);

            conferencesData.add(confData);
        }

        // Global stats across all admin's conferences
        Map<String, Object> globalStats = new HashMap<>();
        globalStats.put("totalConferences", conferencesData.size());
        globalStats.put("totalDocuments", allDocs.size());

        // Total file size
        long totalSize = allDocs.stream()
            .mapToLong(d -> d.getFileSize() != null ? d.getFileSize() : 0L)
            .sum();
        globalStats.put("totalFileSizeBytes", totalSize);

        // All years across all conferences
        List<Integer> allYears = allDocs.stream()
            .map(ConferenceDocument::getYear)
            .distinct()
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        globalStats.put("years", allYears);

        // Docs by type across all conferences
        Map<String, Object> typeStats = allDocs.stream()
            .collect(Collectors.groupingBy(
                d -> d.getDocumentType().getDisplayName(),
                Collectors.counting()
            )).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (Object) e.getValue()
            ));
        globalStats.put("documentsByType", typeStats);

        // Latest upload
        Optional<LocalDateTime> lastUpload = allDocs.stream()
            .map(ConferenceDocument::getUploadedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo);
        lastUpload.ifPresent(t -> globalStats.put("lastUploadedAt", t));

        result.put("globalStats", globalStats);
        result.put("conferencesData", conferencesData);

        logger.info("Retrieved admin document data - Admin conferences: {}, Total docs: {}",
            conferencesData.size(), allDocs.size());

        return result;
    }
}
