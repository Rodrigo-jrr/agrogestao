package com.agrogestao.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

class AccountDeletionTest extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void previewSeparatesWhatIsDeletedLeftOrBlocking() throws Exception {
        Session me = registerOwnerWithProperty("Eu");                          // sozinha: apaga
        Session partner = registerOwnerWithProperty("Sócia");
        Session sharedAsOwner = addMember(partner, me, "OWNER");                // outra dona: só sai
        Session other = registerOwnerWithProperty("Outro");
        addMember(other, me, "WORKER");                                         // não é dona: só sai
        Session mine2 = createSecondProperty(me, "Sítio Com Equipe");
        addMember(mine2, registerUser("Peão"), "WORKER");                       // única dona com equipe: bloqueia

        JsonNode preview = get("/api/me/deletion-preview", me);
        assertThat(names(preview.get("toDelete"))).containsExactly("Sítio Eu");
        assertThat(names(preview.get("toLeave"))).containsExactlyInAnyOrder("Sítio Sócia", "Sítio Outro");
        assertThat(names(preview.get("blocking"))).containsExactly("Sítio Com Equipe");
        assertThat(sharedAsOwner).isNotNull();
    }

    @Test
    void blockingPropertyOrWrongPasswordPreventsDeletion() throws Exception {
        Session me = registerOwnerWithProperty("Eu");
        addMember(me, registerUser("Peão"), "WORKER");

        call(HttpMethod.DELETE, "/api/me", me, Map.of("password", "senha1234"), 422);
        call(HttpMethod.DELETE, "/api/me", registerUser("Outra"), Map.of("password", "errada123"), 422);
        assertThat(get("/api/me", me).get("email").asString()).isEqualTo(me.email());
    }

    @Test
    void deletingRemovesPersonalDataAndSoloPropertiesButKeepsSharedOnes() throws Exception {
        Session me = registerOwnerWithProperty("Apagada");
        post("/api/activities", me, Map.of("name", "Horta", "type", "HORTA"));
        Session partner = registerOwnerWithProperty("Sócia");
        Session inShared = addMember(partner, me, "WORKER");
        post("/api/activities", inShared, Map.of("name", "Milho", "type", "AGRICULTURA"));
        Long myId = get("/api/me", me).get("userId").asLong();

        call(HttpMethod.DELETE, "/api/me", me, Map.of("password", "senha1234"), 204);

        // Sessão encerrada, login não funciona e o e-mail fica livre para uma conta nova.
        call(HttpMethod.GET, "/api/me", me, null, 401);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", me.email(), "password", "senha1234"), 401);
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ?", myId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM terms_acceptances WHERE user_id = ?", myId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_tokens WHERE user_id = ?", myId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM properties WHERE id = ?", me.propertyId())).isZero();

        // A propriedade compartilhada continua, com o que a pessoa registrou, mas sem o nome/e-mail dela no histórico.
        assertThat(get("/api/activities", partner).toString()).contains("Milho");
        List<String> details = new ArrayList<>();
        page("/api/properties/current/audit", partner).forEach(e -> details.add(e.get("details").asString()));
        assertThat(String.join(" | ", details)).doesNotContain(me.email()).doesNotContain("Apagada")
                .contains("usuário removido");
        JsonNode newest = page("/api/properties/current/audit", partner).get(0);
        assertThat(newest.get("userName").asString()).isEqualTo("Usuário removido");

        post("/api/auth/register", null, registration("De Novo", me.email(), "senha1234"));
    }

    private Session createSecondProperty(Session owner, String name) throws Exception {
        Long organizationId = get("/api/properties/me", owner).get(0).get("organizationId").asLong();
        JsonNode created = post("/api/properties", owner, Map.of("organizationId", organizationId, "name", name));
        return owner.on(created.get("id").asLong());
    }

    private int count(String sql, Long id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }

    private static List<String> names(JsonNode refs) {
        List<String> names = new ArrayList<>();
        refs.forEach(ref -> names.add(ref.get("name").asString()));
        return names;
    }
}
