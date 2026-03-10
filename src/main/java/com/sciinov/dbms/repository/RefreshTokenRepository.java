package com.sciinov.dbms.repository;

import com.sciinov.dbms.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(String userId);

    /** Find ALL refresh tokens for a user (supports multi-device sessions) */
    List<RefreshToken> findAllByUserId(String userId);

    /** Find all active (non-revoked) tokens for a user */
    List<RefreshToken> findAllByUserIdAndRevokedFalse(String userId);

    long deleteByUserId(String userId);

    long deleteByToken(String token);
}

