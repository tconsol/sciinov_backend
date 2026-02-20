package com.sciinov.dbms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean mailAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:15000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:15000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:15000}")
    private int writeTimeout;

    /**
     * Configure JavaMailSender with SMTP properties
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Set SMTP server details
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        // Set additional mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", mailAuth);
        props.put("mail.smtp.starttls.enable", startTlsEnable);
        props.put("mail.smtp.starttls.required", startTlsRequired);
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);

        // Log configuration (excluding password)
        logger.info("Mail Configuration Initialized:");
        logger.info("  SMTP Host: {}", mailHost);
        logger.info("  SMTP Port: {}", mailPort);
        logger.info("  SMTP Username: {}", mailUsername);
        logger.info("  SMTP Auth: {}", mailAuth);
        logger.info("  STARTTLS Enabled: {}", startTlsEnable);
        logger.info("  STARTTLS Required: {}", startTlsRequired);
        logger.info("  Connection Timeout: {}ms", connectionTimeout);
        logger.info("  Read Timeout: {}ms", timeout);
        logger.info("  Write Timeout: {}ms", writeTimeout);

        return mailSender;
    }
}

