package com.polo42.project_flow;

import com.polo42.project_flow.service.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * Com SMTP configurado o comportamento original e mantido: o e-mail e enviado pelo JavaMailSender
 * (mock nos testes) e o conteudo nao e despejado no log.
 */
@TestPropertySource(properties = {"spring.mail.host=smtp.exemplo.invalid", "spring.mail.from=noreply@example.test"})
class MailSmtpIntegrationTest extends MailTestSupport {

    @Autowired
    private MailService mailService;

    @Test
    void cadastroEnviaEmailPeloSmtp() throws Exception {
        assertTrue(mailService.isSmtpConfigured());

        register("com-smtp@example.test");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@example.test", message.getFrom());
        assertEquals("com-smtp@example.test", message.getTo()[0]);
        assertTrue(message.getText().contains("/verify-email?token="));
        assertFalse(loggedText().contains("/verify-email?token="));
    }
}
