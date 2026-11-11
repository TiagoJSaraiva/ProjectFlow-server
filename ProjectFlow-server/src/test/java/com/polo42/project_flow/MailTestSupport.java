package com.polo42.project_flow;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.polo42.project_flow.service.MailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Apoio aos testes de e-mail: captura o log do MailService e executa o cadastro. */
abstract class MailTestSupport extends AbstractIntegrationTest {

    protected ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(MailService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    protected String loggedText() {
        StringBuilder sb = new StringBuilder();
        appender.list.forEach(e -> sb.append(e.getFormattedMessage()).append(System.lineSeparator()));
        return sb.toString();
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Pessoa Teste", "email", email, "senha", "senha-123"))))
                .andExpect(status().isOk());
    }
}
