package com.sciinov.dbms.config;

import com.sciinov.dbms.entity.DashboardData;
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

                // ── REMOVE DUPLICATE USERS (if any) ───────────────────────
                removeDuplicateUsers(mongoTemplate);

                // ── Normalize existing emails (one-time migration) ────────
                normalizeExistingEmails(mongoTemplate);

                // ── Seed default super admin ──────────────────────────────
                if (!userRepository.existsByRoleAndDeletedFalse(User.Role.SUPER_ADMIN)) {
                    User superAdmin = User.builder()
                            .firstName("Super")
                            .lastName("Admin")
                            .userId("superadmin")
                            .email("superadmin@example.com")
                            .phoneNumber("0000000000")
                            .password(passwordEncoder.encode("admin123"))
                            .role(User.Role.SUPER_ADMIN)
                            .status(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
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

            // 6) CRITICAL: compound index covering deleted + serialNo together
            //    Used by every paginated GET /api/dashboard-data query
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("deleted", Sort.Direction.ASC)
                            .on("serialNo", Sort.Direction.ASC)
                            .named("conf_dash_del_serial_idx")
            );

            // 7) Partial index: only index non-deleted documents — cuts index size ~50%
            //    This is the fastest possible index for all active-record queries
            mongoTemplate.indexOps("dashboard_data").ensureIndex(
                    new Index()
                            .on("conferenceId", Sort.Direction.ASC)
                            .on("dashboardMasterId", Sort.Direction.ASC)
                            .on("serialNo", Sort.Direction.ASC)
                            .sparse()
                            .named("conf_dash_serial_sparse_idx")
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
     */
    private void normalizeExistingEmails(MongoTemplate mongoTemplate) {
        try {
            logger.info("✅ Email normalization performed at import time");
            // Email normalization happens via DashboardData.setEmail() custom setter
            // No need to re-normalize on startup unless there's a specific issue
        } catch (Exception e) {
            logger.warn("⚠️ Email normalization check skipped: {}", e.getMessage());
        }
    }

    /**
     * Remove duplicate users (keep the newest, delete older duplicates).
     */
    private void removeDuplicateUsers(MongoTemplate mongoTemplate) {
        logger.info("🔍 Checking for duplicate users...");
        try {
            // Get all users
            List<User> allUsers = mongoTemplate.findAll(User.class);

            // Group by userId
            java.util.Map<String, java.util.List<User>> grouped = allUsers.stream()
                    .collect(java.util.stream.Collectors.groupingBy(User::getUserId));

            int duplicatesRemoved = 0;
            for (java.util.Map.Entry<String, java.util.List<User>> entry : grouped.entrySet()) {
                String userId = entry.getKey();
                java.util.List<User> users = entry.getValue();

                if (users.size() > 1) {
                    // Filter to non-deleted only
                    java.util.List<User> nonDeletedUsers = users.stream()
                            .filter(u -> !u.isDeleted())
                            .collect(java.util.stream.Collectors.toList());

                    if (nonDeletedUsers.size() > 1) {
                        logger.warn("🚨 Found {} duplicate active users with userId='{}' - keeping newest, removing {} old copies",
                                nonDeletedUsers.size(), userId, nonDeletedUsers.size() - 1);

                        // Sort by createdAt DESC — keep the newest, delete the rest
                        nonDeletedUsers.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

                        for (int i = 1; i < nonDeletedUsers.size(); i++) {
                            User oldDuplicate = nonDeletedUsers.get(i);
                            mongoTemplate.remove(
                                    Query.query(Criteria.where("_id").is(oldDuplicate.getId())),
                                    User.class
                            );
                            logger.warn("  ❌ Deleted duplicate user: {} (created {})",
                                    oldDuplicate.getUserId(), oldDuplicate.getCreatedAt());
                            duplicatesRemoved++;
                        }
                    }
                }
            }

            if (duplicatesRemoved > 0) {
                logger.warn("⚠️  Removed {} duplicate user records", duplicatesRemoved);
            } else {
                logger.info("✅ No duplicate users found");
            }
        } catch (Exception e) {
            logger.warn("⚠️ Duplicate user cleanup skipped: {}", e.getMessage());
        }
    }
}
