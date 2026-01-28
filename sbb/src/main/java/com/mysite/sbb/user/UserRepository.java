package com.mysite.sbb.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<SiteUser, Long> {
    Optional<SiteUser> findByusername(String username);
    Optional<SiteUser> findByProviderAndProviderId(String provider, String providerId);
    Optional<SiteUser> findByEmail(String email);
    
}
