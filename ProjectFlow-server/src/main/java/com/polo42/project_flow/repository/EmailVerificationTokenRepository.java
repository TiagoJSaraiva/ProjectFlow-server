package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.EmailVerificationToken;
import com.polo42.project_flow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);
    void deleteAllByUser(User user);
}
