package com.agrogestao.agriculture;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class CropTest extends ApiTestSupport {

    private JsonNode create(Session s, String name) throws Exception {
        return post("/api/crops", s, Map.of("name", name));
    }

    @Test
    void ownerCreatesListsArchivesAndDeletesACropNotInUse() throws Exception {
        Session owner = registerOwnerWithProperty("Catálogo");
        JsonNode soja = create(owner, "Soja");
        assertThat(soja.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/crops", owner)).hasSize(1);

        JsonNode archived = post("/api/crops/" + soja.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/crops", owner)).isEmpty();
        assertThat(get("/api/crops?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/crops/" + soja.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();

        call(HttpMethod.DELETE, "/api/crops/" + soja.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/crops", owner)).isEmpty();
    }

    @Test
    void rejectsDuplicateNames() throws Exception {
        Session owner = registerOwnerWithProperty("Duplicada");
        create(owner, "Milho");
        call(HttpMethod.POST, "/api/crops", owner, Map.of("name", " milho "), 422);
    }

    @Test
    void cannotDeleteACropAlreadyUsedByACropCycle() throws Exception {
        Session owner = registerOwnerWithProperty("EmUso");
        JsonNode soja = create(owner, "Soja");
        Long fieldId = post("/api/fields", owner, Map.of("name", "Talhão 1", "areaHa", 10)).get("id").asLong();
        post("/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", soja.get("id").asLong(),
                "plannedPlantingDate", "2026-10-01"));

        call(HttpMethod.DELETE, "/api/crops/" + soja.get("id").asLong(), owner, null, 422);
        // Mas pode ser arquivada mesmo em uso.
        JsonNode archived = post("/api/crops/" + soja.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
    }

    @Test
    void workerCreatesButOnlyManagersArchiveOrDelete() throws Exception {
        Session owner = registerOwnerWithProperty("Papéis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode soja = create(worker, "Soja");
        call(HttpMethod.POST, "/api/crops", viewer, Map.of("name", "Milho"), 403);
        call(HttpMethod.POST, "/api/crops/" + soja.get("id").asLong() + "/archive", worker, null, 403);
        post("/api/crops/" + soja.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void catalogIsIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        JsonNode soja = create(owner, "Soja");

        assertThat(get("/api/crops", other)).isEmpty();
        call(HttpMethod.POST, "/api/crops/" + soja.get("id").asLong() + "/archive", other, null, 404);
    }
}
