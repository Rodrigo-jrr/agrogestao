package com.agrogestao.machinery;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class MachineTest extends ApiTestSupport {

    private long type(Session s, String name) throws Exception {
        return post("/api/machine-types", s, Map.of("name", name)).get("id").asLong();
    }

    @Test
    void ownerCreatesListsEditsAndArchivesAMachine() throws Exception {
        Session owner = registerOwnerWithProperty("Máquinas");
        long tratorType = type(owner, "Trator");

        JsonNode machine = post("/api/machines", owner, Map.of("name", "Trator 1", "machineTypeId", tratorType,
                "identification", "ABC-1234", "measurementType", "HOROMETRO", "currentReading", 100));
        assertThat(machine.get("machineTypeName").asString()).isEqualTo("Trator");
        assertThat(machine.get("currentReading").asDouble()).isEqualTo(100.0);
        assertThat(machine.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/machines", owner)).hasSize(1);

        JsonNode updated = call(HttpMethod.PUT, "/api/machines/" + machine.get("id").asLong(), owner,
                Map.of("name", "Trator 1 - John Deere", "machineTypeId", tratorType, "identification", "XYZ-9999",
                        "measurementType", "HOROMETRO"), 200);
        assertThat(updated.get("name").asString()).isEqualTo("Trator 1 - John Deere");
        assertThat(updated.get("identification").asString()).isEqualTo("XYZ-9999");
        // Editar o cadastro não muda a leitura, mesmo sem mandar currentReading.
        assertThat(updated.get("currentReading").asDouble()).isEqualTo(100.0);

        JsonNode archived = post("/api/machines/" + machine.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/machines", owner)).isEmpty();
        assertThat(get("/api/machines?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/machines/" + machine.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();

        call(HttpMethod.DELETE, "/api/machines/" + machine.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/machines", owner)).isEmpty();
    }

    @Test
    void updatesReadingButNotBackwards() throws Exception {
        Session owner = registerOwnerWithProperty("Leitura");
        long tratorType = type(owner, "Trator");
        JsonNode machine = post("/api/machines", owner, Map.of("name", "Trator 1", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO", "currentReading", 100));

        JsonNode updated = post("/api/machines/" + machine.get("id").asLong() + "/reading", owner, Map.of("reading", 150));
        assertThat(updated.get("currentReading").asDouble()).isEqualTo(150.0);

        call(HttpMethod.POST, "/api/machines/" + machine.get("id").asLong() + "/reading", owner,
                Map.of("reading", 140), 422);
    }

    @Test
    void rejectsDuplicateNamesAndArchivedOrInvalidType() throws Exception {
        Session owner = registerOwnerWithProperty("Validação");
        long tratorType = type(owner, "Trator");
        post("/api/machines", owner, Map.of("name", "Trator 1", "machineTypeId", tratorType, "measurementType", "HOROMETRO"));

        call(HttpMethod.POST, "/api/machines", owner, Map.of("name", " trator 1 ", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO"), 422);
        call(HttpMethod.POST, "/api/machines", owner, Map.of("name", "Trator 2", "machineTypeId", 999999,
                "measurementType", "HOROMETRO"), 422);

        post("/api/machine-types/" + tratorType + "/archive", owner, null);
        call(HttpMethod.POST, "/api/machines", owner, Map.of("name", "Trator 2", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO"), 422);
    }

    @Test
    void workerCreatesButOnlyManagersArchiveOrDelete() throws Exception {
        Session owner = registerOwnerWithProperty("Papéis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");
        long tratorType = type(owner, "Trator");

        JsonNode machine = post("/api/machines", worker, Map.of("name", "Trator 1", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO"));
        call(HttpMethod.POST, "/api/machines", viewer, Map.of("name", "Trator 2", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO"), 403);
        call(HttpMethod.POST, "/api/machines/" + machine.get("id").asLong() + "/archive", worker, null, 403);
        call(HttpMethod.DELETE, "/api/machines/" + machine.get("id").asLong(), worker, null, 403);
        post("/api/machines/" + machine.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void machinesAreIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        long tratorType = type(owner, "Trator");
        JsonNode machine = post("/api/machines", owner, Map.of("name", "Trator 1", "machineTypeId", tratorType,
                "measurementType", "HOROMETRO"));

        assertThat(get("/api/machines", other)).isEmpty();
        call(HttpMethod.PUT, "/api/machines/" + machine.get("id").asLong(), other,
                Map.of("name", "Outro nome", "machineTypeId", tratorType, "measurementType", "HOROMETRO"), 404);
        call(HttpMethod.POST, "/api/machines/" + machine.get("id").asLong() + "/archive", other, null, 404);
        call(HttpMethod.DELETE, "/api/machines/" + machine.get("id").asLong(), other, null, 404);
        // Tipo de outra propriedade não serve para criar máquina.
        long otherType = type(other, "Trator do Outro");
        call(HttpMethod.POST, "/api/machines", owner, Map.of("name", "Trator 2", "machineTypeId", otherType,
                "measurementType", "HOROMETRO"), 422);
    }
}
