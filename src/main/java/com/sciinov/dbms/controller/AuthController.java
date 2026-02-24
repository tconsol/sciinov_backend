package com.sciinov.dbms.controller;

import com.sciinov.dbms.dto.*;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.security.JwtUtils;
import com.sciinov.dbms.security.UserDetailsImpl;
import com.sciinov.dbms.service.PasswordResetService;
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

            logger.info("POST /api/auth/signin - User authenticated successfully: {} with roles: {}", loginRequest.getUserId(), roles);

            return ResponseEntity.ok(new JwtResponse(jwt,
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
}
