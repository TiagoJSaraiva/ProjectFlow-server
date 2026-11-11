package com.polo42.project_flow;

import com.polo42.project_flow.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem SMTP configurado (perfil de teste padrao: spring.mail.host vazio) a aplicacao sobe normalmente,
 * nada e enviado e o link/codigo de verificacao e recuperacao vai para o log.
 */
class MailLogFallbackIntegrationTest extends MailTestSupport {

    @Autowired
    private MailService mailService;

    @Test
    void cadastroSemSmtpRegistraLinkDeVerificacaoNoLog() throws Exception {
        assertFalse(mailService.isSmtpConfigured());

        register("sem-smtp-cadastro@example.test");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        String log = loggedText();
        assertTrue(log.contains("/verify-email?token="), "o link de verificacao deve aparecer no log");
        assertTrue(log.contains("sem-smtp-cadastro@example.test"));
    }

    @Test
    void recuperacaoDeSenhaSemSmtpRegistraCodigoELinkNoLog() throws Exception {
        register("sem-smtp-recuperacao@example.test");
        appender.list.clear();

        mockMvc.perform(post("/api/auth/recover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sem-smtp-recuperacao@example.test\"}"))
                .andExpect(status().isOk());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertTrue(loggedText().contains("/reset?token="), "o link de recuperacao deve aparecer no log");
    }
}
