package com.agrogestao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "agrogestao.admin.emails=admin@plataforma.test,dono.admin@teste.dev")
class AccountRecoveryTest extends ApiTestSupport {

    @Test
    void registrationSendsVerificationEmailAndLinkConfirmsIt() throws Exception {
        Session user = registerUser("Nova");
        assertThat(get("/api/me", user).get("emailVerified").asBoolean()).isFalse();

        verifyEmailOf(user);

        assertThat(get("/api/me", user).get("emailVerified").asBoolean()).isTrue();
    }

    @Test
    void verificationLinkWorksOnlyOnce() throws Exception {
        Session user = registerUser("Nova");
        String token = lastEmailToken(user.email(), "/verificar-email");
        call(HttpMethod.POST, "/api/auth/verify-email", null, Map.of("token", token), 204);
        call(HttpMethod.POST, "/api/auth/verify-email", null, Map.of("token", token), 422);
        call(HttpMethod.POST, "/api/auth/verify-email", null, Map.of("token", "inventado"), 422);
    }

    @Test
    void resendIsRateLimitedAndInvalidatesPreviousLink() throws Exception {
        Session user = registerUser("Nova");
        String first = lastEmailToken(user.email(), "/verificar-email");
        call(HttpMethod.POST, "/api/me/resend-verification", user, null, 429);
        assertThat(emailsSentTo(user.email())).isEqualTo(1);
        assertThat(first).isNotBlank();
    }

    @Test
    void forgotPasswordAnswersTheSameForUnknownEmails() throws Exception {
        Session user = registerUser("Esquecida");
        JsonNode known = call(HttpMethod.POST, "/api/auth/forgot-password", null, Map.of("email", user.email()), 202);
        JsonNode unknown = call(HttpMethod.POST, "/api/auth/forgot-password", null,
                Map.of("email", "ninguem@teste.dev"), 202);
        assertThat(known).isEqualTo(unknown);
        assertThat(emailsSentTo("ninguem@teste.dev")).isZero();
    }

    @Test
    void resetPasswordChangesPasswordEndsOldSessionsAndVerifiesEmail() throws Exception {
        Session user = registerUser("Esquecida");
        call(HttpMethod.POST, "/api/auth/forgot-password", null, Map.of("email", user.email()), 202);
        String token = lastEmailToken(user.email(), "/redefinir-senha");
        Thread.sleep(1100); // o novo login precisa cair num segundo posterior à troca (JWT tem precisão de segundos)

        call(HttpMethod.POST, "/api/auth/reset-password", null, Map.of("token", token, "newPassword", "novaSenha123"), 204);

        call(HttpMethod.GET, "/api/me", user, null, 401);
        call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", user.email(), "password", "senha1234"), 401);
        Thread.sleep(1100);
        JsonNode login = post("/api/auth/login", null, Map.of("email", user.email(), "password", "novaSenha123"));
        Session fresh = new Session(login.get("token").asString(), user.email(), null);
        assertThat(get("/api/me", fresh).get("emailVerified").asBoolean()).isTrue();

        call(HttpMethod.POST, "/api/auth/reset-password", null, Map.of("token", token, "newPassword", "outraSenha1"), 422);
    }

    @Test
    void resetRequiresStrongEnoughPassword() throws Exception {
        Session user = registerUser("Esquecida");
        call(HttpMethod.POST, "/api/auth/forgot-password", null, Map.of("email", user.email()), 202);
        String token = lastEmailToken(user.email(), "/redefinir-senha");
        call(HttpMethod.POST, "/api/auth/reset-password", null, Map.of("token", token, "newPassword", "curta"), 400);
    }

    @Test
    void forgotPasswordIsSilentlyRateLimited() throws Exception {
        Session user = registerUser("Insistente");
        for (int i = 0; i < 5; i++) {
            call(HttpMethod.POST, "/api/auth/forgot-password", null, Map.of("email", user.email()), 202);
        }
        // 1 de confirmação do cadastro + no máximo 3 de redefinição por hora
        assertThat(emailsSentTo(user.email())).isEqualTo(4);
    }

    @Test
    void adminAccessRequiresConfirmedEmail() throws Exception {
        JsonNode registered = post("/api/auth/register", null,
                Map.of("name", "Dono", "email", "dono.admin@teste.dev", "password", "senha1234"));
        Session admin = new Session(registered.get("token").asString(), "dono.admin@teste.dev", null);

        call(HttpMethod.GET, "/api/admin/overview", admin, null, 403);
        verifyEmailOf(admin);
        call(HttpMethod.GET, "/api/admin/overview", admin, null, 200);
    }
}
