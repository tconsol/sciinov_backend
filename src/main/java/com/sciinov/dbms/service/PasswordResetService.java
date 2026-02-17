package com.sciinov.dbms.service;

import com.sciinov.dbms.dto.*;
import com.sciinov.dbms.entity.PasswordResetToken;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.PasswordResetTokenRepository;
import com.sciinov.dbms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    // Token validity in hours
    private static final int TOKEN_VALIDITY_HOURS = 24;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /**
     * Generate password reset token for a user based on userId
     */
    @Transactional
    public PasswordResetTokenResponse generatePasswordResetToken(ForgotPasswordRequest request) {
        String userId = request.getUserId();
        logger.info("Password reset request for userId: {}", userId);

        Optional<User> userOpt = userRepository.findByUserIdAndDeletedFalse(userId);

        if (userOpt.isEmpty()) {
            logger.warn("User not found for password reset: {}", userId);
            return new PasswordResetTokenResponse(null, null, null,
                "User not found with the provided User ID", false);
        }

        User user = userOpt.get();

        if (!user.isStatus()) {
            logger.warn("Inactive user attempted password reset: {}", userId);
            return new PasswordResetTokenResponse(null, null, null,
                "Your account is inactive. Please contact administrator.", false);
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS);

        PasswordResetToken resetToken = new PasswordResetToken(token, user.getId(), expiryDate);
        resetToken.setCreatedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        logger.info("Password reset token generated for user: {}", userId);

        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUserId(), token);
        } catch (Exception e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
        }

        return new PasswordResetTokenResponse(token, user.getUserId(), user.getEmail(),
            "Password reset link has been sent to your registered email. Token is valid for " + TOKEN_VALIDITY_HOURS + " hours.", true);
    }

    /**
     * Validate reset token
     */
    public MessageResponse validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByTokenAndUsedFalse(token);

        if (tokenOpt.isEmpty()) {
            return new MessageResponse("Invalid or expired token", false);
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            return new MessageResponse("Token has expired. Please request a new password reset.", false);
        }

        return new MessageResponse("Token is valid", true);
    }

    /**
     * Reset password using token
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        logger.info("Password reset attempt with token");

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new MessageResponse("Passwords do not match", false);
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken());

        if (tokenOpt.isEmpty()) {
            logger.warn("Invalid token used for password reset");
            return new MessageResponse("Invalid or expired token", false);
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            logger.warn("Expired token used for password reset");
            return new MessageResponse("Token has expired. Please request a new password reset.", false);
        }

        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());

        if (userOpt.isEmpty()) {
            return new MessageResponse("User not found", false);
        }

        User user = userOpt.get();

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Send password changed confirmation email
        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            logger.error("Failed to send password changed email: {}", e.getMessage());
        }

        logger.info("Password reset successful for user: {}", user.getUserId());

        return new MessageResponse("Password has been reset successfully. You can now login with your new password.", true);
    }

    /**
     * Find username by email or phone number
     */
    public ForgotUsernameResponse findUsername(ForgotUsernameRequest request) {
        logger.info("Username recovery request");

        if ((request.getEmail() == null || request.getEmail().isEmpty()) &&
            (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())) {
            return new ForgotUsernameResponse(null, null,
                "Please provide either email or phone number", false);
        }

        Optional<User> userOpt = Optional.empty();

        // Try to find by email first
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userOpt = userRepository.findByEmailAndDeletedFalse(request.getEmail());
        }

        // If not found by email, try phone number
        if (userOpt.isEmpty() && request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            userOpt = userRepository.findByPhoneNumberAndDeletedFalse(request.getPhoneNumber());
        }

        if (userOpt.isEmpty()) {
            logger.warn("User not found for username recovery");
            return new ForgotUsernameResponse(null, null,
                "No user found with the provided email or phone number", false);
        }

        User user = userOpt.get();

        if (!user.isStatus()) {
            return new ForgotUsernameResponse(null, null,
                "Your account is inactive. Please contact administrator.", false);
        }

        // Send username recovery email
        try {
            emailService.sendForgotUsernameEmail(user.getEmail(), user.getUserId(), user.getFirstName());
        } catch (Exception e) {
            logger.error("Failed to send forgot username email: {}", e.getMessage());
        }

        logger.info("Username found for recovery request");

        // Return masked username for security (actual username sent via email)
        return new ForgotUsernameResponse(user.getUserId(), user.getEmail(),
            "Your User ID has been sent to your registered email.", true);
    }

    /**
     * Change password for logged-in user
     */
    @Transactional
    public MessageResponse changePassword(String userId, ChangePasswordRequest request) {
        logger.info("Password change request for user: {}", userId);

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new MessageResponse("New passwords do not match", false);
        }

        Optional<User> userOpt = userRepository.findByUserIdAndDeletedFalse(userId);

        if (userOpt.isEmpty()) {
            return new MessageResponse("User not found", false);
        }

        User user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            logger.warn("Incorrect current password for user: {}", userId);
            return new MessageResponse("Current password is incorrect", false);
        }

        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return new MessageResponse("New password cannot be the same as current password", false);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Send password changed confirmation email
        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            logger.error("Failed to send password changed email: {}", e.getMessage());
        }

        logger.info("Password changed successfully for user: {}", userId);

        return new MessageResponse("Password changed successfully", true);
    }
}

