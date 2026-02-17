package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.MessageResponse;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.GoogleCloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private GoogleCloudStorageService gcsService;

    /**
     * Upload a file to Google Cloud Storage
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conferenceId") String conferenceId,
            @RequestParam("dashboardMasterId") String dashboardMasterId) {

        validateAccess(conferenceId);

        if (!gcsService.isConfigured()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Google Cloud Storage is not configured", false));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Please select a file to upload", false));
        }

        try {
            String fileUrl = gcsService.uploadFile(file, conferenceId, dashboardMasterId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("fileUrl", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed to upload file: " + e.getMessage(), false));
        }
    }

    /**
     * Upload multiple files
     */
    @PostMapping("/upload-multiple")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("conferenceId") String conferenceId,
            @RequestParam("dashboardMasterId") String dashboardMasterId) {

        validateAccess(conferenceId);

        if (!gcsService.isConfigured()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Google Cloud Storage is not configured", false));
        }

        if (files.length == 0) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Please select files to upload", false));
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalFiles", files.length);

            java.util.List<Map<String, Object>> uploadedFiles = new java.util.ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (MultipartFile file : files) {
                try {
                    String fileUrl = gcsService.uploadFile(file, conferenceId, dashboardMasterId);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", file.getOriginalFilename());
                    fileInfo.put("fileUrl", fileUrl);
                    fileInfo.put("status", "success");
                    uploadedFiles.add(fileInfo);
                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileName", file.getOriginalFilename());
                    fileInfo.put("error", e.getMessage());
                    fileInfo.put("status", "failed");
                    uploadedFiles.add(fileInfo);
                    failCount++;
                }
            }

            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("files", uploadedFiles);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed to upload files: " + e.getMessage(), false));
        }
    }

    /**
     * List files in a conference/dashboard folder
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> listFiles(
            @RequestParam("conferenceId") String conferenceId,
            @RequestParam("dashboardMasterId") String dashboardMasterId) {

        validateAccess(conferenceId);

        if (!gcsService.isConfigured()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Google Cloud Storage is not configured", false));
        }

        try {
            List<String> files = gcsService.listFiles(conferenceId, dashboardMasterId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("folderPath", gcsService.generateFolderPath(conferenceId, dashboardMasterId));
            response.put("fileCount", files.size());
            response.put("files", files);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed to list files: " + e.getMessage(), false));
        }
    }

    /**
     * Delete a file from GCS
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteFile(@RequestParam("blobName") String blobName) {
        if (!gcsService.isConfigured()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Google Cloud Storage is not configured", false));
        }

        try {
            boolean deleted = gcsService.deleteFile(blobName);

            if (deleted) {
                return ResponseEntity.ok(new MessageResponse("File deleted successfully", true));
            } else {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("File not found", false));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed to delete file: " + e.getMessage(), false));
        }
    }

    /**
     * Get signed URL for a file (temporary access)
     */
    @GetMapping("/signed-url")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getSignedUrl(@RequestParam("blobName") String blobName) {
        if (!gcsService.isConfigured()) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Google Cloud Storage is not configured", false));
        }

        try {
            String signedUrl = gcsService.getSignedUrl(blobName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("signedUrl", signedUrl);
            response.put("validForMinutes", 15);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Failed to get signed URL: " + e.getMessage(), false));
        }
    }

    /**
     * Check GCS configuration status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getStorageStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("configured", gcsService.isConfigured());
        response.put("bucketName", gcsService.getBucketName());

        return ResponseEntity.ok(response);
    }

    private void validateAccess(String conferenceId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDetails.getUser();
        if (user.getRole() == User.Role.ADMIN) {
            if (user.getConferenceIds() == null || !user.getConferenceIds().contains(conferenceId)) {
                throw new AccessDeniedException("Access Denied: You are not assigned to this conference.");
            }
        }
    }
}

