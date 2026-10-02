package com.agrogestao.agriculture;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CropIndicatorsTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();

    private long field(Session s, String name, double areaHa) throws Exception {
        return post("/api/fields", s, Map.of("name", name, "areaHa", areaHa)).get("id").asLong();
    }

    private long crop(Session s, String name) throws Exception {
        return post("/api/crops", s, Map.of("name", name)).get("id").asLong();
    }

    private long cycle(Session s, long fieldId, long cropId) throws Exception {
        return post("/api/crop-cycles", s, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", TODAY.toString())).get("id").asLong();
    }

    @Test
    void singleCycleCostProductionRevenueAndMargin() throws Exception {
        Session owner = registerOwnerWithProperty("Indicadores");
        long fieldId = field(owner, "Talhão 1", 10);
        long cropId = crop(owner, "Soja");
        long cycleId = cycle(owner, fieldId, cropId);
        long customer = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();

        post("/api/crop-operations", owner, Map.of("cropCycleId", cycleId, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 1000));
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycleId, "kind", "ADUBACAO",
                "operationDate", TODAY.toString(), "cost", 500));
        post("/api/crop-harvests", owner, Map.of("cropCycleId", cycleId, "harvestDate", TODAY.toString(),
                "quantity", 300, "quantityUnit", "sc", "destination", "VENDA", "customerId", customer,
                "totalAmount", 9000));

        JsonNode indicators = get("/api/crop-indicators?cropCycleId=" + cycleId, owner);
        assertThat(indicators.get("totalAreaHa").asDouble()).isEqualTo(10.0);
        assertThat(indicators.get("totalCost").asDouble()).isEqualTo(1500.0);
        assertThat(indicators.get("costPerHa").asDouble()).isEqualTo(150.0);
        assertThat(indicators.get("harvestedQuantityByUnit").get("sc").asDouble()).isEqualTo(300.0);
        assertThat(indicators.get("productionPerHaByUnit").get("sc").asDouble()).isEqualTo(30.0);
        assertThat(indicators.get("revenue").asDouble()).isEqualTo(9000.0);
        assertThat(indicators.get("revenuePerHa").asDouble()).isEqualTo(900.0);
        assertThat(indicators.get("marginPerHa").asDouble()).isEqualTo(750.0);
        assertThat(indicators.get("cropCycleLabel").asString()).isEqualTo("Soja em Talhão 1");
    }

    @Test
    void stockHarvestsDoNotCountAsRevenue() throws Exception {
        Session owner = registerOwnerWithProperty("Estoque");
        long fieldId = field(owner, "Talhão 1", 5);
        long cropId = crop(owner, "Milho");
        long cycleId = cycle(owner, fieldId, cropId);
        long product = post("/api/inventory/products", owner, Map.of("name", "Milho em grão", "unit", "sc", "minStock", 0))
                .get("id").asLong();

        post("/api/crop-harvests", owner, Map.of("cropCycleId", cycleId, "harvestDate", TODAY.toString(),
                "quantity", 200, "quantityUnit", "sc", "destination", "ESTOQUE", "productId", product, "unitCost", 50));

        JsonNode indicators = get("/api/crop-indicators?cropCycleId=" + cycleId, owner);
        assertThat(indicators.get("harvestedQuantityByUnit").get("sc").asDouble()).isEqualTo(200.0);
        assertThat(indicators.get("revenue").asDouble()).isEqualTo(0.0);
        assertThat(indicators.get("revenuePerHa").asDouble()).isEqualTo(0.0);
    }

    @Test
    void aggregatesByFieldAcrossMultipleCycles() throws Exception {
        Session owner = registerOwnerWithProperty("Talhao");
        long fieldId = field(owner, "Talhão 1", 10);
        long soja = crop(owner, "Soja");
        long milho = crop(owner, "Milho");
        long cycle1 = cycle(owner, fieldId, soja);
        long cycle2 = cycle(owner, fieldId, milho);
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycle1, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 100));
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycle2, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 200));

        JsonNode indicators = get("/api/crop-indicators?fieldId=" + fieldId, owner);
        assertThat(indicators.get("cycleCount").asInt()).isEqualTo(2);
        assertThat(indicators.get("totalAreaHa").asDouble()).isEqualTo(10.0);
        assertThat(indicators.get("totalCost").asDouble()).isEqualTo(300.0);
        assertThat(indicators.get("costPerHa").asDouble()).isEqualTo(30.0);
        assertThat(indicators.get("fieldName").asString()).isEqualTo("Talhão 1");
        assertThat(indicators.get("cropCycleId").isNull()).isTrue();
    }

    @Test
    void aggregatesWholePropertySummingDistinctFieldAreas() throws Exception {
        Session owner = registerOwnerWithProperty("Propriedade");
        long field1 = field(owner, "Talhão 1", 10);
        long field2 = field(owner, "Talhão 2", 5);
        long cropId = crop(owner, "Soja");
        long cycle1 = cycle(owner, field1, cropId);
        long cycle2 = cycle(owner, field2, cropId);
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycle1, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 150));
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycle2, "kind", "PREPARO",
                "operationDate", TODAY.toString(), "cost", 75));

        JsonNode indicators = get("/api/crop-indicators", owner);
        assertThat(indicators.get("cycleCount").asInt()).isEqualTo(2);
        assertThat(indicators.get("totalAreaHa").asDouble()).isEqualTo(15.0);
        assertThat(indicators.get("totalCost").asDouble()).isEqualTo(225.0);
        assertThat(indicators.get("costPerHa").asDouble()).isEqualTo(15.0);
    }

    @Test
    void periodFiltersOperationsAndHarvests() throws Exception {
        Session owner = registerOwnerWithProperty("Periodo");
        long fieldId = field(owner, "Talhão 1", 10);
        long cropId = crop(owner, "Soja");
        long cycleId = cycle(owner, fieldId, cropId);
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycleId, "kind", "PREPARO",
                "operationDate", TODAY.minusDays(30).toString(), "cost", 100));
        post("/api/crop-operations", owner, Map.of("cropCycleId", cycleId, "kind", "ADUBACAO",
                "operationDate", TODAY.toString(), "cost", 50));

        JsonNode fromToday = get("/api/crop-indicators?cropCycleId=" + cycleId + "&from=" + TODAY, owner);
        assertThat(fromToday.get("totalCost").asDouble()).isEqualTo(50.0);

        JsonNode noFilter = get("/api/crop-indicators?cropCycleId=" + cycleId, owner);
        assertThat(noFilter.get("totalCost").asDouble()).isEqualTo(150.0);
    }

    @Test
    void isolatedByPropertyAndRejectsInvalidScope() throws Exception {
        Session owner = registerOwnerWithProperty("Isolado");
        Session other = registerOwnerWithProperty("Outro");
        long fieldId = field(owner, "Talhão 1", 10);
        long cropId = crop(owner, "Soja");
        long cycleId = cycle(owner, fieldId, cropId);

        call(org.springframework.http.HttpMethod.GET, "/api/crop-indicators?cropCycleId=" + cycleId, other, null, 422);
        call(org.springframework.http.HttpMethod.GET, "/api/crop-indicators?fieldId=" + fieldId, other, null, 422);
        assertThat(get("/api/crop-indicators", other).get("cycleCount").asInt()).isEqualTo(0);
    }
}
