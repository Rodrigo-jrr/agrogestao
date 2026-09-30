package com.agrogestao.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

class AdminAccessTest extends ApiTestSupport {

    /** Configurado em application-test.yml; cadastrado com maiúsculas para testar a comparação sem caixa. */
    private static final String ADMIN_EMAIL = "Admin@Plataforma.test";

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Session admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = loginOrRegisterAdmin();
    }

    @Test
    void anonymousRequestsGetUnauthorized() throws Exception {
        call(HttpMethod.GET, "/api/admin/overview", null, null, 401);
    }

    @Test
    void regularUsersAndPropertyOwnersAreForbidden() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        call(HttpMethod.GET, "/api/admin/overview", owner, null, 403);
        call(HttpMethod.GET, "/api/admin/overview", owner.on(null), null, 403);
        assertThat(get("/api/me", owner.on(null)).get("platformAdmin").asBoolean()).isFalse();
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        Session forged = new Session(admin.token().substring(0, admin.token().length() - 4) + "abcd", ADMIN_EMAIL, null);
        call(HttpMethod.GET, "/api/admin/overview", forged, null, 401);
    }

    @Test
    void adminSeesEveryOrganizationWithoutSensitiveFields() throws Exception {
        Session owner = registerOwnerWithProperty("Cliente");
        post("/api/activities", owner, Map.of("name", "Soja", "type", "AGRICULTURA"));

        JsonNode overview = get("/api/admin/overview", admin);

        assertThat(overview.toString()).doesNotContainIgnoringCase("password");
        JsonNode org = findByField(overview.get("organizations"), "ownerEmail", owner.email());
        assertThat(org.get("name").asText()).isEqualTo("Org Cliente");
        JsonNode property = org.get("properties").get(0);
        assertThat(property.get("id").asLong()).isEqualTo(owner.propertyId());
        assertThat(property.get("members").asLong()).isEqualTo(1);
        assertThat(property.get("lastActivityAt").isNull()).isFalse();

        JsonNode adminRow = findByField(overview.get("users"), "email", ADMIN_EMAIL);
        assertThat(adminRow.get("platformAdmin").asBoolean()).isTrue();
        assertThat(overview.get("summary").get("users").asLong()).isEqualTo(overview.get("users").size());
        long propertiesWithActivity = 0;
        for (JsonNode organization : overview.get("organizations")) {
            for (JsonNode p : organization.get("properties")) {
                if (!p.get("lastActivityAt").isNull()) {
                    propertiesWithActivity++;
                }
            }
        }
        assertThat(overview.get("summary").get("activePropertiesLast30Days").asLong()).isEqualTo(propertiesWithActivity);
    }

    @Test
    void meEndpointFlagsAdmin() throws Exception {
        assertThat(get("/api/me", admin).get("platformAdmin").asBoolean()).isTrue();
    }

    @Test
    void everyAdminAccessIsAudited() throws Exception {
        long before = countAdminViews();
        get("/api/admin/overview", admin);
        assertThat(countAdminViews()).isEqualTo(before + 1);
    }

    @Test
    void adminHasNoImplicitAccessToPropertyData() throws Exception {
        Session owner = registerOwnerWithProperty("Privada");
        call(HttpMethod.GET, "/api/activities", admin.on(owner.propertyId()), null, 403);
    }

    private long countAdminViews() {
        return auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() == AuditAction.VIEW && "AdminConsole".equals(log.getEntityName()))
                .count();
    }

    private Session loginOrRegisterAdmin() throws Exception {
        Map<String, String> credentials = Map.of("email", ADMIN_EMAIL, "password", "senha1234");
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login").contentType("application/json")
                        .content(objectMapper.writeValueAsString(credentials)))
                .andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        if (response.has("token")) {
            return new Session(response.get("token").asText(), ADMIN_EMAIL, null);
        }
        JsonNode registered = post("/api/auth/register", null,
                Map.of("name", "Admin", "email", ADMIN_EMAIL, "password", "senha1234"));
        return new Session(registered.get("token").asText(), ADMIN_EMAIL, null);
    }

    private static JsonNode findByField(JsonNode array, String field, String value) {
        for (JsonNode node : array) {
            if (value.equals(node.get(field).asText())) {
                return node;
            }
        }
        throw new AssertionError(field + "=" + value + " não encontrado");
    }
}
