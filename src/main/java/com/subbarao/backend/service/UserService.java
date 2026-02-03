package com.subbarao.backend.service;

import com.subbarao.backend.entity.User;
import com.subbarao.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        if (userRepository.findByEmailAndDeletedFalse(user.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Email is already in use!");
        }
        if (userRepository.findByPhoneNumberAndDeletedFalse(user.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Error: Phone number is already in use!");
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
        user.setPhoneNumber(userDetails.getPhoneNumber());
        user.setEmail(userDetails.getEmail());
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
}
