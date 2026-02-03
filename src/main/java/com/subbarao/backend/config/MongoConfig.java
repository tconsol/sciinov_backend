package com.subbarao.backend.config;

import com.subbarao.backend.entity.User;
import com.subbarao.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class MongoConfig {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
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
                System.out.println("Default Super Admin created: superadmin / admin123");
            }
        };
    }
}
