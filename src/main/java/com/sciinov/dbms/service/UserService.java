package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.UserRepository;
import com.sciinov.dbms.dto.AdminConferenceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllAdmins() {
        return userRepository.findByRoleAndDeletedFalse(User.Role.ADMIN);
    }
    
    public List<User> getAllSuperAdmins() {
        return userRepository.findByRoleAndDeletedFalse(User.Role.SUPER_ADMIN);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findByIdAndDeletedFalse(id);
    }

    public User createUser(User user) {
        if (userRepository.findByUserIdAndDeletedFalse(user.getUserId()).isPresent()) {
            throw new RuntimeException("Error: UserId is already taken!");
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            if (userRepository.findByEmailAndDeletedFalse(user.getEmail()).isPresent()) {
                throw new RuntimeException("Error: Email is already in use!");
            }
        } else {
            user.setEmail(null);
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            if (userRepository.findByPhoneNumberAndDeletedFalse(user.getPhoneNumber()).isPresent()) {
                throw new RuntimeException("Error: Phone number is already in use!");
            }
        } else {
            user.setPhoneNumber(null);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User updateUser(String id, User userDetails) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());

        // Email is optional — only update/validate if provided
        String newEmail = (userDetails.getEmail() != null && !userDetails.getEmail().isBlank())
                ? userDetails.getEmail() : null;
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmailAndDeletedFalse(newEmail).isPresent()) {
                throw new RuntimeException("Error: Email is already in use!");
            }
        }
        user.setEmail(newEmail);

        // Phone is optional — only update/validate if provided
        String newPhone = (userDetails.getPhoneNumber() != null && !userDetails.getPhoneNumber().isBlank())
                ? userDetails.getPhoneNumber() : null;
        if (newPhone != null && !newPhone.equals(user.getPhoneNumber())) {
            if (userRepository.findByPhoneNumberAndDeletedFalse(newPhone).isPresent()) {
                throw new RuntimeException("Error: Phone number is already in use!");
            }
        }
        user.setPhoneNumber(newPhone);

        user.setStatus(userDetails.isStatus());
        
        if (userDetails.getConferenceIds() != null) {
            user.setConferenceIds(userDetails.getConferenceIds());
        }
        
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == User.Role.SUPER_ADMIN) {
             List<User> superAdmins = userRepository.findByRoleAndDeletedFalse(User.Role.SUPER_ADMIN);
             if (superAdmins.size() <= 1) {
                 throw new RuntimeException("Cannot delete the last Super Admin");
             }
        }

        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public User updateUserStatus(String id, boolean status) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Get all conferences assigned to an admin
     */
    public List<String> getAdminConferences(String adminId) {
        User admin = userRepository.findByIdAndDeletedFalse(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        return admin.getConferenceIds() != null ? admin.getConferenceIds() : new ArrayList<>();
    }

    /**
     * Assign a conference to an admin
     */
    public User assignConferenceToAdmin(String adminId, String conferenceId) {
        User admin = userRepository.findByIdAndDeletedFalse(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        if (admin.getConferenceIds() == null) {
            admin.setConferenceIds(new ArrayList<>());
        }

        if (!admin.getConferenceIds().contains(conferenceId)) {
            admin.getConferenceIds().add(conferenceId);
            admin.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(admin);
        }

        return admin; // Already assigned
    }

    /**
     * Remove a conference from an admin
     */
    public User removeConferenceFromAdmin(String adminId, String conferenceId) {
        User admin = userRepository.findByIdAndDeletedFalse(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        if (admin.getConferenceIds() != null && admin.getConferenceIds().contains(conferenceId)) {
            admin.getConferenceIds().remove(conferenceId);
            admin.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(admin);
        }

        throw new RuntimeException("Conference not assigned to this admin");
    }

    /**
     * Get all admins with their assigned conferences
     */
    public List<AdminConferenceResponse> getAllAdminsWithConferences() {
        List<User> admins = getAllAdmins();
        List<AdminConferenceResponse> result = new ArrayList<>();

        for (User admin : admins) {
            AdminConferenceResponse response = new AdminConferenceResponse(
                    admin.getId(),
                    admin.getUserId(),
                    admin.getFirstName(),
                    admin.getLastName(),
                    admin.getEmail(),
                    admin.getPhoneNumber(),
                    admin.isStatus(),
                    admin.getConferenceIds() != null ? admin.getConferenceIds() : new ArrayList<>()
            );
            result.add(response);
        }

        return result;
    }
}
