package com.agrogestao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class ProfileTest extends ApiTestSupport {

    @Test
    void userChangesOwnName() throws Exception {
        Session user = registerUser("Antigo");
        JsonNode me = call(HttpMethod.PUT, "/api/me", user, Map.of("name", "  Nome Novo "), 200);
        assertThat(me.get("name").asString()).isEqualTo("Nome Novo");
        call(HttpMethod.PUT, "/api/me", user, Map.of("name", " "), 400);
    }

    @Test
    void changingPasswordRequiresCurrentOneAndEndsOtherSessions() throws Exception {
        Session user = registerUser("Senha");
        Thread.sleep(1100); // a troca precisa cair num segundo posterior ao token antigo (JWT em segundos)

        call(HttpMethod.POST, "/api/me/password", user,
                Map.of("currentPassword", "errada123", "newPassword", "novaSenha123"), 422);
        call(HttpMethod.POST, "/api/me/password", user,
                Map.of("currentPassword", "senha1234", "newPassword", "senha1234"), 422);

        JsonNode auth = call(HttpMethod.POST, "/api/me/password", user,
                Map.of("currentPassword", "senha1234", "newPassword", "novaSenha123"), 200);
        Session renewed = new Session(auth.get("token").asString(), user.email(), null);

        call(HttpMethod.GET, "/api/me", user, null, 401);
        assertThat(get("/api/me", renewed).get("email").asString()).isEqualTo(user.email());
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "novaSenha123"), 200);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "senha1234"), 401);
        assertThat(emailsSentTo(user.email())).isEqualTo(2); // confirmação do cadastro + aviso da troca
    }

    @Test
    void emailChangesOnlyAfterNewAddressConfirms() throws Exception {
        Session user = registerUser("Troca");
        String newEmail = "novo." + user.email();

        call(HttpMethod.POST, "/api/me/email", user, Map.of("newEmail", newEmail, "currentPassword", "errada123"), 422);
        call(HttpMethod.POST, "/api/me/email", user, Map.of("newEmail", newEmail, "currentPassword", "senha1234"), 202);

        JsonNode me = get("/api/me", user);
        assertThat(me.get("email").asString()).isEqualTo(user.email());
        assertThat(me.get("pendingEmail").asString()).isEqualTo(newEmail);
        assertThat(emailsSentTo(user.email())).isEqualTo(2); // confirmação do cadastro + aviso da troca

        String token = lastEmailToken(newEmail, "/confirmar-novo-email");
        call(HttpMethod.POST, "/api/auth/confirm-email-change", null, Map.of("token", token), 204);
        call(HttpMethod.POST, "/api/auth/confirm-email-change", null, Map.of("token", token), 422);

        me = get("/api/me", user);
        assertThat(me.get("email").asString()).isEqualTo(newEmail);
        assertThat(me.get("emailVerified").asBoolean()).isTrue();
        assertThat(me.get("pendingEmail").isNull()).isTrue();
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", newEmail, "password", "senha1234"), 200);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "senha1234"), 401);
    }

    @Test
    void cannotTakeAnotherAccountsEmail() throws Exception {
        Session user = registerUser("Troca");
        Session other = registerUser("Outra");
        call(HttpMethod.POST, "/api/me/email", user,
                Map.of("newEmail", other.email(), "currentPassword", "senha1234"), 422);
    }

    @Test
    void emailTakenBeforeConfirmationIsRejected() throws Exception {
        Session user = registerUser("Troca");
        String newEmail = "disputado." + user.email();
        call(HttpMethod.POST, "/api/me/email", user, Map.of("newEmail", newEmail, "currentPassword", "senha1234"), 202);
        String token = lastEmailToken(newEmail, "/confirmar-novo-email");

        call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Rápido", "email", newEmail, "password", "senha1234"), 200);

        call(HttpMethod.POST, "/api/auth/confirm-email-change", null, Map.of("token", token), 422);
        assertThat(get("/api/me", user).get("email").asString()).isEqualTo(user.email());
    }

    @Test
    void cancelingInvalidatesTheLink() throws Exception {
        Session user = registerUser("Troca");
        String newEmail = "cancelado." + user.email();
        call(HttpMethod.POST, "/api/me/email", user, Map.of("newEmail", newEmail, "currentPassword", "senha1234"), 202);
        String token = lastEmailToken(newEmail, "/confirmar-novo-email");

        call(HttpMethod.DELETE, "/api/me/email", user, null, 204);

        call(HttpMethod.POST, "/api/auth/confirm-email-change", null, Map.of("token", token), 422);
        JsonNode me = get("/api/me", user);
        assertThat(me.get("email").asString()).isEqualTo(user.email());
        assertThat(me.get("pendingEmail").isNull()).isTrue();
    }
}
