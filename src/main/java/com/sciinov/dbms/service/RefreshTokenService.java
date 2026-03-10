package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.RefreshToken;
import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.RefreshTokenRepository;
import com.sciinov.dbms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshTokenExpirationDays;

    /**
     * Generate a new refresh token for a user.
     * If the user has an existing active token, revoke it.
     *
     * DEFENSIVE: Use MongoDB query instead of findByUserId to handle duplicate users gracefully.
     */
    public String generateRefreshToken(String userId) {
        logger.debug("Generating refresh token for user: {}", userId);

        try {
            // Verify user exists and is not deleted (graceful fallback)
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.isDeleted() || !user.isStatus()) {
                logger.warn("Cannot generate refresh token: user {} is null/deleted/inactive", userId);
                throw new IllegalArgumentException("User not found or inactive");
            }
        } catch (Exception e) {
            logger.error("Error fetching user {}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("Invalid user");
        }

        // Revoke existing token (if any)
        try {
            List<RefreshToken> existingTokens = mongoTemplate.find(
                    Query.query(Criteria.where("userId").is(userId).and("revoked").is(false)),
                    RefreshToken.class
            );
            for (RefreshToken token : existingTokens) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                logger.debug("Revoked existing refresh token for user: {}", userId);
            }
        } catch (Exception e) {
            logger.warn("Could not revoke existing tokens for user {}: {}", userId, e.getMessage());
            // Continue anyway — generate new token
        }

        // Generate new token
        String tokenValue = generateRandomToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpirationDays);
        RefreshToken refreshToken = new RefreshToken(tokenValue, userId, expiryDate);
        refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token generated for user: {} with expiry: {}", userId, expiryDate);
        return tokenValue;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
            if (refreshToken.isEmpty()) {
                logger.warn("Refresh token not found");
                return false;
            }
            RefreshToken rt = refreshToken.get();
            if (rt.isRevoked()) {
                logger.warn("Refresh token has been revoked");
                return false;
            }
            if (rt.isExpired()) {
                logger.warn("Refresh token has expired");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error validating refresh token: {}", e.getMessage());
            return false;
        }
    }

    public Optional<String> getUserIdFromRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isRevoked() && !rt.isExpired())
                .map(RefreshToken::getUserId);
    }

    public void revokeRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            logger.info("Refresh token revoked");
        }
    }

    public void revokeAllTokensForUser(String userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        int revokedCount = 0;
        for (RefreshToken rt : activeTokens) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            revokedCount++;
        }
        logger.info("All refresh tokens revoked for user: {} (count: {})", userId, revokedCount);
    }

    private String generateRandomToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public int getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }
}

