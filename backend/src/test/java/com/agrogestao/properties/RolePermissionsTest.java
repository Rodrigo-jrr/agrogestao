package com.agrogestao.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/** Papéis na propriedade: VIEWER só lê, WORKER cria/edita, OWNER/MANAGER excluem e gerenciam a equipe. */
class RolePermissionsTest extends ApiTestSupport {

    private static final Map<String, Object> ACTIVITY = Map.of("name", "Milho", "type", "AGRICULTURA");

    private Session owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        Session viewer = addMember(owner, registerUser("Visitante"), "VIEWER");
        assertThat(get("/api/activities", viewer)).hasSize(1);
        call(HttpMethod.POST, "/api/activities", viewer, ACTIVITY, 403);
    }

    @Test
    void workerCanCreateAndEditButNotDelete() throws Exception {
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");
        long id = post("/api/activities", worker, ACTIVITY).get("id").asLong();
        call(HttpMethod.PUT, "/api/activities/" + id, worker, Map.of("name", "Milho safrinha", "type", "AGRICULTURA"), 200);
        call(HttpMethod.DELETE, "/api/activities/" + id, worker, null, 403);
        call(HttpMethod.DELETE, "/api/activities/" + id, owner, null, 204);
    }

    @Test
    void managerCanDeleteAndManageNonOwners() throws Exception {
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        long id = post("/api/activities", manager, ACTIVITY).get("id").asLong();
        call(HttpMethod.DELETE, "/api/activities/" + id, manager, null, 204);

        Session worker = addMember(manager, registerUser("Novato"), "WORKER");
        assertThat(get("/api/activities", worker)).isNotNull();
    }

    @Test
    void workerCannotManageTeam() throws Exception {
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");
        call(HttpMethod.POST, "/api/properties/current/members", worker,
                Map.of("email", registerUser("Outro").email(), "role", "VIEWER"), 403);
    }

    @Test
    void onlyOwnersGrantOrRemoveOwnership() throws Exception {
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        call(HttpMethod.POST, "/api/properties/current/members", manager,
                Map.of("email", registerUser("Socio").email(), "role", "OWNER"), 403);

        long ownerMemberId = memberIdOf(owner, owner.email());
        call(HttpMethod.DELETE, "/api/properties/current/members/" + ownerMemberId, manager, null, 403);
        call(HttpMethod.PUT, "/api/properties/current/members/" + ownerMemberId, manager, Map.of("role", "VIEWER"), 403);
    }

    @Test
    void propertyAlwaysKeepsAtLeastOneOwner() throws Exception {
        long ownerMemberId = memberIdOf(owner, owner.email());
        call(HttpMethod.PUT, "/api/properties/current/members/" + ownerMemberId, owner, Map.of("role", "MANAGER"), 422);
        call(HttpMethod.DELETE, "/api/properties/current/members/" + ownerMemberId, owner, null, 422);

        Session partner = addMember(owner, registerUser("Socia"), "OWNER");
        call(HttpMethod.PUT, "/api/properties/current/members/" + ownerMemberId, partner, Map.of("role", "MANAGER"), 200);
    }

    @Test
    void addingMemberRequiresExistingAccountAndNoDuplicates() throws Exception {
        call(HttpMethod.POST, "/api/properties/current/members", owner,
                Map.of("email", "ninguem@teste.dev", "role", "WORKER"), 422);
        Session worker = registerUser("Peao");
        addMember(owner, worker, "WORKER");
        call(HttpMethod.POST, "/api/properties/current/members", owner,
                Map.of("email", worker.email(), "role", "WORKER"), 422);
    }

    @Test
    void removedMemberLosesAccess() throws Exception {
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");
        call(HttpMethod.DELETE, "/api/properties/current/members/" + memberIdOf(owner, worker.email()), owner, null, 204);
        call(HttpMethod.GET, "/api/activities", worker, null, 403);
    }

    private long memberIdOf(Session session, String email) throws Exception {
        for (JsonNode member : get("/api/properties/current/members", session)) {
            if (email.equals(member.get("email").asText())) {
                return member.get("id").asLong();
            }
        }
        throw new AssertionError("membro não encontrado: " + email);
    }
}
