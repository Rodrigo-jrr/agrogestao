package com.agrogestao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Base para testes de API ponta a ponta: HTTP real (JWT + filtro de tenant) sobre H2 com as migrations Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** E-mails não saem nos testes: são capturados aqui. */
    @MockitoBean
    protected JavaMailSender mailSender;

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)");

    /** Token do último e-mail enviado para {@code email} cujo link contém {@code path}. */
    protected String lastEmailToken(String email, String path) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        List<SimpleMailMessage> sent = captor.getAllValues();
        for (int i = sent.size() - 1; i >= 0; i--) {
            SimpleMailMessage message = sent.get(i);
            if (message.getTo() != null && email.equalsIgnoreCase(message.getTo()[0]) && message.getText().contains(path)) {
                Matcher matcher = TOKEN_IN_LINK.matcher(message.getText());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        throw new AssertionError("nenhum e-mail com " + path + " para " + email);
    }

    protected long emailsSentTo(String email) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeast(0)).send(captor.capture());
        return captor.getAllValues().stream()
                .filter(m -> m.getTo() != null && email.equalsIgnoreCase(m.getTo()[0])).count();
    }

    protected void verifyEmailOf(Session session) throws Exception {
        call(HttpMethod.POST, "/api/auth/verify-email", null,
                Map.of("token", lastEmailToken(session.email(), "/verificar-email")), 204);
    }

    /** Usuário autenticado e, opcionalmente, a propriedade em uso (header X-Property-Id). */
    public record Session(String token, String email, Long propertyId) {
        public Session on(Long otherPropertyId) {
            return new Session(token, email, otherPropertyId);
        }
    }

    protected Session registerUser(String name) throws Exception {
        String email = name.toLowerCase().replace(' ', '.') + SEQ.incrementAndGet() + "@teste.dev";
        JsonNode auth = call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", name, "email", email, "password", "senha1234"), 200);
        return new Session(auth.get("token").asString(), email, null);
    }

    /** Registra um usuário e cria organização + propriedade com ele como OWNER. */
    protected Session registerOwnerWithProperty(String name) throws Exception {
        Session session = registerUser(name);
        JsonNode property = call(HttpMethod.POST, "/api/onboarding", session, Map.of(
                "organizationName", "Org " + name,
                "propertyName", "Sítio " + name,
                "activities", List.of(Map.of("name", "Leite", "type", "PECUARIA"))), 200);
        return session.on(property.get("id").asLong());
    }

    /** Adiciona {@code member} à propriedade de {@code owner} com o papel informado. */
    protected Session addMember(Session owner, Session member, String role) throws Exception {
        call(HttpMethod.POST, "/api/properties/current/members", owner,
                Map.of("email", member.email(), "role", role), 200);
        return member.on(owner.propertyId());
    }

    protected JsonNode call(HttpMethod method, String path, Session session, Object body, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path);
        if (session != null) {
            builder.header("Authorization", "Bearer " + session.token());
            if (session.propertyId() != null) {
                builder.header("X-Property-Id", session.propertyId());
            }
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        String response = mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return response.isEmpty() ? null : objectMapper.readTree(response);
    }

    protected JsonNode get(String path, Session session) throws Exception {
        return call(HttpMethod.GET, path, session, null, 200);
    }

    protected JsonNode post(String path, Session session, Object body) throws Exception {
        return call(HttpMethod.POST, path, session, body, 200);
    }
}
