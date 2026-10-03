package com.agrogestao.machinery;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class MachineTypeTest extends ApiTestSupport {

    private JsonNode create(Session s, String name) throws Exception {
        return post("/api/machine-types", s, Map.of("name", name));
    }

    @Test
    void ownerCreatesListsArchivesAndDeletesATypeNotInUse() throws Exception {
        Session owner = registerOwnerWithProperty("Catálogo");
        JsonNode trator = create(owner, "Trator");
        assertThat(trator.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/machine-types", owner)).hasSize(1);

        JsonNode archived = post("/api/machine-types/" + trator.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/machine-types", owner)).isEmpty();
        assertThat(get("/api/machine-types?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/machine-types/" + trator.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();

        call(HttpMethod.DELETE, "/api/machine-types/" + trator.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/machine-types", owner)).isEmpty();
    }

    @Test
    void rejectsDuplicateNames() throws Exception {
        Session owner = registerOwnerWithProperty("Duplicado");
        create(owner, "Trator");
        call(HttpMethod.POST, "/api/machine-types", owner, Map.of("name", " trator "), 422);
    }

    @Test
    void cannotDeleteATypeAlreadyUsedByAMachine() throws Exception {
        Session owner = registerOwnerWithProperty("EmUso");
        JsonNode trator = create(owner, "Trator");
        post("/api/machines", owner, Map.of("name", "Trator 1", "machineTypeId", trator.get("id").asLong(),
                "measurementType", "HOROMETRO"));

        call(HttpMethod.DELETE, "/api/machine-types/" + trator.get("id").asLong(), owner, null, 422);
        JsonNode archived = post("/api/machine-types/" + trator.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
    }

    @Test
    void workerCreatesButOnlyManagersArchiveOrDelete() throws Exception {
        Session owner = registerOwnerWithProperty("Papéis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode trator = create(worker, "Trator");
        call(HttpMethod.POST, "/api/machine-types", viewer, Map.of("name", "Colheitadeira"), 403);
        call(HttpMethod.POST, "/api/machine-types/" + trator.get("id").asLong() + "/archive", worker, null, 403);
        post("/api/machine-types/" + trator.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void catalogIsIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        JsonNode trator = create(owner, "Trator");

        assertThat(get("/api/machine-types", other)).isEmpty();
        call(HttpMethod.POST, "/api/machine-types/" + trator.get("id").asLong() + "/archive", other, null, 404);
    }
}
