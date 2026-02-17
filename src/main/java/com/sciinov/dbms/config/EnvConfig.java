package com.sciinov.dbms.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    static {
        loadEnv();
    }

    private static void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

            // Set environment variables from .env file
            dotenv.entries().forEach(entry -> {
                if (System.getenv(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });

            logger.info("Environment variables loaded from .env file successfully");
        } catch (Exception e) {
            logger.warn("Could not load .env file: {}. Using system environment variables or defaults.", e.getMessage());
        }
    }
}

