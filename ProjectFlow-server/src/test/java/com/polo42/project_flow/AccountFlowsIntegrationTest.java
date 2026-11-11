package com.polo42.project_flow;

import com.polo42.project_flow.model.EmailVerificationToken;
import com.polo42.project_flow.model.PasswordResetToken;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.EmailVerificationTokenRepository;
import com.polo42.project_flow.repository.PasswordResetTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxos de conta: verificacao de e-mail, redefinicao de senha, login e validade dos JWTs.
 */
class AccountFlowsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmailVerificationTokenRepository verificationRepository;
    @Autowired
    private PasswordResetTokenRepository resetRepository;
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // ---------- helpers ----------

    private User registerUnverified(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Pessoa Teste", "email", email, "senha", PASSWORD))))
                .andExpect(status().isOk());
        return userRepository.findByEmail(email).orElseThrow();
    }

    private EmailVerificationToken verificationTokenOf(User user) {
        return verificationRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .findFirst().orElseThrow();
    }

    private ResultActions verify(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/resend-verification").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))));
    }

    private PasswordResetToken newResetToken(User user, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(expiresAt);
        return resetRepository.save(token);
    }

    private ResultActions reset(String token, String novaSenha) throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "novaSenha", novaSenha))));
    }

    private int loginStatus(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andReturn().getResponse().getStatus();
    }

    // ---------- login ----------

    @Test
    void loginSemEmailVerificadoRetorna422() throws Exception {
        User user = registerUnverified("naoverificado");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "senha", PASSWORD))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Confirme seu email antes de acessar."));
    }

    // ---------- verificacao de e-mail ----------

    @Test
    void verificacaoValidaLiberaOLogin() throws Exception {
        User user = registerUnverified("verifica");
        verify(verificationTokenOf(user).getToken()).andExpect(status().isOk());
        assertEquals(200, loginStatus(user.getEmail(), PASSWORD));
    }

    @Test
    void tokenDeVerificacaoReutilizadoEhRecusado() throws Exception {
        User user = registerUnverified("reuso");
        String token = verificationTokenOf(user).getToken();
        verify(token).andExpect(status().isOk());
        verify(token).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void tokenDeVerificacaoExpiradoEhRecusado() throws Exception {
        User user = registerUnverified("expirado");
        EmailVerificationToken token = verificationTokenOf(user);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        verificationRepository.save(token);

        verify(token.getToken()).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").isNotEmpty());
        assertFalse(userRepository.findById(user.getId()).orElseThrow().isEmailVerificado());
    }

    @Test
    void tokenDeVerificacaoInexistenteEhRecusado() throws Exception {
        verify(UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity());
    }

    // ---------- reenvio de verificacao ----------

    @Test
    void reenvioGeraNovoTokenParaContaNaoVerificada() throws Exception {
        User user = registerUnverified("reenvio");
        String antigo = verificationTokenOf(user).getToken();

        resend(user.getEmail()).andExpect(status().isOk());

        String novo = verificationTokenOf(user).getToken();
        assertFalse(antigo.equals(novo), "o reenvio deve substituir o token");
        verify(antigo).andExpect(status().isUnprocessableEntity());
        verify(novo).andExpect(status().isOk());
    }

    @Test
    void reenvioRespondeIgualParaContaVerificadaEEmailInexistente() throws Exception {
        TestUser verified = createUser("Verificada");

        String inexistente = resend("ninguem-" + UUID.randomUUID() + "@example.test")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String verificada = resend(verified.email())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(inexistente, verificada);
    }

    // ---------- redefinicao de senha ----------

    @Test
    void tokenDeResetReutilizadoEhRecusado() throws Exception {
        TestUser user = createUser("ResetReuso");
        PasswordResetToken token = newResetToken(userRepository.findById(user.id()).orElseThrow(),
                LocalDateTime.now().plusMinutes(10));

        reset(token.getToken(), "primeira-senha-1").andExpect(status().isOk());
        reset(token.getToken(), "segunda-senha-2").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").isNotEmpty());
        assertEquals(200, loginStatus(user.email(), "primeira-senha-1"));
    }

    @Test
    void tokenDeResetExpiradoEhRecusado() throws Exception {
        TestUser user = createUser("ResetExpirado");
        PasswordResetToken token = newResetToken(userRepository.findById(user.id()).orElseThrow(),
                LocalDateTime.now().minusMinutes(1));

        reset(token.getToken(), "nova-senha-123").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").isNotEmpty());
        assertEquals(200, loginStatus(user.email(), PASSWORD));
    }

    // ---------- JWT ----------

    private String jwtSignedWith(String secret, UUID subject, Date expiration) {
        return Jwts.builder()
                .setSubject(subject.toString())
                .setIssuedAt(new Date(System.currentTimeMillis() - 60_000))
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();
    }

    @Test
    void jwtAssinadoComOutraChaveEhRecusado() throws Exception {
        TestUser user = createUser("Forjado");
        String forged = jwtSignedWith("outra-chave-totalmente-diferente-da-do-servidor-com-mais-de-64-caracteres-0000",
                user.id(), new Date(System.currentTimeMillis() + 3_600_000));

        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void jwtExpiradoEhRecusado() throws Exception {
        TestUser user = createUser("Expirado");
        String expired = jwtSignedWith(jwtSecret, user.id(), new Date(System.currentTimeMillis() - 1_000));

        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        // sanidade: o mesmo token ainda valido seria aceito
        String valid = jwtSignedWith(jwtSecret, user.id(), new Date(System.currentTimeMillis() + 3_600_000));
        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk());
    }
}
