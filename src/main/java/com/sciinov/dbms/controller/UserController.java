package com.sciinov.dbms.controller;

import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<User> getAllAdmins() {
        logger.info("GET /api/users/admins - Retrieving all admin users");
        List<User> admins = userService.getAllAdmins();
        logger.info("GET /api/users/admins - Retrieved {} admin users", admins.size());
        return admins;
    }
    
    @GetMapping("/super-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<User> getAllSuperAdmins() {
        logger.info("GET /api/users/super-admins - Retrieving all super admin users");
        List<User> superAdmins = userService.getAllSuperAdmins();
        logger.info("GET /api/users/super-admins - Retrieved {} super admin users", superAdmins.size());
        return superAdmins;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        logger.info("GET /api/users/{} - Retrieving user by ID", id);
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("GET /api/users/{} - User found: {}", id, user.getUserId());
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> {
                    logger.warn("GET /api/users/{} - User not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public User createUser(@RequestBody User user) {
        logger.info("POST /api/users - Creating new user: {}", user.getUserId());
        User created = userService.createUser(user);
        logger.info("POST /api/users - User created successfully: {} with role: {}", created.getUserId(), created.getRole());
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User user) {
        logger.info("PUT /api/users/{} - Updating user: {}", id, user.getUserId());
        User updated = userService.updateUser(id, user);
        logger.info("PUT /api/users/{} - User updated successfully", id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        logger.info("DELETE /api/users/{} - Deleting user", id);
        userService.deleteUser(id);
        logger.info("DELETE /api/users/{} - User deleted successfully", id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<User> updateUserStatus(@PathVariable String id, @RequestBody Map<String, Boolean> status) {
        logger.info("PATCH /api/users/{}/status - Updating user status", id);
        boolean newStatus = status.get("status");
        User updated = userService.updateUserStatus(id, newStatus);
        logger.info("PATCH /api/users/{}/status - User status updated successfully to {}", id, newStatus);
        return ResponseEntity.ok(updated);
    }
}
