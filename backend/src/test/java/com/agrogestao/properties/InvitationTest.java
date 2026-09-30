package com.agrogestao.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class InvitationTest extends ApiTestSupport {

    private static final String INVITATIONS = "/api/properties/current/invitations";

    @Test
    void existingAccountJoinsDirectlyAndIsNotified() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session colleague = registerUser("Colega");

        JsonNode result = post(INVITATIONS, owner, Map.of("email", colleague.email(), "role", "WORKER"));

        assertThat(result.get("outcome").asString()).isEqualTo("ADDED");
        assertThat(get("/api/properties/me", colleague)).hasSize(1);
        assertThat(emailsSentTo(colleague.email())).isEqualTo(2); // confirmação do cadastro + aviso de entrada
        assertThat(get(INVITATIONS, owner)).isEmpty();
    }

    @Test
    void newPersonRegistersThroughInvitationAndJoins() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        String email = "novato" + System.nanoTime() + "@teste.dev";

        JsonNode result = post(INVITATIONS, owner, Map.of("email", email, "role", "MANAGER"));
        assertThat(result.get("outcome").asString()).isEqualTo("INVITED");
        assertThat(get(INVITATIONS, owner)).hasSize(1);

        String token = lastEmailToken(email, "/convite");
        JsonNode preview = post("/api/auth/invitations/preview", null, Map.of("token", token));
        assertThat(preview.get("propertyName").asString()).isEqualTo("Sítio Dono");
        assertThat(preview.get("invitedByName").asString()).isEqualTo("Dono");
        assertThat(preview.get("accountExists").asBoolean()).isFalse();

        JsonNode auth = post("/api/auth/invitations/register", null,
                Map.of("token", token, "name", "Novato", "password", "senha1234",
                        "termsVersion", com.agrogestao.auth.terms.TermsService.CURRENT_VERSION));
        Session invited = new Session(auth.get("token").asString(), email, null);

        JsonNode me = get("/api/me", invited);
        assertThat(me.get("email").asString()).isEqualTo(email);
        assertThat(me.get("emailVerified").asBoolean()).isTrue();
        JsonNode properties = get("/api/properties/me", invited);
        assertThat(properties).hasSize(1);
        assertThat(properties.get(0).get("role").asString()).isEqualTo("MANAGER");
        assertThat(get(INVITATIONS, owner)).isEmpty();

        // Uso único.
        call(HttpMethod.POST, "/api/auth/invitations/preview", null, Map.of("token", token), 422);
    }

    @Test
    void invitationOnlyWorksForTheInvitedEmail() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session stranger = registerUser("Estranho");
        String email = "convidada" + System.nanoTime() + "@teste.dev";
        post(INVITATIONS, owner, Map.of("email", email, "role", "VIEWER"));
        String token = lastEmailToken(email, "/convite");

        call(HttpMethod.POST, "/api/invitations/accept", stranger, Map.of("token", token), 422);
        assertThat(get("/api/properties/me", stranger)).isEmpty();

        // A pessoa convidada cria a conta por fora e depois aceita logada.
        JsonNode auth = post("/api/auth/register", null, registration("Convidada", email, "senha1234"));
        Session invited = new Session(auth.get("token").asString(), email, null);
        JsonNode joined = post("/api/invitations/accept", invited, Map.of("token", token));
        assertThat(joined.get("role").asString()).isEqualTo("VIEWER");
        assertThat(get("/api/me", invited).get("emailVerified").asBoolean()).isTrue();
    }

    @Test
    void managerCannotInviteOwnersAndWorkerCannotInvite() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");

        call(HttpMethod.POST, INVITATIONS, manager, Map.of("email", "x" + System.nanoTime() + "@teste.dev", "role", "OWNER"), 403);
        call(HttpMethod.POST, INVITATIONS, worker, Map.of("email", "y" + System.nanoTime() + "@teste.dev", "role", "VIEWER"), 403);
        call(HttpMethod.GET, INVITATIONS, worker, null, 403);
    }

    @Test
    void resendReplacesLinkAndCancelInvalidatesIt() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        String email = "reenvio" + System.nanoTime() + "@teste.dev";
        long id = post(INVITATIONS, owner, Map.of("email", email, "role", "WORKER")).get("invitation").get("id").asLong();
        String first = lastEmailToken(email, "/convite");

        post(INVITATIONS + "/" + id + "/resend", owner, null);
        String second = lastEmailToken(email, "/convite");
        assertThat(second).isNotEqualTo(first);
        call(HttpMethod.POST, "/api/auth/invitations/preview", null, Map.of("token", first), 422);
        post("/api/auth/invitations/preview", null, Map.of("token", second));

        call(HttpMethod.DELETE, INVITATIONS + "/" + id, owner, null, 204);
        call(HttpMethod.POST, "/api/auth/invitations/preview", null, Map.of("token", second), 422);
    }

    @Test
    void invitationsAreIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session other = registerOwnerWithProperty("Outro");
        long id = post(INVITATIONS, owner, Map.of("email", "iso" + System.nanoTime() + "@teste.dev", "role", "WORKER"))
                .get("invitation").get("id").asLong();

        assertThat(get(INVITATIONS, other)).isEmpty();
        call(HttpMethod.DELETE, INVITATIONS + "/" + id, other, null, 404);
        call(HttpMethod.POST, INVITATIONS + "/" + id + "/resend", other, null, 404);
    }
}
