package com.polo42.project_flow;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base dos testes de integracao: perfil "test" (H2 em modo MySQL, segredo JWT de teste)
 * e envio de e-mail substituido por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @MockBean
    protected JavaMailSender mailSender;
}
