package com.polo42.project_flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo42.project_flow.model.User;
import com.polo42.project_flow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base dos testes de integracao: perfil "test" (H2 em modo MySQL, segredo JWT de teste)
 * e envio de e-mail substituido por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "senha-de-teste-123";

    @MockBean
    protected JavaMailSender mailSender;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Usuario de teste ja verificado, com token JWT obtido pelo login real. */
    protected record TestUser(UUID id, String email, String token) {
        public String bearer() {
            return "Bearer " + token;
        }
    }

    protected TestUser createUser(String nome) throws Exception {
        User user = new User();
        user.setNome(nome);
        user.setEmail(nome.toLowerCase() + "-" + UUID.randomUUID() + "@example.test");
        user.setSenha(passwordEncoder.encode(PASSWORD));
        user.setEmailVerificado(true);
        user.setDataVerificado(LocalDateTime.now());
        user = userRepository.save(user);
        return new TestUser(user.getId(), user.getEmail(), login(user.getEmail(), PASSWORD));
    }

    protected String login(String email, String senha) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "senha", senha));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.path("token").asText();
    }
}
