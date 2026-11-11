package com.polo42.project_flow;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Datas geradas e serializadas em UTC (o frontend le strings ISO sem fuso como UTC).
 */
class UtcDatesIntegrationTest extends AbstractIntegrationTest {

    @Test
    void fusoPadraoDaJvmEUtc() {
        assertEquals("UTC", TimeZone.getDefault().getID());
    }

    @Test
    void datasDeProjetoSaemEmUtc() throws Exception {
        TestUser user = createUser("Utc");
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(2);

        String created = mockMvc.perform(post("/api/projects/user/" + user.id())
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"P\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).path("id").asText();

        String body = mockMvc.perform(get("/api/projects/" + id).header("Authorization", user.bearer()))
                .andReturn().getResponse().getContentAsString();
        LocalDateTime dataCriado = LocalDateTime.parse(objectMapper.readTree(body).path("dataCriado").asText());
        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(2);

        assertTrue(!dataCriado.isBefore(before) && !dataCriado.isAfter(after),
                "dataCriado deve estar em UTC: " + dataCriado + " fora de [" + before + ", " + after + "]");
        assertTrue(Duration.between(dataCriado, LocalDateTime.now(ZoneOffset.UTC)).abs().toMinutes() < 1);
    }
}
