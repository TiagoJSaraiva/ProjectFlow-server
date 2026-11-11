package com.polo42.project_flow;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validacao de PUT /api/users/{id} e PUT /api/users/{id}/password.
 */
class UserUpdateIntegrationTest extends AbstractIntegrationTest {

    private void updateUser(TestUser user, String json, int expectedStatus) throws Exception {
        mockMvc.perform(put("/api/users/" + user.id()).header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus))
                .andExpect(expectedStatus >= 400 ? jsonPath("$.message").isNotEmpty() : jsonPath("$.id").isNotEmpty());
    }

    private void changePassword(TestUser user, String json, int expectedStatus) throws Exception {
        mockMvc.perform(put("/api/users/" + user.id() + "/password").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void nomeAusenteOuInvalidoRetorna400() throws Exception {
        TestUser user = createUser("Nome");
        updateUser(user, "{}", 400);
        updateUser(user, "{\"nome\":null,\"email\":\"" + user.email() + "\"}", 400);
        updateUser(user, "{\"nome\":\"x\"}", 400);
    }

    @Test
    void emailNaoPodeSerAlterado() throws Exception {
        TestUser user = createUser("Email");
        updateUser(user, "{\"nome\":\"Novo Nome\",\"email\":\"outro@example.test\"}", 422);
        assertEquals(user.email(), userRepository.findById(user.id()).orElseThrow().getEmail());
    }

    @Test
    void alterarSoONomeOuEnviarOMesmoEmailFunciona() throws Exception {
        TestUser user = createUser("Ok");
        updateUser(user, "{\"nome\":\"Nome Atualizado\"}", 200);
        updateUser(user, "{\"nome\":\"Outro Nome\",\"email\":\"" + user.email().toUpperCase() + "\"}", 200);
        assertEquals("Outro Nome", userRepository.findById(user.id()).orElseThrow().getNome());
    }

    @Test
    void trocaDeSenhaExigeSenhaAtualENovaSenhaValida() throws Exception {
        TestUser user = createUser("Senha");

        changePassword(user, "{}", 400);
        changePassword(user, "{\"novaSenha\":\"nova-senha-123\"}", 400);
        changePassword(user, "{\"senhaAtual\":\"" + PASSWORD + "\"}", 400);
        changePassword(user, "{\"senhaAtual\":\"" + PASSWORD + "\",\"novaSenha\":\"\"}", 400);
        changePassword(user, "{\"senhaAtual\":\"" + PASSWORD + "\",\"novaSenha\":\"123\"}", 400);
        // senha atual errada
        mockMvc.perform(put("/api/users/" + user.id() + "/password").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada-errada\",\"novaSenha\":\"nova-senha-123\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").isNotEmpty());
        // nada mudou
        assertFalse(login(user.email(), PASSWORD).isBlank());

        changePassword(user, "{\"senhaAtual\":\"" + PASSWORD + "\",\"novaSenha\":\"nova-senha-123\"}", 204);
        assertFalse(login(user.email(), "nova-senha-123").isBlank());
    }
}
