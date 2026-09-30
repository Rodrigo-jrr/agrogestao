package com.agrogestao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "agrogestao.security.login.max-failures-per-account=3")
class LoginProtectionTest extends ApiTestSupport {

    @Test
    void accountIsTemporarilyBlockedAfterRepeatedWrongPasswords() throws Exception {
        Session user = registerUser("Alvo");
        for (int i = 0; i < 3; i++) {
            login(user.email(), "errada" + i, 401);
        }

        JsonNode blocked = login(user.email(), "senha1234", 429);

        assertThat(blocked.get("message").asString()).contains("Muitas tentativas").contains("Esqueci minha senha");
    }

    @Test
    void successfulLoginClearsPreviousFailures() throws Exception {
        Session user = registerUser("Distraida");
        login(user.email(), "errada1", 401);
        login(user.email(), "errada2", 401);
        login(user.email(), "senha1234", 200);
        login(user.email(), "errada3", 401);
        login(user.email(), "errada4", 401);
        login(user.email(), "senha1234", 200);
    }

    @Test
    void unknownEmailsAreLimitedTooSoAttackersCannotTellThemApart() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("naoexiste@teste.dev", "x", 401);
        }
        login("naoexiste@teste.dev", "x", 429);
    }

    @Test
    void blockOnOneAccountDoesNotAffectOthers() throws Exception {
        Session target = registerUser("Alvo");
        Session other = registerUser("Outra");
        for (int i = 0; i < 3; i++) {
            login(target.email(), "errada", 401);
        }
        login(other.email(), "senha1234", 200);
    }

    private JsonNode login(String email, String password, int expectedStatus) throws Exception {
        return call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", password), expectedStatus);
    }
}
