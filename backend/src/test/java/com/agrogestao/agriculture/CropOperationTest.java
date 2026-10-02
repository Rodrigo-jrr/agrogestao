package com.agrogestao.agriculture;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class CropOperationTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();
    private Session owner;
    private long fieldId;
    private long cropId;
    private long cropCycleId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Operações");
        fieldId = post("/api/fields", owner, Map.of("name", "Talhão 1", "areaHa", 10)).get("id").asLong();
        cropId = post("/api/crops", owner, Map.of("name", "Soja")).get("id").asLong();
        cropCycleId = post("/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", TODAY.toString())).get("id").asLong();
    }

    @Test
    void stockInputDeductsQuantityAtAverageCostAndUndoReturnsIt() throws Exception {
        long herbicide = post("/api/inventory/products", owner, Map.of("name", "Glifosato", "unit", "L", "minStock", 0))
                .get("id").asLong();
        post("/api/inventory/movements", owner, Map.of("productId", herbicide, "type", "IN", "quantity", 100,
                "unitCost", 20, "movementDate", TODAY.toString()));

        JsonNode operation = post("/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PULVERIZACAO",
                "operationDate", TODAY.toString(), "productId", herbicide, "quantity", 10, "responsible", "Zé",
                "machine", "Pulverizador 1"));

        assertThat(operation.get("quantity").asDouble()).isEqualTo(10.0);
        assertThat(operation.get("quantityUnit").asString()).isEqualTo("L");
        assertThat(operation.get("totalCost").asDouble()).isEqualTo(200.0);
        assertThat(operation.get("fieldName").asString()).isEqualTo("Talhão 1");
        assertThat(operation.get("cropName").asString()).isEqualTo("Soja");
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(90.0);

        call(HttpMethod.DELETE, "/api/crop-operations/" + operation.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(100.0);
    }

    @Test
    void notEnoughStockBlocksTheOperation() throws Exception {
        long herbicide = post("/api/inventory/products", owner, Map.of("name", "Glifosato", "unit", "L", "minStock", 0))
                .get("id").asLong();
        post("/api/inventory/movements", owner, Map.of("productId", herbicide, "type", "IN", "quantity", 5,
                "movementDate", TODAY.toString()));

        call(HttpMethod.POST, "/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PULVERIZACAO",
                "operationDate", TODAY.toString(), "productId", herbicide, "quantity", 10), 422);
        assertThat(page("/api/crop-operations", owner)).isEmpty();
    }

    @Test
    void manualProductOrNoProductAtAllAreAllowed() throws Exception {
        JsonNode manual = post("/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "ADUBACAO",
                "operationDate", TODAY.toString(), "productName", "NPK 10-10-10", "quantity", 200,
                "quantityUnit", "kg", "cost", 300));
        assertThat(manual.get("productName").asString()).isEqualTo("NPK 10-10-10");
        assertThat(manual.get("totalCost").asDouble()).isEqualTo(300.0);

        JsonNode noInput = post("/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 150, "machine", "Grade aradora"));
        assertThat(noInput.get("productName").isNull()).isTrue();
        assertThat(noInput.get("machine").asString()).isEqualTo("Grade aradora");

        assertThat(page("/api/crop-operations", owner)).hasSize(2);
    }

    @Test
    void rejectsFutureDateAndArchivedOrInvalidCycle() throws Exception {
        call(HttpMethod.POST, "/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.plusDays(1).toString()), 422);
        call(HttpMethod.POST, "/api/crop-operations", owner, Map.of("cropCycleId", 999999, "kind", "PREPARO",
                "operationDate", TODAY.toString()), 422);

        post("/api/crop-cycles/" + cropCycleId + "/archive", owner, null);
        call(HttpMethod.POST, "/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString()), 422);
    }

    @Test
    void filtersByCycleFieldAndPeriod() throws Exception {
        long field2 = post("/api/fields", owner, Map.of("name", "Talhão 2", "areaHa", 5)).get("id").asLong();
        long cycle2 = post("/api/crop-cycles", owner, Map.of("fieldId", field2, "cropId", cropId,
                "plannedPlantingDate", TODAY.toString())).get("id").asLong();
        post("/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.minusDays(10).toString()));
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycle2, "kind", "PLANTIO",
                "operationDate", TODAY.toString()));

        assertThat(page("/api/crop-operations", owner)).hasSize(2);
        assertThat(page("/api/crop-operations?cropCycleId=" + cropCycleId, owner)).hasSize(1);
        assertThat(page("/api/crop-operations?fieldId=" + field2, owner)).hasSize(1);
        assertThat(page("/api/crop-operations?from=" + TODAY, owner)).hasSize(1);
    }

    @Test
    void workerRegistersButOnlyManagersDelete() throws Exception {
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode operation = post("/api/crop-operations", worker, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString()));
        call(HttpMethod.POST, "/api/crop-operations", viewer, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString()), 403);
        call(HttpMethod.DELETE, "/api/crop-operations/" + operation.get("id").asLong(), worker, null, 403);
        call(HttpMethod.DELETE, "/api/crop-operations/" + operation.get("id").asLong(), owner, null, 204);
    }

    @Test
    void operationsAreIsolatedByProperty() throws Exception {
        Session other = registerOwnerWithProperty("Outro");
        JsonNode operation = post("/api/crop-operations", owner, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString()));

        assertThat(page("/api/crop-operations", other)).isEmpty();
        call(HttpMethod.DELETE, "/api/crop-operations/" + operation.get("id").asLong(), other, null, 404);
        call(HttpMethod.POST, "/api/crop-operations", other, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString()), 422);
    }
}
