package com.agrogestao.agriculture;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class CropHarvestTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate NEXT_MONTH = TODAY.plusMonths(1);
    private Session owner;
    private long fieldId;
    private long cropId;
    private long cropCycleId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Colheita");
        fieldId = post("/api/fields", owner, Map.of("name", "Talhão 1", "areaHa", 10)).get("id").asLong();
        cropId = post("/api/crops", owner, Map.of("name", "Soja")).get("id").asLong();
        cropCycleId = post("/api/crop-cycles", owner, Map.of("fieldId", fieldId, "cropId", cropId,
                "plannedPlantingDate", TODAY.toString())).get("id").asLong();
    }

    @Test
    void stockHarvestCreditsStockAndUndoReversesIt() throws Exception {
        long product = post("/api/inventory/products", owner, Map.of("name", "Soja em grão", "unit", "sc", "minStock", 0))
                .get("id").asLong();

        JsonNode harvest = post("/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 500, "quantityUnit", "sc", "destination", "ESTOQUE",
                "productId", product, "unitCost", 60));

        assertThat(harvest.get("quantity").asDouble()).isEqualTo(500.0);
        assertThat(harvest.get("quantityUnit").asString()).isEqualTo("sc");
        assertThat(harvest.get("productName").asString()).isEqualTo("Soja em grão");
        assertThat(harvest.get("fieldName").asString()).isEqualTo("Talhão 1");
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(500.0);

        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(0.0);
    }

    @Test
    void stockHarvestRequiresProductAndUnitCost() throws Exception {
        long product = post("/api/inventory/products", owner, Map.of("name", "Soja em grão", "unit", "sc", "minStock", 0))
                .get("id").asLong();
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 500, "quantityUnit", "sc", "destination", "ESTOQUE"), 422);
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 500, "quantityUnit", "sc", "destination", "ESTOQUE",
                "productId", product), 422);
    }

    @Test
    void cashSaleRegistersIncomeAndUndoRemovesIt() throws Exception {
        long customer = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();

        JsonNode harvest = post("/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 300, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", customer, "totalAmount", 15000));

        assertThat(harvest.get("totalAmount").asDouble()).isEqualTo(15000.0);
        assertThat(harvest.get("customerName").asString()).isEqualTo("Laticínio");
        assertThat(page("/api/finance/transactions", owner).toString()).contains("Venda de colheita #" + harvest.get("id").asLong());

        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), owner, null, 204);
        assertThat(page("/api/finance/transactions", owner).toString()).doesNotContain("Venda de colheita");
    }

    @Test
    void installmentSaleCreatesReceivablesAndBlocksDeleteAfterSettled() throws Exception {
        long customer = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();

        JsonNode harvest = post("/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 300, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", customer, "totalAmount", 1000, "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", NEXT_MONTH.toString(), "amount", 1000))));

        JsonNode receivables = page("/api/finance/receivables", owner);
        JsonNode installment = find(receivables, harvest.get("id").asLong());
        assertThat(installment.get("amount").asDouble()).isEqualTo(1000.0);

        call(HttpMethod.PATCH, "/api/finance/receivables/" + installment.get("id").asLong() + "/receive", owner, null, 200);
        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), owner, null, 422);
    }

    @Test
    void saleRequiresCustomerAndAmount() throws Exception {
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 300, "quantityUnit", "sc", "destination", "VENDA"), 422);
    }

    @Test
    void rejectsFutureDateAndArchivedOrInvalidCycle() throws Exception {
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.plusDays(1).toString(), "quantity", 1, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", 1, "totalAmount", 10), 422);
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", 999999,
                "harvestDate", TODAY.toString(), "quantity", 1, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", 1, "totalAmount", 10), 422);

        post("/api/crop-cycles/" + cropCycleId + "/archive", owner, null);
        call(HttpMethod.POST, "/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 1, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", 1, "totalAmount", 10), 422);
    }

    @Test
    void workerRegistersButOnlyManagersDelete() throws Exception {
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");
        long customer = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();

        JsonNode harvest = post("/api/crop-harvests", worker, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 10, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", customer, "totalAmount", 500));
        call(HttpMethod.POST, "/api/crop-harvests", viewer, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 10, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", customer, "totalAmount", 500), 403);
        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), worker, null, 403);
        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), owner, null, 204);
    }

    @Test
    void harvestsAreIsolatedByPropertyAndFiltersWork() throws Exception {
        Session other = registerOwnerWithProperty("Outro");
        long customer = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();
        JsonNode harvest = post("/api/crop-harvests", owner, Map.of("cropCycleId", cropCycleId,
                "harvestDate", TODAY.toString(), "quantity", 10, "quantityUnit", "sc", "destination", "VENDA",
                "customerId", customer, "totalAmount", 500));

        assertThat(page("/api/crop-harvests", other)).isEmpty();
        call(HttpMethod.DELETE, "/api/crop-harvests/" + harvest.get("id").asLong(), other, null, 404);

        assertThat(page("/api/crop-harvests", owner)).hasSize(1);
        assertThat(page("/api/crop-harvests?cropCycleId=" + cropCycleId, owner)).hasSize(1);
        assertThat(page("/api/crop-harvests?fieldId=" + fieldId, owner)).hasSize(1);
        assertThat(page("/api/crop-harvests?from=" + TODAY.plusDays(1), owner)).isEmpty();
    }

    private static JsonNode find(JsonNode receivables, long cropHarvestId) {
        for (JsonNode r : receivables) {
            if (!r.get("cropHarvestId").isNull() && r.get("cropHarvestId").asLong() == cropHarvestId) {
                return r;
            }
        }
        throw new AssertionError("parcela da colheita #" + cropHarvestId + " não encontrada");
    }
}
