package com.agrogestao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
        return new Session(auth.get("token").asText(), email, null);
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
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? null : objectMapper.readTree(response);
    }

    protected JsonNode get(String path, Session session) throws Exception {
        return call(HttpMethod.GET, path, session, null, 200);
    }

    protected JsonNode post(String path, Session session, Object body) throws Exception {
        return call(HttpMethod.POST, path, session, body, 200);
    }
}
