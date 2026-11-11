package com.polo42.project_flow.service;

import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.model.PasswordResetToken;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.PasswordResetTokenRepository;
import com.polo42.project_flow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final String frontendBaseUrl;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            MailService mailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.base-url:https://projectflow.app}") String frontendBaseUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void sendRecoveryEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            LOGGER.warn("Password recovery requested for non-existing email: {}", email);
            return;
        }

        User user = userOptional.get();
        tokenRepository.deleteAllByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
        token.setUser(user);

        tokenRepository.save(token);

        try {
            sendEmail(user, token);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while sending recovery email", exception);
            throw new BusinessException("Não foi possível enviar o email de recuperação. Tente novamente.");
        }
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenAndUsedFalse(tokenValue)
                .orElseThrow(() -> new BusinessException("Código de recuperação inválido ou já utilizado."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Código de recuperação expirado.");
        }

        User user = token.getUser();
        user.setSenha(passwordEncoder.encode(newPassword));
        user.setSenhaAlteradaEm(Instant.now());
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
        tokenRepository.deleteAllByUser(user);
    }

    private void sendEmail(User user, PasswordResetToken token) {
        String resetLink = String.format("%s/reset?token=%s", frontendBaseUrl, token.getToken());
        String messageBody = "Olá, " + user.getNome() + "!\n\n" +
                "Recebemos um pedido para redefinir sua senha no ProjectFlow.\n" +
                "Use o código abaixo ou clique no link dentro de " + TOKEN_VALIDITY_MINUTES + " minutos:\n\n" +
                token.getToken() + "\n\n" +
                "Link direto: " + resetLink + "\n\n" +
                "Se você não solicitou, ignore este email.";

        try {
            mailService.send(user.getEmail(), "Recuperação de senha - ProjectFlow", messageBody);
        } catch (MailException exception) {
            LOGGER.error("Failed to send password recovery email", exception);
            throw new BusinessException("Não foi possível enviar o email de recuperação. Verifique o endereço e tente novamente.");
        }
    }
}
