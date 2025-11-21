package com.polo42.project_flow.repository;

import com.polo42.project_flow.model.PasswordResetToken;
import com.polo42.project_flow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    void deleteAllByUser(User user);
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
}
