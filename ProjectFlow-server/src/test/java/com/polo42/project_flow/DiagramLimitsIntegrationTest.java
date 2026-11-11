package com.polo42.project_flow;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ids repetidos no mesmo payload e limite de tamanho do diagrama respondem 422 (nao 500).
 */
class DiagramLimitsIntegrationTest extends AbstractIntegrationTest {

    private UUID createProject(TestUser user) throws Exception {
        String created = mockMvc.perform(post("/api/projects/user/" + user.id())
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"P\"}"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).path("id").asText());
    }

    private Map<String, Object> node(UUID id) {
        return Map.of("id", id.toString(), "type", "RECTANGLE", "name", "n", "x", 1, "y", 1, "rotation", 0);
    }

    private void save(TestUser user, UUID projectId, List<Map<String, Object>> nodes,
                      List<Map<String, Object>> connections, int expectedStatus) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "project", Map.of("name", "P", "description", "d"),
                "viewport", Map.of("zoom", 1, "offsetX", 0, "offsetY", 0, "gridSize", 32),
                "nodes", nodes, "connections", connections));
        mockMvc.perform(put("/api/projects/" + projectId + "/diagram").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andExpect(expectedStatus == 200 ? jsonPath("$.projectId").isNotEmpty() : jsonPath("$.message").isNotEmpty());
    }

    @Test
    void idsDeNosDuplicadosNoPayloadRetornam422() throws Exception {
        TestUser user = createUser("Dup");
        UUID projectId = createProject(user);
        UUID same = UUID.randomUUID();
        save(user, projectId, List.of(node(same), node(same)), List.of(), 422);
    }

    @Test
    void idsDeConexoesDuplicadosNoPayloadRetornam422() throws Exception {
        TestUser user = createUser("DupCon");
        UUID projectId = createProject(user);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID conId = UUID.randomUUID();
        Map<String, Object> con = Map.of("id", conId.toString(), "fromNodeId", a.toString(), "toNodeId", b.toString());
        save(user, projectId, List.of(node(a), node(b)), List.of(con, con), 422);
    }

    @Test
    void limiteDeNosEDeConexoes() throws Exception {
        TestUser user = createUser("Limite");
        UUID projectId = createProject(user);

        List<Map<String, Object>> nodes500 = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            nodes500.add(node(UUID.randomUUID()));
        }
        save(user, projectId, nodes500, List.of(), 200);

        List<Map<String, Object>> nodes501 = new ArrayList<>(nodes500);
        nodes501.add(node(UUID.randomUUID()));
        save(user, projectId, nodes501, List.of(), 422);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<Map<String, Object>> manyConnections = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            manyConnections.add(Map.of("id", UUID.randomUUID().toString(),
                    "fromNodeId", a.toString(), "toNodeId", b.toString()));
        }
        save(user, projectId, List.of(node(a), node(b)), manyConnections, 422);
    }
}
