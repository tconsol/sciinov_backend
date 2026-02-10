package com.subbarao.backend.security;

import com.subbarao.backend.entity.User;
import com.subbarao.backend.repository.UserRepository;
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
        logger.debug("Loading user by userId: {}", userId);
        User user = userRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> {
                    logger.error("User Not Found with userId: {}", userId);
                    return new UsernameNotFoundException("User Not Found with userId: " + userId);
                });

        logger.debug("User found: {}", user.getUserId());
        return UserDetailsImpl.build(user);
    }
}
