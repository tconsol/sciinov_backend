package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.*;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.security.JwtUtils;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.PasswordResetService;
import com.sciinov.dbms.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetService passwordResetService;

    @Autowired
    RefreshTokenService refreshTokenService;

    /**
     * Get JWT token expiration info (in milliseconds)
     * GET /api/auth/token-expiration
     */
    @GetMapping("/token-expiration")
    public ResponseEntity<?> getTokenExpiration() {
        logger.info("GET /api/auth/token-expiration - Retrieving JWT expiration time");
        int expirationMs = jwtUtils.getJwtExpirationMs();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "tokenExpirationMs", expirationMs,
                "tokenExpirationMinutes", expirationMs / 60000,
                "tokenExpirationHours", expirationMs / 3600000
        ));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("POST /api/auth/signin - Authenticating user: {}", loginRequest.getUserId());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            // Generate refresh token
            String refreshToken = refreshTokenService.generateRefreshToken(userDetails.getId());

            logger.info("POST /api/auth/signin - User authenticated successfully: {} with roles: {}", loginRequest.getUserId(), roles);

            return ResponseEntity.ok(new JwtResponse(jwt,
                    refreshToken,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUser().getEmail(),
                    roles));
        } catch (AuthenticationException e) {
            logger.warn("POST /api/auth/signin - Authentication failed for user: {} - {}", loginRequest.getUserId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Forgot Password - Generate reset token based on User ID
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetTokenResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("POST /api/auth/forgot-password - Processing password reset request for userId: {}", request.getUserId());
        PasswordResetTokenResponse response = passwordResetService.generatePasswordResetToken(request);
        if (response.isSuccess()) {
            logger.info("POST /api/auth/forgot-password - Password reset token generated for: {} sent to: {}", request.getUserId(), response.getEmail());
            return ResponseEntity.ok(response);
        }
        logger.warn("POST /api/auth/forgot-password - Failed to generate token for: {} - {}", request.getUserId(), response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Validate password reset token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<MessageResponse> validateResetToken(@RequestParam String token) {
        logger.info("GET /api/auth/validate-reset-token - Validating reset token");
        MessageResponse response = passwordResetService.validateResetToken(token);
        if (response.isSuccess()) {
            logger.info("GET /api/auth/validate-reset-token - Token validated successfully");
            return ResponseEntity.ok(response);
        }
        logger.warn("GET /api/auth/validate-reset-token - Token validation failed: {}", response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Reset Password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("POST /api/auth/reset-password - Processing password reset");
        MessageResponse response = passwordResetService.resetPassword(request);
        if (response.isSuccess()) {
            logger.info("POST /api/auth/reset-password - Password reset successful");
            return ResponseEntity.ok(response);
        }
        logger.warn("POST /api/auth/reset-password - Password reset failed: {}", response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Forgot Username - Find username by email or phone number
     */
    @PostMapping("/forgot-username")
    public ResponseEntity<ForgotUsernameResponse> forgotUsername(@RequestBody ForgotUsernameRequest request) {
        logger.info("POST /api/auth/forgot-username - Processing username recovery request");
        ForgotUsernameResponse response = passwordResetService.findUsername(request);
        if (response.isSuccess()) {
            logger.info("POST /api/auth/forgot-username - Username recovery email sent to: {}", response.getEmail());
            return ResponseEntity.ok(response);
        }
        logger.warn("POST /api/auth/forgot-username - Username recovery failed: {}", response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Change Password for logged-in user
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info("POST /api/auth/change-password - User {} changing password", userDetails.getUsername());
        MessageResponse response = passwordResetService.changePassword(userDetails.getUsername(), request);
        if (response.isSuccess()) {
            logger.info("POST /api/auth/change-password - Password changed successfully for: {}", userDetails.getUsername());
            return ResponseEntity.ok(response);
        }
        logger.warn("POST /api/auth/change-password - Password change failed for: {} - {}", userDetails.getUsername(), response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Refresh Access Token using Refresh Token
     * POST /api/auth/refresh-token
     * Body: { "refreshToken": "..." }
     * Returns: New access token and refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logger.info("POST /api/auth/refresh-token - Attempting to refresh access token");

        try {
            // Validate refresh token
            if (!refreshTokenService.validateRefreshToken(request.getRefreshToken())) {
                logger.warn("POST /api/auth/refresh-token - Invalid or expired refresh token");
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Invalid or expired refresh token"
                ));
            }

            // Get user ID from refresh token
            java.util.Optional<String> userIdOpt = refreshTokenService.getUserIdFromRefreshToken(request.getRefreshToken());

            if (userIdOpt.isEmpty()) {
                logger.warn("POST /api/auth/refresh-token - Could not extract user ID from refresh token");
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "Invalid refresh token"
                ));
            }

            String userId = userIdOpt.get();

            // Load user details
            com.sciinov.dbms.entity.User user = userRepository.findById(userId).orElse(null);

            if (user == null || user.isDeleted() || !user.isStatus()) {
                logger.warn("POST /api/auth/refresh-token - User not found or inactive: {}", userId);
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "User not found or inactive"
                ));
            }

            // Create authentication and generate new JWT token
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            String newAccessToken = jwtUtils.generateJwtToken(auth);

            // Optionally generate new refresh token (or reuse the old one)
            // For security, we can either:
            // 1. Keep the same refresh token (current implementation)
            // 2. Generate a new one (uncomment line below)
            String newRefreshToken = request.getRefreshToken(); // Reuse existing refresh token

            List<String> roles = user.getRole().toString().equals("SUPER_ADMIN") ?
                    java.util.List.of("ROLE_SUPER_ADMIN") : java.util.List.of("ROLE_ADMIN");

            logger.info("POST /api/auth/refresh-token - Access token refreshed for user: {}", userId);

            return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, newRefreshToken));

        } catch (Exception e) {
            logger.error("POST /api/auth/refresh-token - Error refreshing token: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error refreshing token"
            ));
        }
    }

    /**
     * Logout - Clear JWT token and revoke refresh token
     * POST /api/auth/logout
     * Requires: None (works with or without valid JWT token)
     *
     * Behavior:
     *  - If Bearer token is valid → revoke refresh token(s) and clear session
     *  - If Bearer token is expired/invalid → still return 200 (graceful logout)
     *  - If refreshToken is in body → revoke only that token
     *  - If no refreshToken → revoke all tokens for the user (if authenticated)
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody(required = false) Map<String, String> body) {
        try {
            // Try to get current user (may be null if token is invalid/expired)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
                // User IS authenticated — revoke tokens and clear context
                UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                logger.info("POST /api/auth/logout - Authenticated user {} logging out", userDetails.getUsername());

                // Revoke refresh token if provided in body
                if (body != null && body.containsKey("refreshToken")) {
                    String refreshToken = body.get("refreshToken");
                    refreshTokenService.revokeRefreshToken(refreshToken);
                    logger.info("POST /api/auth/logout - Refresh token revoked for user: {}", userDetails.getUsername());
                } else {
                    // Revoke all refresh tokens for the user
                    refreshTokenService.revokeAllTokensForUser(userDetails.getId());
                    logger.info("POST /api/auth/logout - All refresh tokens revoked for user: {}", userDetails.getUsername());
                }

                // Clear authentication from security context
                SecurityContextHolder.clearContext();
                logger.info("POST /api/auth/logout - User {} logged out successfully", userDetails.getUsername());

            } else {
                // User is NOT authenticated (token expired/invalid/missing)
                // Still allow logout gracefully — this is normal and expected
                logger.info("POST /api/auth/logout - Logout request received (user not authenticated or token expired)");

                // If refreshToken is in body, try to revoke it anyway
                if (body != null && body.containsKey("refreshToken")) {
                    String refreshToken = body.get("refreshToken");
                    try {
                        refreshTokenService.revokeRefreshToken(refreshToken);
                        logger.info("POST /api/auth/logout - Refresh token revoked (unauthenticated logout)");
                    } catch (Exception e) {
                        logger.debug("POST /api/auth/logout - Could not revoke refresh token: {}", e.getMessage());
                    }
                }
            }

            // ALWAYS return 200 OK — logout is successful regardless of token state
            return ResponseEntity.ok(new MessageResponse("Logged out successfully", true));

        } catch (Exception e) {
            logger.error("POST /api/auth/logout - Error during logout: {}", e.getMessage());
            // Even on error, return success — logout intent was clear
            return ResponseEntity.ok(new MessageResponse("Logged out successfully", true));
        }
    }
}
