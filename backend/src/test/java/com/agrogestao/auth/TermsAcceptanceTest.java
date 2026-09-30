package com.agrogestao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import com.agrogestao.auth.terms.TermsService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

class TermsAcceptanceTest extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationRequiresAcceptingTheCurrentTerms() throws Exception {
        call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Sem", "email", "sem.termos@teste.dev", "password", "senha1234"), 400);
        call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Velho", "email", "velho.termos@teste.dev", "password", "senha1234",
                        "termsVersion", "2000-01-01"), 422);
        call(HttpMethod.POST, "/api/auth/login", null,
                Map.of("email", "sem.termos@teste.dev", "password", "senha1234"), 401);
    }

    @Test
    void acceptanceIsRecordedWithVersionAndDate() throws Exception {
        Session user = registerUser("Aceita");
        assertThat(get("/api/me", user).get("termsAccepted").asBoolean()).isTrue();

        Long userId = get("/api/me", user).get("userId").asLong();
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT terms_version, accepted_at, ip_address FROM terms_acceptances WHERE user_id = ?", userId);
        assertThat(row.get("terms_version")).isEqualTo(TermsService.CURRENT_VERSION);
        assertThat(row.get("accepted_at")).isNotNull();
        assertThat(row.get("ip_address")).isEqualTo("127.0.0.1");
    }

    @Test
    void existingUserAcceptsNewVersion() throws Exception {
        Session user = registerUser("Antiga");
        Long userId = get("/api/me", user).get("userId").asLong();
        // Simula quem aceitou uma versão anterior.
        jdbcTemplate.update("UPDATE terms_acceptances SET terms_version = '2000-01-01' WHERE user_id = ?", userId);
        assertThat(get("/api/me", user).get("termsAccepted").asBoolean()).isFalse();

        call(HttpMethod.POST, "/api/me/accept-terms", user, Map.of("termsVersion", "2000-01-01"), 422);
        post("/api/me/accept-terms", user, Map.of("termsVersion", TermsService.CURRENT_VERSION));

        assertThat(get("/api/me", user).get("termsAccepted").asBoolean()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM terms_acceptances WHERE user_id = ?",
                Integer.class, userId)).isEqualTo(2); // o histórico do aceite anterior fica
    }

    @Test
    void invitationRegistrationAlsoRequiresTerms() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        String email = "termos.convite" + System.nanoTime() + "@teste.dev";
        post("/api/properties/current/invitations", owner, Map.of("email", email, "role", "WORKER"));
        String token = lastEmailToken(email, "/convite");

        call(HttpMethod.POST, "/api/auth/invitations/register", null,
                Map.of("token", token, "name", "Sem", "password", "senha1234"), 400);
        post("/api/auth/invitations/register", null,
                Map.of("token", token, "name", "Com", "password", "senha1234", "termsVersion", TermsService.CURRENT_VERSION));
    }
}
