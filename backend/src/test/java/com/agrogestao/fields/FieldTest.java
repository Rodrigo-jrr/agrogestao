package com.agrogestao.fields;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class FieldTest extends ApiTestSupport {

    private JsonNode create(Session s, String name, double areaHa) throws Exception {
        return post("/api/fields", s, Map.of("name", name, "areaHa", areaHa));
    }

    @Test
    void ownerCreatesListsEditsAndArchivesAField() throws Exception {
        Session owner = registerOwnerWithProperty("Talhoes");
        JsonNode field = create(owner, "Talhão 1", 12.5);
        assertThat(field.get("areaHa").asDouble()).isEqualTo(12.5);
        assertThat(field.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/fields", owner)).hasSize(1);

        JsonNode updated = call(HttpMethod.PUT, "/api/fields/" + field.get("id").asLong(), owner,
                Map.of("name", "Talhão 1", "areaHa", 15, "location", "Fundo da sede", "soilType", "Argiloso"), 200);
        assertThat(updated.get("areaHa").asDouble()).isEqualTo(15.0);
        assertThat(updated.get("location").asString()).isEqualTo("Fundo da sede");
        assertThat(updated.get("soilType").asString()).isEqualTo("Argiloso");

        JsonNode archived = post("/api/fields/" + field.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/fields", owner)).isEmpty();
        assertThat(get("/api/fields?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/fields/" + field.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();
    }

    @Test
    void rejectsDuplicateNamesAndNonPositiveArea() throws Exception {
        Session owner = registerOwnerWithProperty("Duplicado");
        create(owner, "Talhão A", 10);

        call(HttpMethod.POST, "/api/fields", owner, Map.of("name", " talhão a ", "areaHa", 5), 422);
        call(HttpMethod.POST, "/api/fields", owner, Map.of("name", "Talhão B", "areaHa", 0), 400);
        call(HttpMethod.POST, "/api/fields", owner, Map.of("name", "Talhão C", "areaHa", -1), 400);
    }

    @Test
    void workerCreatesButOnlyManagersArchive() throws Exception {
        Session owner = registerOwnerWithProperty("Papeis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode field = create(worker, "Talhão 1", 8);
        call(HttpMethod.POST, "/api/fields", viewer, Map.of("name", "Talhão 2", "areaHa", 3), 403);
        call(HttpMethod.POST, "/api/fields/" + field.get("id").asLong() + "/archive", worker, null, 403);
        post("/api/fields/" + field.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void fieldsAreIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        JsonNode field = create(owner, "Talhão 1", 10);

        assertThat(get("/api/fields", other)).isEmpty();
        call(HttpMethod.PUT, "/api/fields/" + field.get("id").asLong(), other,
                Map.of("name", "Outro nome", "areaHa", 1), 404);
        call(HttpMethod.POST, "/api/fields/" + field.get("id").asLong() + "/archive", other, null, 404);
    }
}
