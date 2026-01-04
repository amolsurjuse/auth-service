package com.amy.auth.service;

import com.amy.auth.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrincipalLookupService {

    public record CachedPrincipal(String email, String passwordHash, boolean enabled, List<String> roles) {}

    private final UserRepository userRepository;

    public PrincipalLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Cacheable(cacheNames = "userDetailsByEmail", key = "#email")
    public CachedPrincipal loadByEmail(String email) {
        var user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var roles = user.getRoles().stream().map(r -> r.getName()).toList();
        return new CachedPrincipal(user.getEmail(), user.getPasswordHash(), user.isEnabled(), roles);
    }
}

