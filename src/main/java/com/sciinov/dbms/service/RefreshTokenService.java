package com.sciinov.dbms.service;

import com.sciinov.dbms.entity.RefreshToken;
import com.sciinov.dbms.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private int refreshTokenExpirationDays;

    public String generateRefreshToken(String userId) {
        logger.debug("Generating refresh token for user: {}", userId);
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(userId);
        if (existingToken.isPresent()) {
            RefreshToken token = existingToken.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
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
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(userId);
        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            logger.info("All refresh tokens revoked for user: {}", userId);
        }
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

