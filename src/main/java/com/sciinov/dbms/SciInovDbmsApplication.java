package com.sciinov.dbms;

 import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
public class SciInovDbmsApplication {

    static {
        // Load .env file before Spring Boot initializes
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Set system properties from .env file
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });

            System.out.println("✅ Environment variables loaded from .env file successfully");
        } catch (Exception e) {
            System.err.println("⚠️  Could not load .env file: " + e.getMessage());
            System.err.println("⚠️  Using system environment variables or defaults");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SciInovDbmsApplication.class, args);
    }

}
