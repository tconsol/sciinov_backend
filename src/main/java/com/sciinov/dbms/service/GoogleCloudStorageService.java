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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
                    try {
                        GoogleCredentials credentials = GoogleCredentials.fromStream(
                            new FileInputStream(credentialsPath)
                        );

                        storage = StorageOptions.newBuilder()
                            .setProjectId(projectId)
                            .setCredentials(credentials)
                            .build()
                            .getService();

                        logger.info("Google Cloud Storage credentials loaded successfully for project: {}", projectId);

                        // Ensure bucket exists - non-blocking
                        ensureBucketExists();

                        if (isConfigured) {
                            logger.info("✅ GCS READY: All systems operational");
                        } else {
                            logger.warn("⚠️  GCS DEGRADED: Credentials valid but bucket access failed. " +
                                "Upload features disabled. Check IAM permissions on service account.");
                        }
                    } catch (Exception authErr) {
                        logger.warn("❌ GCS AUTHENTICATION FAILED: {}. " +
                            "Credentials file at '{}' may be invalid. Application will continue without GCS.",
                            authErr.getMessage(), credentialsPath);
                        isConfigured = false;
                        storage = null;
                    }
                } else {
                    logger.warn("❌ GCS DISABLED: Credentials file not found at '{}'. " +
                        "File upload features will be unavailable. Please place credentials file in project root.",
                        credentialsPath);
                    isConfigured = false;
                }
            } else {
                logger.warn("❌ GCS DISABLED: Configuration incomplete in .env file. " +
                    "Required: GCS_PROJECT_ID={}, GCS_CREDENTIALS_PATH={}, GCS_BUCKET_NAME={}",
                    projectId, credentialsPath, bucketName);
                isConfigured = false;
            }
        } catch (Exception e) {
            logger.error("❌ GCS INITIALIZATION ERROR: Unexpected error during setup: {}", e.getMessage());
            isConfigured = false;
            storage = null;
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
     * Handles permission errors gracefully - app continues to run without GCS
     */
    private void ensureBucketExists() {
        if (storage == null) {
            logger.warn("Storage object is null, skipping bucket verification");
            isConfigured = false;
            return;
        }

        try {
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                // Bucket doesn't exist, try to create it
                try {
                    storage.create(BucketInfo.newBuilder(bucketName)
                        .setStorageClass(StorageClass.STANDARD)
                        .setLocation("US")
                        .build());
                    isConfigured = true;
                    logger.info("✅ Created new GCS bucket: {}", bucketName);
                } catch (com.google.api.gax.rpc.PermissionDeniedException e) {
                    logger.warn("⚠️  Cannot create bucket '{}': Permission denied. " +
                        "Service account needs roles/storage.bucketAdmin role. " +
                        "Grant this role in Google Cloud Console: " +
                        "IAM & Admin > IAM > sciinov@fineflux.iam.gserviceaccount.com > Add Role > Storage Bucket Admin",
                        bucketName);
                    isConfigured = false;
                } catch (Exception e) {
                    logger.warn("⚠️  Cannot create bucket '{}': {}. " +
                        "Ensure bucket exists in Google Cloud Console with name '{}' and service account has permissions.",
                        bucketName, e.getMessage(), bucketName);
                    isConfigured = false;
                }
            } else {
                // Bucket exists and is accessible
                isConfigured = true;
                logger.info("✅ GCS bucket '{}' verified and accessible", bucketName);
            }
        } catch (com.google.api.gax.rpc.PermissionDeniedException e) {
            // Permission denied to even check bucket
            logger.error("❌ PERMISSION DENIED on bucket '{}': Service account lacks 'storage.buckets.get' permission. " +
                "\n\n📋 TO FIX THIS:\n" +
                "1. Go to: https://console.cloud.google.com/iam-admin/iam?project=fineflux\n" +
                "2. Find service account: sciinov@fineflux.iam.gserviceaccount.com\n" +
                "3. Click EDIT (pencil icon)\n" +
                "4. Click 'ADD ANOTHER ROLE'\n" +
                "5. Search for and select: 'Cloud Storage > Storage Object Admin' OR 'Cloud Storage > Storage Bucket Admin'\n" +
                "6. Click SAVE\n" +
                "7. Restart the application\n\n" +
                "Error details: {}",
                bucketName, e.getMessage());
            isConfigured = false;
        } catch (Exception e) {
            logger.warn("⚠️  Failed to verify bucket access: {}. " +
                "Check if bucket '{}' exists and service account has permissions.",
                e.getMessage(), bucketName);
            isConfigured = false;
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
     * Generate a signed URL for a file in GCS
     * Signed URLs allow authenticated access without making the bucket public
     *
     * @param blobName The blob name (full path) in GCS
     * @return Signed URL with lifetime access (no expiration)
     */
    public String generateSignedUrl(String blobName) {
        return generateSignedUrl(blobName, 3650); // 10 years = lifetime access
    }

    /**
     * Generate a signed URL for a file in GCS with custom expiration
     * Signed URLs allow authenticated access without making the bucket public
     *
     * @param blobName The blob name (full path) in GCS
     * @param expirationDays Number of days the URL should remain valid (use 3650 for 10 years/lifetime)
     * @return Signed URL with embedded authentication
     */
    public String generateSignedUrl(String blobName, int expirationDays) {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not properly configured."
            );
        }

        try {
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            URL signedUrl = storage.signUrl(
                blobInfo,
                expirationDays,
                TimeUnit.DAYS
            );

            String expirationDesc = (expirationDays >= 3650) ? "lifetime" : expirationDays + " days";
            logger.info("Generated signed URL for: {} (expires in: {})", blobName, expirationDesc);
            return signedUrl.toString();
        } catch (Exception e) {
            logger.error("Failed to generate signed URL for {}: {}", blobName, e.getMessage());
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        }
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
     * @return Signed URL of the uploaded file (lifetime access, no expiration)
     */
    public String uploadFile(MultipartFile file, String conferenceId, String dashboardMasterId) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not properly configured. " +
                "Please check logs for configuration errors and ensure:\n" +
                "1. Credentials file exists at path specified in GCS_CREDENTIALS_PATH\n" +
                "2. Service account has 'Storage Object Admin' or 'Storage Bucket Admin' role\n" +
                "3. Bucket '" + bucketName + "' exists in Google Cloud Console\n" +
                "4. All environment variables are set in .env file"
            );
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

        // Generate signed URL with lifetime access (no expiration)
        String signedUrl = generateSignedUrl(blobName);
        logger.info("File uploaded to GCS with lifetime signed URL: {}", signedUrl);

        return signedUrl;
    }

    /**
     * Upload file with custom filename
     * @return Signed URL of the uploaded file (lifetime access, no expiration)
     */
    public String uploadFile(MultipartFile file, String conferenceId, String dashboardMasterId, String customFilename) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not properly configured. " +
                "Check logs and ensure service account has 'Storage Object Admin' role."
            );
        }

        String folderPath = generateFolderPath(conferenceId, dashboardMasterId);
        String blobName = folderPath + customFilename;

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();

        storage.create(blobInfo, file.getBytes());

        // Generate signed URL with lifetime access (no expiration)
        String signedUrl = generateSignedUrl(blobName);
        logger.info("File uploaded to GCS with lifetime signed URL: {}", signedUrl);

        return signedUrl;
    }

    /**
     * Upload file to custom folder path (for conference documents)
     * @param file File to upload
     * @param customFolderPath Custom folder path (e.g., "conferences/tech-summit/2026/program/")
     * @return Signed URL of uploaded file (lifetime access, no expiration)
     */
    public String uploadFile(MultipartFile file, String customFolderPath) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not properly configured. " +
                "Check logs and ensure service account has 'Storage Object Admin' role."
            );
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String fileName = sanitizeFileName(originalFilename);
        String blobName = customFolderPath + fileName;

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .build();

        storage.create(blobInfo, file.getBytes());

        // Generate signed URL with lifetime access (no expiration)
        String signedUrl = generateSignedUrl(blobName);
        logger.info("File uploaded to GCS with lifetime signed URL: {}", signedUrl);

        return signedUrl;
    }

    /**
     * Upload file from InputStream
     * @return Signed URL of uploaded file (lifetime access, no expiration)
     */
    public String uploadFile(InputStream inputStream, String conferenceId, String dashboardMasterId,
                             String filename, String contentType) throws IOException {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not properly configured. " +
                "Check logs and ensure service account has 'Storage Object Admin' role."
            );
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

        // Generate signed URL with lifetime access (no expiration)
        String signedUrl = generateSignedUrl(blobName);
        logger.info("File uploaded to GCS with lifetime signed URL: {}", signedUrl);

        return signedUrl;
    }

    /**
     * Download file from GCS
     */
    public byte[] downloadFile(String blobName) {
        if (!isConfigured) {
            throw new IllegalStateException(
                "Google Cloud Storage is not configured. Please check application logs for details."
            );
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
            throw new IllegalStateException(
                "Google Cloud Storage is not configured. Please check application logs for details."
            );
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
            throw new IllegalStateException(
                "Google Cloud Storage is not configured. Please check application logs for details."
            );
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
            throw new IllegalStateException(
                "Google Cloud Storage is not configured. Please check application logs for details."
            );
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
     * Sanitize file name (remove special characters, preserve extension)
     */
    private String sanitizeFileName(String filename) {
        if (filename == null) return "file";

        // Get extension
        String extension = getFileExtension(filename);

        // Remove extension from name
        String nameWithoutExt = filename;
        if (extension.length() > 0) {
            nameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
        }

        // Sanitize name
        String sanitized = nameWithoutExt.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        if (sanitized.isEmpty()) {
            sanitized = "file";
        }

        return sanitized + extension;
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

