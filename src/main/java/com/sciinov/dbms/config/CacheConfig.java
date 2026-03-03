package com.sciinov.dbms.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine-based in-memory cache configuration.
 *
 * Caches:
 *  - userDetails      : User auth objects — cached 10 min, max 500 entries
 *                       Eliminates DB hit on EVERY API request (biggest win)
 *  - domainExtensions : TLD list per conference — cached 5 min, max 200 entries
 *                       Avoids full email scan on every page load
 *  - dataCount        : Record count per conference — cached 30 sec, max 200 entries
 *                       Avoids count query on every dashboard refresh
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Default spec for all caches not explicitly configured
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000));

        return manager;
    }

    /** User details — cache 10 minutes, evicted on password change / user update */
    @Bean("userDetailsCache")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> userDetailsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /** Domain extensions per conference — cache 5 minutes */
    @Bean("domainExtensionsCache")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> domainExtensionsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(200)
                .build();
    }

    /** Record counts — cache 30 seconds to reduce count queries on dashboard refresh */
    @Bean("dataCountCache")
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> dataCountCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(200)
                .build();
    }
}

