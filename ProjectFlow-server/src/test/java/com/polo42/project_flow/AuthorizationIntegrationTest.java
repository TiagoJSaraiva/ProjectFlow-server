package com.polo42.project_flow;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorizacao por dono: recurso de outro usuario responde sempre 404 e nunca e alterado.
 */
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    private TestUser userA;
    private TestUser userB;
    private UUID projectA;
    private UUID nodeA;
    private UUID elementA;

    @BeforeEach
    void setUp() throws Exception {
        userA = createUser("Alice");
        userB = createUser("Bruno");

        String created = mockMvc.perform(post("/api/projects/user/" + userA.id())
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Projeto da Alice\",\"descricao\":\"privado\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        projectA = UUID.fromString(objectMapper.readTree(created).path("id").asText());

        nodeA = UUID.randomUUID();
        mockMvc.perform(put("/api/projects/" + projectA + "/diagram")
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagramBody(nodeA, "No da Alice")))
                .andExpect(status().isOk());

        String element = mockMvc.perform(post("/api/elements/project/" + projectA)
                        .header("Authorization", userA.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Elemento\",\"tipo\":\"TEXTO\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        elementA = UUID.fromString(objectMapper.readTree(element).path("id").asText());
    }

    private String diagramBody(UUID nodeId, String nodeName) throws Exception {
        Map<String, Object> node = Map.of("id", nodeId.toString(), "type", "RECTANGLE", "name", nodeName,
                "x", 10, "y", 20, "width", 200, "height", 120, "rotation", 0);
        Map<String, Object> body = Map.of(
                "project", Map.of("name", "Projeto da Alice", "description", "privado"),
                "viewport", Map.of("zoom", 1, "offsetX", 0, "offsetY", 0, "gridSize", 32),
                "nodes", List.of(node),
                "connections", List.of());
        return objectMapper.writeValueAsString(body);
    }

    private ResultActions asB(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", userB.bearer()));
    }

    // ---------- o dono continua acessando normalmente ----------

    @Test
    void donoAcessaOsProprios() throws Exception {
        mockMvc.perform(get("/api/projects/user/" + userA.id()).header("Authorization", userA.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Projeto da Alice"));
        mockMvc.perform(get("/api/projects/" + projectA).header("Authorization", userA.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/projects/" + projectA + "/diagram").header("Authorization", userA.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].name").value("No da Alice"));
        mockMvc.perform(get("/api/users/" + userA.id()).header("Authorization", userA.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/elements/" + elementA).header("Authorization", userA.bearer()))
                .andExpect(status().isOk());
    }

    // ---------- projetos ----------

    @Test
    void listarProjetosDeOutroUsuarioRetorna404() throws Exception {
        asB(get("/api/projects/user/" + userA.id())).andExpect(status().isNotFound());
    }

    @Test
    void criarProjetoNaContaDeOutroUsuarioRetorna404() throws Exception {
        asB(post("/api/projects/user/" + userA.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"invasor\"}")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/user/" + userA.id()).header("Authorization", userA.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void lerProjetoAlheioRetorna404() throws Exception {
        asB(get("/api/projects/" + projectA)).andExpect(status().isNotFound());
    }

    @Test
    void alterarProjetoAlheioRetorna404ENaoAltera() throws Exception {
        asB(put("/api/projects/" + projectA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"hackeado\",\"descricao\":\"x\"}")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/" + projectA).header("Authorization", userA.bearer()))
                .andExpect(jsonPath("$.nome").value("Projeto da Alice"));
    }

    @Test
    void apagarProjetoAlheioRetorna404ENaoApaga() throws Exception {
        asB(delete("/api/projects/" + projectA)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/" + projectA).header("Authorization", userA.bearer()))
                .andExpect(status().isOk());
    }

    // ---------- diagramas ----------

    @Test
    void carregarDiagramaAlheioRetorna404() throws Exception {
        asB(get("/api/projects/" + projectA + "/diagram")).andExpect(status().isNotFound());
    }

    @Test
    void salvarDiagramaAlheioRetorna404ENaoAltera() throws Exception {
        asB(put("/api/projects/" + projectA + "/diagram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(diagramBody(UUID.randomUUID(), "invasor"))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/" + projectA + "/diagram").header("Authorization", userA.bearer()))
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].name").value("No da Alice"));
    }

    @Test
    void reaproveitarIdDeNoAlheioNoProprioProjetoEhRecusado() throws Exception {
        String created = mockMvc.perform(post("/api/projects/user/" + userB.id())
                        .header("Authorization", userB.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Projeto do Bruno\"}"))
                .andReturn().getResponse().getContentAsString();
        String projectB = objectMapper.readTree(created).path("id").asText();

        asB(put("/api/projects/" + projectB + "/diagram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(diagramBody(nodeA, "sequestrado"))).andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/projects/" + projectA + "/diagram").header("Authorization", userA.bearer()))
                .andExpect(jsonPath("$.nodes[0].name").value("No da Alice"));
    }

    // ---------- usuarios ----------

    @Test
    void listarUsuariosRetornaApenasOProprio() throws Exception {
        String response = asB(get("/api/users")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode users = objectMapper.readTree(response);
        assertEquals(1, users.size());
        assertEquals(userB.id().toString(), users.get(0).path("id").asText());
    }

    @Test
    void lerUsuarioAlheioRetorna404() throws Exception {
        asB(get("/api/users/" + userA.id())).andExpect(status().isNotFound());
    }

    @Test
    void alterarUsuarioAlheioRetorna404ENaoAltera() throws Exception {
        asB(put("/api/users/" + userA.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"hackeado\",\"email\":\"hack@example.test\"}")).andExpect(status().isNotFound());
        assertEquals("Alice", userRepository.findById(userA.id()).orElseThrow().getNome());
    }

    @Test
    void trocarSenhaDeOutroUsuarioRetorna404ENaoTroca() throws Exception {
        asB(put("/api/users/" + userA.id() + "/password")
                .contentType(MediaType.TEXT_PLAIN)
                .content("nova-senha-do-invasor")).andExpect(status().isNotFound());
        assertFalse(login(userA.email(), PASSWORD).isBlank(), "a senha original de A deve continuar valida");
    }

    @Test
    void excluirUsuarioAlheioRetorna404ENaoExclui() throws Exception {
        asB(delete("/api/users/" + userA.id())).andExpect(status().isNotFound());
        assertTrue(userRepository.existsById(userA.id()));
    }

    @Test
    void usuarioTrocaAPropriaSenha() throws Exception {
        asB(put("/api/users/" + userB.id() + "/password")
                .contentType(MediaType.TEXT_PLAIN)
                .content("outra-senha-123")).andExpect(status().isNoContent());
        assertFalse(login(userB.email(), "outra-senha-123").isBlank());
    }

    // ---------- API legada de elementos ----------

    @Test
    void elementosAlheiosRetornam404() throws Exception {
        asB(get("/api/elements/project/" + projectA)).andExpect(status().isNotFound());
        asB(post("/api/elements/project/" + projectA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"x\",\"tipo\":\"TEXTO\"}")).andExpect(status().isNotFound());
        asB(get("/api/elements/" + elementA)).andExpect(status().isNotFound());
        asB(put("/api/elements/" + elementA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"x\"}")).andExpect(status().isNotFound());
        asB(put("/api/elements/" + elementA + "/content")
                .contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isNotFound());
        asB(put("/api/elements/" + elementA + "/coordinates")
                .contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isNotFound());
        asB(put("/api/elements/" + elementA + "/relations")
                .contentType(MediaType.TEXT_PLAIN).content("x")).andExpect(status().isNotFound());
        asB(delete("/api/elements/" + elementA)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/elements/" + elementA).header("Authorization", userA.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Elemento"));
    }
}
