package com.polo42.project_flow;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.polo42.project_flow.exception.GlobalExceptionHandler;
import com.polo42.project_flow.model.EmailVerificationToken;
import com.polo42.project_flow.model.PasswordResetToken;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.DiagramConnectionRepository;
import com.polo42.project_flow.repository.DiagramNodeRepository;
import com.polo42.project_flow.repository.DiagramViewStateRepository;
import com.polo42.project_flow.repository.ElementRepository;
import com.polo42.project_flow.repository.EmailVerificationTokenRepository;
import com.polo42.project_flow.repository.PasswordResetTokenRepository;
import com.polo42.project_flow.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um teste (ou grupo curto) por bug corrigido.
 */
class BugFixesIntegrationTest extends AbstractIntegrationTest {

    private static final String ISO_DATE_TIME = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$";

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private DiagramNodeRepository nodeRepository;
    @Autowired
    private DiagramConnectionRepository connectionRepository;
    @Autowired
    private DiagramViewStateRepository viewStateRepository;
    @Autowired
    private ElementRepository elementRepository;
    @Autowired
    private EmailVerificationTokenRepository emailTokenRepository;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;
    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    // ---------- helpers ----------

    private UUID createProject(TestUser user, String nome) throws Exception {
        String created = mockMvc.perform(post("/api/projects/user/" + user.id())
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome, "descricao", "desc"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).path("id").asText());
    }

    private Map<String, Object> node(UUID id, String name) {
        return Map.of("id", id.toString(), "type", "RECTANGLE", "name", name,
                "x", 1, "y", 2, "width", 200, "height", 120, "rotation", 0);
    }

    private String diagramBody(String projectName, String projectDescription, List<Map<String, Object>> nodes,
                               List<Map<String, Object>> connections) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "project", Map.of("name", projectName, "description", projectDescription),
                "viewport", Map.of("zoom", 1, "offsetX", 0, "offsetY", 0, "gridSize", 32),
                "nodes", nodes,
                "connections", connections));
    }

    // ---------- bug: renomear/descricao nao persistia e ultimaAlteracao nunca mudava ----------

    @Test
    void salvarDiagramaPersisteNomeDescricaoEAtualizaUltimaAlteracao() throws Exception {
        TestUser user = createUser("Carla");
        UUID projectId = createProject(user, "Nome antigo");

        String before = mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", user.bearer()))
                .andReturn().getResponse().getContentAsString();
        LocalDateTime ultimaAntes = LocalDateTime.parse(objectMapper.readTree(before).path("ultimaAlteracao").asText());

        Thread.sleep(25);
        mockMvc.perform(put("/api/projects/" + projectId + "/diagram")
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagramBody("Nome novo", "Descricao nova",
                                List.of(node(UUID.randomUUID(), "A")), List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("Nome novo"))
                .andExpect(jsonPath("$.projectDescription").value("Descricao nova"));

        // nova leitura, em outra requisicao: precisa ter ido para o banco
        String after = mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome novo"))
                .andExpect(jsonPath("$.descricao").value("Descricao nova"))
                .andReturn().getResponse().getContentAsString();
        LocalDateTime ultimaDepois = LocalDateTime.parse(objectMapper.readTree(after).path("ultimaAlteracao").asText());
        assertTrue(ultimaDepois.isAfter(ultimaAntes), "ultimaAlteracao deve avancar ao salvar o diagrama");
    }

    // ---------- bug: excluir usuario dava 500 ----------

    @Test
    void excluirUsuarioRemoveTodosOsDadosEmUmaTransacao() throws Exception {
        TestUser user = createUser("Diego");
        TestUser other = createUser("Elisa");
        UUID projectId = createProject(user, "Projeto do Diego");
        UUID otherProjectId = createProject(other, "Projeto da Elisa");

        UUID n1 = UUID.randomUUID();
        UUID n2 = UUID.randomUUID();
        mockMvc.perform(put("/api/projects/" + projectId + "/diagram")
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagramBody("Projeto do Diego", "d",
                                List.of(node(n1, "A"), node(n2, "B")),
                                List.of(Map.of("id", UUID.randomUUID().toString(),
                                        "fromNodeId", n1.toString(), "toNodeId", n2.toString())))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/elements/project/" + projectId)
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"legado\",\"tipo\":\"TEXTO\"}"))
                .andExpect(status().isOk());

        User entity = userRepository.findById(user.id()).orElseThrow();
        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setUser(entity);
        emailToken.setToken(UUID.randomUUID().toString());
        emailToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailTokenRepository.save(emailToken);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(entity);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetTokenRepository.save(resetToken);

        mockMvc.perform(delete("/api/users/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(user.id()));
        assertFalse(projectRepository.existsById(projectId));
        assertEquals(0, nodeRepository.countByProjetoId(projectId));
        assertTrue(connectionRepository.findByProjetoId(projectId).isEmpty());
        assertTrue(viewStateRepository.findByProjetoId(projectId).isEmpty());
        assertTrue(elementRepository.findByProjetoId(projectId).isEmpty());
        assertTrue(emailTokenRepository.findAll().stream().noneMatch(t -> t.getId().equals(emailToken.getId())));
        assertTrue(resetTokenRepository.findAll().stream().noneMatch(t -> t.getId().equals(resetToken.getId())));

        // dados de outro usuario permanecem
        assertTrue(userRepository.existsById(other.id()));
        assertTrue(projectRepository.existsById(otherProjectId));
    }

    // ---------- bug: handler transformava 400/404/405 em 500 (e nao logava) ----------

    @Test
    void rotaInexistenteRetorna404ComMessage() throws Exception {
        TestUser user = createUser("Fabio");
        mockMvc.perform(get("/api/rota-que-nao-existe").header("Authorization", user.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void metodoNaoSuportadoRetorna405ComMessage() throws Exception {
        TestUser user = createUser("Gabi");
        UUID projectId = createProject(user, "P");
        mockMvc.perform(post("/api/projects/" + projectId).header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void jsonMalformadoRetorna400ComMessage() throws Exception {
        TestUser user = createUser("Hugo");
        mockMvc.perform(post("/api/projects/user/" + user.id()).header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void uuidInvalidoNaUrlRetorna400ComMessage() throws Exception {
        TestUser user = createUser("Iris");
        mockMvc.perform(get("/api/projects/nao-e-um-uuid").header("Authorization", user.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void idDeNoInvalidoRetorna400() throws Exception {
        TestUser user = createUser("Joao");
        UUID projectId = createProject(user, "P");
        String body = diagramBody("P", "d", List.of(Map.of("id", "123", "type", "RECTANGLE", "name", "x",
                "x", 1, "y", 1, "rotation", 0)), List.of());
        mockMvc.perform(put("/api/projects/" + projectId + "/diagram").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void criarProjetoSemNomeRetorna400ComMessageEMantemOCampo() throws Exception {
        TestUser user = createUser("Karla");
        mockMvc.perform(post("/api/projects/user/" + user.id()).header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nome").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void semTokenRetorna401ComMessage() throws Exception {
        mockMvc.perform(get("/api/projects/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(get("/api/projects/" + UUID.randomUUID()).header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void erroInesperadoRetorna500ComMessageEEhRegistradoNoLog() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ResponseEntity<Object> response = exceptionHandler.handleGlobalException(
                    new IllegalStateException("falha simulada"),
                    new ServletWebRequest(new MockHttpServletRequest("GET", "/api/qualquer")));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(appender.list.stream().anyMatch(e ->
                    e.getLevel() == Level.ERROR && e.getThrowableProxy() != null
                            && "falha simulada".equals(e.getThrowableProxy().getMessage())),
                    "a excecao deve ser registrada em nivel ERROR com stack trace");
        } finally {
            logger.detachAppender(appender);
        }
    }

    // ---------- bug: datas saiam como array (@EnableWebMvc anulava o JacksonConfig) ----------

    @Test
    void datasSaemComoStringIso8601() throws Exception {
        TestUser user = createUser("Lia");
        UUID projectId = createProject(user, "P");

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.dataCriado").isString())
                .andExpect(jsonPath("$.dataCriado").value(org.hamcrest.Matchers.matchesPattern(ISO_DATE_TIME)))
                .andExpect(jsonPath("$.ultimaAlteracao").value(org.hamcrest.Matchers.matchesPattern(ISO_DATE_TIME)))
                .andExpect(jsonPath("$.ultimoAcesso").value(org.hamcrest.Matchers.matchesPattern(ISO_DATE_TIME)));
        mockMvc.perform(get("/api/projects/user/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(jsonPath("$[0].dataCriado").isString());
        mockMvc.perform(get("/api/users/" + user.id()).header("Authorization", user.bearer()))
                .andExpect(jsonPath("$.dataCriado").value(org.hamcrest.Matchers.matchesPattern(ISO_DATE_TIME)))
                .andExpect(jsonPath("$.dataVerificado").value(org.hamcrest.Matchers.matchesPattern(ISO_DATE_TIME)));
    }

    // ---------- bug: nome do no limitado a 20 (agora 35) ----------

    @Test
    void nomeDoNoAceitaAte35Caracteres() throws Exception {
        TestUser user = createUser("Marco");
        UUID projectId = createProject(user, "P");
        String name35 = "n".repeat(35);
        String name36 = "n".repeat(36);

        mockMvc.perform(put("/api/projects/" + projectId + "/diagram").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagramBody("P", "d", List.of(node(UUID.randomUUID(), name35)), List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].name").value(name35));

        mockMvc.perform(put("/api/projects/" + projectId + "/diagram").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagramBody("P", "d", List.of(node(UUID.randomUUID(), name36)), List.of())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // o diagrama anterior (35 caracteres) continua intacto apos a tentativa recusada
        JsonNode diagram = objectMapper.readTree(mockMvc.perform(get("/api/projects/" + projectId + "/diagram")
                        .header("Authorization", user.bearer()))
                .andReturn().getResponse().getContentAsString());
        assertEquals(name35, diagram.path("nodes").get(0).path("name").asText());
    }
}
