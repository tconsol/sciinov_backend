package com.sciinov.dbms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.password-reset-url}")
    private String passwordResetUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Send simple text email with retry logic
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromName + " <" + fromEmail + ">");
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                logger.info("Simple email sent successfully to: {}", to);
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    logger.error("Failed to send simple email to {} after {} retries: {}", to, maxRetries, e.getMessage());
                } else {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000; // Exponential backoff
                    logger.warn("Retry {} for email to {} after {}ms. Error: {}", retryCount, to, waitTime, e.getMessage());
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Send HTML email with retry logic
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromName + " <" + fromEmail + ">");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                logger.info("HTML email sent successfully to: {}", to);
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    logger.error("Failed to send HTML email to {} after {} retries: {}", to, maxRetries, e.getMessage());
                } else {
                    long waitTime = (long) Math.pow(2, retryCount - 1) * 1000; // Exponential backoff
                    logger.warn("Retry {} for HTML email to {} after {}ms. Error: {}", retryCount, to, waitTime, e.getMessage());
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String userId, String token) {
        try {
            String resetLink = passwordResetUrl + "?token=" + token;

            Context context = new Context();
            context.setVariable("userId", userId);
            context.setVariable("resetLink", resetLink);
            context.setVariable("expiryHours", 24);
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("password-reset-email", context);

            sendHtmlEmail(to, "Password Reset Request - SciInov DBMS", htmlContent);
            logger.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            // Fallback to simple email
            sendSimplePasswordResetEmail(to, userId, token);
        }
    }

    /**
     * Fallback simple password reset email
     */
    private void sendSimplePasswordResetEmail(String to, String userId, String token) {
        String resetLink = passwordResetUrl + "?token=" + token;
        String text = String.format(
            "Hello %s,\n\n" +
            "You have requested to reset your password for SciInov DBMS.\n\n" +
            "Please click the link below to reset your password:\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not request this password reset, please ignore this email.\n\n" +
            "Best regards,\n" +
            "SciInov DBMS Team",
            userId, resetLink
        );

        sendSimpleEmail(to, "Password Reset Request - SciInov DBMS", text);
    }

    /**
     * Send forgot username email
     */
    @Async
    public void sendForgotUsernameEmail(String to, String userId, String firstName) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("userId", userId);
            context.setVariable("loginUrl", baseUrl + "/login");
            context.setVariable("baseUrl", baseUrl);

            String htmlContent = templateEngine.process("forgot-username-email", context);

            sendHtmlEmail(to, "Your Username - SciInov DBMS", htmlContent);
            logger.info("Forgot username email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send forgot username email to {}: {}", to, e.getMessage());
            // Fallback to simple email
            sendSimpleForgotUsernameEmail(to, userId, firstName);
        }
    }

    /**
     * Fallback simple forgot username email
     */
    private void sendSimpleForgotUsernameEmail(String to, String userId, String firstName) {
        String text = String.format(
            "Hello %s,\n\n" +
            "You have requested to recover your username for SciInov DBMS.\n\n" +
            "Your User ID is: %s\n\n" +
            "You can login at: %s/login\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Best regards,\n" +
            "SciInov DBMS Team",
            firstName, userId, baseUrl
        );

        sendSimpleEmail(to, "Your Username - SciInov DBMS", text);
    }

    /**
     * Send password changed confirmation email
     */
    @Async
    public void sendPasswordChangedEmail(String to, String firstName) {
        String text = String.format(
            "Hello %s,\n\n" +
            "Your password has been successfully changed for SciInov DBMS.\n\n" +
            "If you did not make this change, please contact the administrator immediately.\n\n" +
            "Best regards,\n" +
            "SciInov DBMS Team",
            firstName
        );

        sendSimpleEmail(to, "Password Changed - SciInov DBMS", text);
    }

    /**
     * Send welcome email to new user
     */
    @Async
    public void sendWelcomeEmail(String to, String firstName, String userId) {
        String text = String.format(
            "Hello %s,\n\n" +
            "Welcome to SciInov DBMS!\n\n" +
            "Your account has been created successfully.\n\n" +
            "Your User ID is: %s\n\n" +
            "You can login at: %s/login\n\n" +
            "Best regards,\n" +
            "SciInov DBMS Team",
            firstName, userId, baseUrl
        );

        sendSimpleEmail(to, "Welcome to SciInov DBMS", text);
    }
}

