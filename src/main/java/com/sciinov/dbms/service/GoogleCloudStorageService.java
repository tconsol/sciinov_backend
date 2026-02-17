package com.sciinov.dbms.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.api.gax.paging.Page;
import com.sciinov.dbms.entity.Conference;
import com.sciinov.dbms.entity.DashboardMaster;
import com.sciinov.dbms.repository.ConferenceRepository;
import com.sciinov.dbms.repository.DashboardMasterRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleCloudStorageService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorageService.class);

    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${gcs.credentials-path}")
    private String credentialsPath;

    @Autowired
    private ConferenceRepository conferenceRepository;

    @Autowired
    private DashboardMasterRepository dashboardMasterRepository;

    private Storage storage;
    private boolean isConfigured = false;

    @PostConstruct
    public void init() {
        try {
            if (credentialsPath != null && !credentialsPath.isEmpty() &&
                projectId != null && !projectId.isEmpty()) {

                Path path = Path.of(credentialsPath);
                if (Files.exists(path)) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new FileInputStream(credentialsPath)
                    );

                    storage = StorageOptions.newBuilder()
                        .setProjectId(projectId)
                        .setCredentials(credentials)
                        .build()
                        .getService();

                    isConfigured = true;
                    logger.info("Google Cloud Storage initialized successfully for project: {}", projectId);

                    // Ensure bucket exists
                    ensureBucketExists();
                } else {
                    logger.warn("GCS credentials file not found at: {}. GCS features will be disabled.", credentialsPath);
                }
            } else {
                logger.warn("GCS configuration incomplete. GCS features will be disabled.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Google Cloud Storage: {}", e.getMessage());
            isConfigured = false;
        }
    }

    /**
     * Check if GCS is properly configured
     */
    public boolean isConfigured() {
        return isConfigured;
    }

    /**
     * Ensure the bucket exists, create if not
     */
    private void ensureBucketExists() {
        try {
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                storage.create(BucketInfo.newBuilder(bucketName)
                    .setStorageClass(StorageClass.STANDARD)
                    .setLocation("US")
                    .build());
                logger.info("Created new GCS bucket: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to ensure bucket exists: {}", e.getMessage());
        }
    }

    /**
     * Generate folder path based on conference name and dashboard name
     * Format: conferences/{conference-name}/{dashboard-name}/
     */
    public String generateFolderPath(String conferenceId, String dashboardMasterId) {
        String conferenceName = "unknown-conference";
        String dashboardName = "unknown-dashboard";

        // Get conference name
        Optional<Conference> conferenceOpt = conferenceRepository.findByIdAndDeletedFalse(conferenceId);
        if (conferenceOpt.isPresent()) {
            conferenceName = sanitizeFolderName(conferenceOpt.get().getTitle());
        }

        // Get dashboard name
        Optional<DashboardMaster> dashboardOpt = dashboardMasterRepository.findByIdAndDeletedFalse(dashboardMasterId);
        if (dashboardOpt.isPresent()) {
            dashboardName = sanitizeFolderName(dashboardOpt.get().getName());
        }

        return String.format("conferences/%s/%s/", conferenceName, dashboardName);
    }

    /**
     * Sanitize folder name (remove special characters, replace spaces)
     */
    private String sanitizeFolderName(String name) {
        if (name == null) return "unnamed";
        return name.toLowerCase()
                   .replaceAll("[^a-zA-Z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .trim();
    }

    /**
     * Upload file to GCS
     * @return Public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String conferenceId, String dashboardMasterId) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        String folderPath = generateFolderPath(conferenceId, dashboardMasterId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = getFileExtension(originalFilename);

        String blobName = folderPath + timestamp + "-" + uniqueId + extension;

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();

        storage.create(blobInfo, file.getBytes());

        String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
        logger.info("File uploaded to GCS: {}", publicUrl);

        return publicUrl;
    }

    /**
     * Upload file with custom filename
     */
    public String uploadFile(MultipartFile file, String conferenceId, String dashboardMasterId, String customFilename) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        String folderPath = generateFolderPath(conferenceId, dashboardMasterId);
        String blobName = folderPath + customFilename;

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();

        storage.create(blobInfo, file.getBytes());

        String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
        logger.info("File uploaded to GCS: {}", publicUrl);

        return publicUrl;
    }

    /**
     * Upload file from InputStream
     */
    public String uploadFile(InputStream inputStream, String conferenceId, String dashboardMasterId,
                             String filename, String contentType) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        String folderPath = generateFolderPath(conferenceId, dashboardMasterId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(filename);

        String blobName = folderPath + timestamp + "-" + uniqueId + extension;

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .build();

        storage.create(blobInfo, inputStream.readAllBytes());

        String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
        logger.info("File uploaded to GCS: {}", publicUrl);

        return publicUrl;
    }

    /**
     * Download file from GCS
     */
    public byte[] downloadFile(String blobName) {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);

        if (blob == null) {
            throw new RuntimeException("File not found: " + blobName);
        }

        return blob.getContent();
    }

    /**
     * Delete file from GCS
     */
    public boolean deleteFile(String blobName) {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        BlobId blobId = BlobId.of(bucketName, blobName);
        boolean deleted = storage.delete(blobId);

        if (deleted) {
            logger.info("File deleted from GCS: {}", blobName);
        } else {
            logger.warn("File not found for deletion: {}", blobName);
        }

        return deleted;
    }

    /**
     * List files in a folder
     */
    public List<String> listFiles(String conferenceId, String dashboardMasterId) {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        String folderPath = generateFolderPath(conferenceId, dashboardMasterId);
        List<String> files = new ArrayList<>();

        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(folderPath));
        for (Blob blob : blobs.iterateAll()) {
            if (!blob.getName().endsWith("/")) {
                files.add(blob.getName());
            }
        }

        return files;
    }

    /**
     * Get signed URL for private file access (valid for 15 minutes)
     */
    public String getSignedUrl(String blobName) {
        if (!isConfigured) {
            throw new IllegalStateException("Google Cloud Storage is not configured");
        }

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        return storage.signUrl(blobInfo, 15, java.util.concurrent.TimeUnit.MINUTES).toString();
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String blobName) {
        if (!isConfigured) {
            return false;
        }

        BlobId blobId = BlobId.of(bucketName, blobName);
        Blob blob = storage.get(blobId);
        return blob != null && blob.exists();
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Get bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Create folder structure for a conference
     */
    public void createConferenceFolder(String conferenceId) {
        if (!isConfigured) {
            logger.warn("GCS not configured, skipping folder creation");
            return;
        }

        try {
            Optional<Conference> conferenceOpt = conferenceRepository.findByIdAndDeletedFalse(conferenceId);
            if (conferenceOpt.isPresent()) {
                String folderPath = "conferences/" + sanitizeFolderName(conferenceOpt.get().getTitle()) + "/";

                // Create a placeholder file to create the folder
                BlobId blobId = BlobId.of(bucketName, folderPath + ".folder");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/x-directory")
                    .build();
                storage.create(blobInfo, new byte[0]);

                logger.info("Created GCS folder for conference: {}", folderPath);
            }
        } catch (Exception e) {
            logger.error("Failed to create conference folder: {}", e.getMessage());
        }
    }

    /**
     * Create folder structure for a dashboard under a conference
     */
    public void createDashboardFolder(String conferenceId, String dashboardMasterId) {
        if (!isConfigured) {
            logger.warn("GCS not configured, skipping folder creation");
            return;
        }

        try {
            String folderPath = generateFolderPath(conferenceId, dashboardMasterId);

            // Create a placeholder file to create the folder
            BlobId blobId = BlobId.of(bucketName, folderPath + ".folder");
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/x-directory")
                .build();
            storage.create(blobInfo, new byte[0]);

            logger.info("Created GCS folder: {}", folderPath);
        } catch (Exception e) {
            logger.error("Failed to create dashboard folder: {}", e.getMessage());
        }
    }
}

