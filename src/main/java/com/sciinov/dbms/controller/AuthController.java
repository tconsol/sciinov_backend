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
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Attempting to authenticate user: {}", loginRequest.getUserId());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            logger.info("User authenticated successfully: {}", loginRequest.getUserId());

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUser().getEmail(),
                    roles));
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUserId(), e);
            throw e;
        }
    }

    /**
     * Forgot Password - Generate reset token based on User ID
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetTokenResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot password request for userId: {}", request.getUserId());
        PasswordResetTokenResponse response = passwordResetService.generatePasswordResetToken(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Validate password reset token
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<MessageResponse> validateResetToken(@RequestParam String token) {
        logger.info("Validating reset token");
        MessageResponse response = passwordResetService.validateResetToken(token);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Reset Password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Reset password request");
        MessageResponse response = passwordResetService.resetPassword(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Forgot Username - Find username by email or phone number
     */
    @PostMapping("/forgot-username")
    public ResponseEntity<ForgotUsernameResponse> forgotUsername(@RequestBody ForgotUsernameRequest request) {
        logger.info("Forgot username request");
        ForgotUsernameResponse response = passwordResetService.findUsername(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Change Password for logged-in user
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info("Change password request for user: {}", userDetails.getUsername());
        MessageResponse response = passwordResetService.changePassword(userDetails.getUsername(), request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
