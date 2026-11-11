package com.polo42.project_flow.service;

import com.polo42.project_flow.exception.BusinessException;
import com.polo42.project_flow.model.EmailVerificationToken;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.EmailVerificationTokenRepository;
import com.polo42.project_flow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int TOKEN_VALIDITY_MINUTES = 60;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final String verificationBaseUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            MailService mailService,
            @Value("${app.frontend.base-url:https://projectflow.app}") String verificationBaseUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.verificationBaseUrl = verificationBaseUrl;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        try {
            tokenRepository.deleteAllByUser(user);

            EmailVerificationToken token = new EmailVerificationToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
            tokenRepository.save(token);

            deliverEmail(user, token);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.error("Unexpected error while generating verification token", exception);
            throw new BusinessException("Não foi possível enviar o email de confirmação. Tente novamente mais tarde.");
        }
    }

    @Transactional
    public void verifyToken(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByTokenAndUsedFalse(tokenValue)
                .orElseThrow(() -> new BusinessException("Token de verificação inválido."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Token de verificação expirado.");
        }

        User user = token.getUser();
        user.setEmailVerificado(true);
        user.setDataVerificado(LocalDateTime.now());
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            LOGGER.warn("Tentativa de reenvio para email inexistente");
            return;
        }

        User user = userOptional.get();
        if (user.isEmailVerificado()) {
            // Resposta identica a de e-mail inexistente, para nao revelar o estado da conta
            LOGGER.info("Reenvio de verificação ignorado: conta já verificada");
            return;
        }

        sendVerificationEmail(user);
    }

    private void deliverEmail(User user, EmailVerificationToken token) {
        String link = String.format("%s/verify-email?token=%s", verificationBaseUrl, token.getToken());

        String body = "Olá, " + user.getNome() + "!\n\n" +
                "Bem-vindo ao ProjectFlow. Precisamos confirmar que este email é seu." +
                " Clique no link abaixo ou cole-o no navegador dentro de " + TOKEN_VALIDITY_MINUTES + " minutos:\n\n" +
                link + "\n\n" +
                "Se você não criou uma conta, ignore esta mensagem.";

        try {
            mailService.send(user.getEmail(), "Confirme seu email - ProjectFlow", body);
        } catch (MailException exception) {
            LOGGER.error("Falha ao enviar email de verificação", exception);
            throw new BusinessException("Não foi possível enviar o email de confirmação. Tente novamente.");
        }
    }
}
