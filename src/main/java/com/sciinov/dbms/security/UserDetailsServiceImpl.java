package com.sciinov.dbms.security;

import com.sciinov.dbms.entity.User;
import com.sciinov.dbms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        logger.info("Loading user details for userId: {}", userId);
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.warn("User Not Found with userId: {}", userId);
                    return new UsernameNotFoundException("User Not Found with userId: " + userId);
                });

        logger.info("User loaded successfully: {} with role: {}", user.getUserId(), user.getRole());
        return UserDetailsImpl.build(user);
    }
}
