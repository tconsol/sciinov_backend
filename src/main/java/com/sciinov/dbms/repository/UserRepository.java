package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserIdAndDeletedFalse(String userId);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByPhoneNumberAndDeletedFalse(String phoneNumber);
    List<User> findByRoleAndDeletedFalse(User.Role role);
    Optional<User> findByIdAndDeletedFalse(String id);
    boolean existsByRoleAndDeletedFalse(User.Role role);
}
