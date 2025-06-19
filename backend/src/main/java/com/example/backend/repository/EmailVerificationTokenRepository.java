package com.example.backend.repository;

import com.example.backend.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    
    @Query("SELECT COUNT(t) FROM EmailVerificationToken t")
    long countTokens();
    
    @Query("SELECT t FROM EmailVerificationToken t")
    List<EmailVerificationToken> findAllTokens();
}
