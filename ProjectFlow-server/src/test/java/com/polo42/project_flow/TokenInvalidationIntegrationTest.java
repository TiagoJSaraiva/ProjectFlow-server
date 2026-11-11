package com.polo42.project_flow;

import com.polo42.project_flow.model.PasswordResetToken;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWTs emitidos antes de uma troca ou redefinicao de senha deixam de valer.
 */
class TokenInvalidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Test
    void tokenAntigoDeixaDeValerAposTrocaDeSenha() throws Exception {
        TestUser user = createUser("Troca");

        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/users/" + user.id() + "/password")
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("senha-nova-456"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String novoToken = login(user.email(), "senha-nova-456");
        assertFalse(novoToken.isBlank());
        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + novoToken))
                .andExpect(status().isOk());
    }

    @Test
    void tokenAntigoDeixaDeValerAposRedefinicaoPorEmail() throws Exception {
        TestUser user = createUser("Reset");

        User entity = userRepository.findById(user.id()).orElseThrow();
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(entity);
        reset.setToken(UUID.randomUUID().toString());
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetTokenRepository.save(reset);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", reset.getToken(), "novaSenha", "senha-redefinida-789"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(status().isUnauthorized());

        String novoToken = login(user.email(), "senha-redefinida-789");
        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", "Bearer " + novoToken))
                .andExpect(status().isOk());
    }
}
