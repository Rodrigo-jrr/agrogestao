package com.agrogestao.agriculture;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class CropCycleTest extends ApiTestSupport {

    private Long field(Session s, String name) throws Exception {
        return post("/api/fields", s, Map.of("name", name, "areaHa", 10)).get("id").asLong();
    }

    private Long crop(Session s, String name) throws Exception {
        return post("/api/crops", s, Map.of("name", name)).get("id").asLong();
    }

    @Test
    void ownerCreatesListsAndArchivesACropCycle() throws Exception {
        Session owner = registerOwnerWithProperty("Safras");
        Long fieldId = field(owner, "Talhão 1");
        Long cropId = crop(owner, "Soja");

        JsonNode cycle = post("/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", cropId,
                "variety", "BRS 1010", "plannedPlantingDate", "2026-10-15", "plannedHarvestDate", "2027-02-15"));
        assertThat(cycle.get("fieldName").asString()).isEqualTo("Talhão 1");
        assertThat(cycle.get("cropName").asString()).isEqualTo("Soja");
        assertThat(cycle.get("variety").asString()).isEqualTo("BRS 1010");
        assertThat(cycle.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/crop-cycles", owner)).hasSize(1);

        JsonNode archived = post("/api/crop-cycles/" + cycle.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/crop-cycles", owner)).isEmpty();
        assertThat(get("/api/crop-cycles?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/crop-cycles/" + cycle.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();
    }

    @Test
    void filtersByField() throws Exception {
        Session owner = registerOwnerWithProperty("Filtro");
        Long field1 = field(owner, "Talhão 1");
        Long field2 = field(owner, "Talhão 2");
        Long cropId = crop(owner, "Milho");
        post("/api/crop-cycles", owner, Map.of("fieldId", field1, "cropId", cropId, "plannedPlantingDate", "2026-10-01"));
        post("/api/crop-cycles", owner, Map.of("fieldId", field2, "cropId", cropId, "plannedPlantingDate", "2026-10-01"));

        assertThat(get("/api/crop-cycles", owner)).hasSize(2);
        assertThat(get("/api/crop-cycles?fieldId=" + field1, owner)).hasSize(1);
    }

    @Test
    void rejectsHarvestBeforePlantingAndInvalidFieldOrCrop() throws Exception {
        Session owner = registerOwnerWithProperty("Validação");
        Long fieldId = field(owner, "Talhão 1");
        Long cropId = crop(owner, "Soja");

        call(HttpMethod.POST, "/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", "2026-10-15", "plannedHarvestDate", "2026-10-01"), 422);
        call(HttpMethod.POST, "/api/crop-cycles", owner,
                Map.of("fieldId", 999999, "cropId", cropId, "plannedPlantingDate", "2026-10-15"), 422);
        call(HttpMethod.POST, "/api/crop-cycles", owner,
                Map.of("fieldId", fieldId, "cropId", 999999, "plannedPlantingDate", "2026-10-15"), 422);
    }

    @Test
    void rejectsArchivedFieldOrCrop() throws Exception {
        Session owner = registerOwnerWithProperty("Arquivados");
        Long fieldId = field(owner, "Talhão 1");
        Long cropId = crop(owner, "Soja");
        post("/api/fields/" + fieldId + "/archive", owner, null);

        call(HttpMethod.POST, "/api/crop-cycles", owner,
                Map.of("fieldId", fieldId, "cropId", cropId, "plannedPlantingDate", "2026-10-15"), 422);
    }

    @Test
    void workerCreatesButOnlyManagersArchive() throws Exception {
        Session owner = registerOwnerWithProperty("Papéis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");
        Long fieldId = field(owner, "Talhão 1");
        Long cropId = crop(owner, "Soja");

        JsonNode cycle = post("/api/crop-cycles", worker, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", "2026-10-15"));
        call(HttpMethod.POST, "/api/crop-cycles", viewer,
                Map.of("fieldId", fieldId, "cropId", cropId, "plannedPlantingDate", "2026-10-15"), 403);
        call(HttpMethod.POST, "/api/crop-cycles/" + cycle.get("id").asLong() + "/archive", worker, null, 403);
        post("/api/crop-cycles/" + cycle.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void cropCyclesAreIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        Long fieldId = field(owner, "Talhão 1");
        Long cropId = crop(owner, "Soja");
        JsonNode cycle = post("/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", "2026-10-15"));

        assertThat(get("/api/crop-cycles", other)).isEmpty();
        call(HttpMethod.POST, "/api/crop-cycles/" + cycle.get("id").asLong() + "/archive", other, null, 404);
        // Talhão e cultura de outra propriedade não servem para criar uma safra.
        Long otherFieldId = field(other, "Talhão do Outro");
        Long otherCropId = crop(other, "Soja do Outro");
        call(HttpMethod.POST, "/api/crop-cycles", owner,
                Map.of("fieldId", otherFieldId, "cropId", cropId, "plannedPlantingDate", "2026-10-15"), 422);
        call(HttpMethod.POST, "/api/crop-cycles", owner,
                Map.of("fieldId", fieldId, "cropId", otherCropId, "plannedPlantingDate", "2026-10-15"), 422);
    }
}
