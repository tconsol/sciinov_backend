package com.sciinov.dbms.config;

import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.service.DocumentTypeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class MongoConfig {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   DocumentTypeService documentTypeService) {
        return args -> {
           try {
                // ── Seed default super admin ──────────────────────────────
                if (!userRepository.existsByRoleAndDeletedFalse(User.Role.SUPER_ADMIN)) {
                    User superAdmin = new User();
                    superAdmin.setFirstName("Super");
                    superAdmin.setLastName("Admin");
                    superAdmin.setUserId("superadmin");
                    superAdmin.setEmail("superadmin@example.com");
                    superAdmin.setPhoneNumber("0000000000");
                    superAdmin.setPassword(passwordEncoder.encode("admin123"));
                    superAdmin.setRole(User.Role.SUPER_ADMIN);
                    superAdmin.setStatus(true);
                    superAdmin.setCreatedAt(LocalDateTime.now());
                    superAdmin.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(superAdmin);
                    System.out.println("✅ Default Super Admin created: superadmin / admin123");
                }

                // ── Seed default document types ───────────────────────────
                documentTypeService.seedDefaultTypes();
            } catch (Exception e) {
                System.err.println("⚠️  Warning: Could not initialize database on startup: " + e.getMessage());
                System.err.println("⚠️  The application will continue, but initial data may not be seeded.");
                System.err.println("⚠️  Please check MongoDB connection and retry initialization.");
                // Don't throw - allow application to start even if DB init fails
            }
        };
    }
}
