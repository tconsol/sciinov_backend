package com.sciinov.dbms.config;

import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.service.DocumentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   DocumentTypeService documentTypeService,
                                   MongoTemplate mongoTemplate) {
        return args -> {
           try {
                // ── Ensure MongoDB indexes ────────────────────────────────
                ensureIndexes(mongoTemplate);

                // ── Normalize existing emails (one-time migration) ────────
                normalizeExistingEmails(mongoTemplate);

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

    /**
     * Create all required MongoDB indexes programmatically.
     * This runs on every startup (ensureIndex is idempotent — it's a no-op if the index already exists).
     *
     * We do this because spring.data.mongodb.auto-index-creation=false,
     * so @CompoundIndex / @Indexed annotations on entities are NOT auto-created.
     */
    private void ensureIndexes(MongoTemplate mongoTemplate) {
        logger.info("🔧 Ensuring MongoDB indexes...");

        try {
            // ── dashboard_data collection ──
            // 1) Unique compound index: (conferenceId, dashboardMasterId, email) with case-insensitive collation
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("email", Sort.Direction.ASC)
                            .unique()
                            .named("conf_dash_email_ci_idx")
                            .collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()))
            );

            // 2) Index for serial number queries
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("serialNo", Sort.Direction.ASC)
                            .named("conf_dash_serial_idx")
            );

            // 3) Index for date range queries
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("createdAt", Sort.Direction.ASC)
                            .named("conf_dash_created_idx")
            );

            // 4) Index for deleted flag filter (used in almost every query)
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("deleted", Sort.Direction.ASC)
                            .named("conf_dash_deleted_idx")
            );

            // 5) Index for email domain regex queries (used in by-domain-extension)
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("deleted", Sort.Direction.ASC)
                            .on("email", Sort.Direction.ASC)
                            .named("conf_dash_del_email_idx")
            );

            // ── users collection ──
            mongoTemplate.indexOps("users").ensureIndex(
                    new Index().on("userId", Sort.Direction.ASC).unique().named("userId_unique_idx")
            );
            mongoTemplate.indexOps("users").ensureIndex(
                    new Index().on("role", Sort.Direction.ASC).on("deleted", Sort.Direction.ASC).named("role_deleted_idx")
            );

            logger.info("✅ MongoDB indexes ensured successfully");
        } catch (Exception e) {
            logger.warn("⚠️ Could not ensure some indexes (may already exist with different options): {}", e.getMessage());
        }
    }

    /**
     * One-time migration: Normalize all emails in dashboard_data to lowercase + trimmed.
     * This ensures the case-insensitive unique index works correctly and
     * duplicate detection is accurate for data uploaded before normalization was added.
     */
    private void normalizeExistingEmails(MongoTemplate mongoTemplate) {
        try {
            // Find documents where email contains uppercase letters
            Query query = new Query(Criteria.where("email").regex("[A-Z]"));
            long count = mongoTemplate.count(query, "dashboard_data");

            if (count > 0) {
                logger.info("🔧 Found {} emails with uppercase letters — normalizing to lowercase...", count);

                // Use MongoDB aggregation pipeline update to lowercase in-place
                // This is more efficient than loading all docs into Java
                List<org.bson.Document> docs = mongoTemplate.findDistinct(
                        query, "email", "dashboard_data", String.class)
                        .stream()
                        .filter(e -> e != null && !e.equals(e.toLowerCase().trim()))
                        .map(e -> new org.bson.Document("original", e).append("normalized", e.toLowerCase().trim()))
                        .toList();

                int updated = 0;
                for (org.bson.Document doc : docs) {
                    String original = doc.getString("original");
                    String normalized = doc.getString("normalized");
                    try {
                        long modified = mongoTemplate.updateMulti(
                                new Query(Criteria.where("email").is(original)),
                                Update.update("email", normalized),
                                "dashboard_data"
                        ).getModifiedCount();
                        updated += (int) modified;
                    } catch (Exception ex) {
                        // May fail due to duplicate key if both "John@Gmail.com" and "john@gmail.com" exist
                        logger.warn("⚠️ Could not normalize email '{}' → '{}' (duplicate may exist): {}",
                                original, normalized, ex.getMessage());
                    }
                }
                logger.info("✅ Normalized {} email records to lowercase", updated);
            } else {
                logger.info("✅ All emails already normalized");
            }
        } catch (Exception e) {
            logger.warn("⚠️ Email normalization check skipped: {}", e.getMessage());
        }
    }
}
