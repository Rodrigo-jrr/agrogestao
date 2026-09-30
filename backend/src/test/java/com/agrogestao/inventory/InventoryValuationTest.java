package com.agrogestao.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class InventoryValuationTest extends ApiTestSupport {

    private Session owner;
    private long productId;
    private long supplierId;
    private long customerId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
        productId = post("/api/inventory/products", owner, Map.of("name", "Ração", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        supplierId = post("/api/purchases/suppliers", owner, Map.of("name", "Agro Loja")).get("id").asLong();
        customerId = post("/api/sales/customers", owner, Map.of("name", "Vizinho")).get("id").asLong();
    }

    @Test
    void weightedAverageCostAndStockValue() throws Exception {
        purchase(10, 5);
        purchase(10, 7);

        JsonNode product = product();
        assertThat(product.get("currentQuantity").asDouble()).isEqualTo(20.0);
        assertThat(product.get("averageCost").asDouble()).isEqualTo(6.0);
        assertThat(product.get("stockValue").asDouble()).isEqualTo(120.0);
    }

    @Test
    void saleUsesAverageCostAndCancellationsReverseExactly() throws Exception {
        long firstPurchase = purchase(10, 5);
        purchase(10, 7);
        long sale = sell(5, 12);

        JsonNode out = page("/api/inventory/movements?from=2026-09-01&to=2026-09-30", owner).get(0);
        assertThat(out.get("type").asString()).isEqualTo("OUT");
        assertThat(out.get("unitCost").asDouble()).as("saída registra o custo médio, não o preço de venda")
                .isEqualTo(6.0);
        assertThat(out.get("productName").asString()).isEqualTo("Ração");

        post("/api/sales/" + sale + "/cancel", owner, null);
        assertThat(product().get("currentQuantity").asDouble()).isEqualTo(20.0);
        assertThat(product().get("averageCost").asDouble()).isEqualTo(6.0);

        post("/api/purchases/" + firstPurchase + "/cancel", owner, null);
        assertThat(product().get("currentQuantity").asDouble()).isEqualTo(10.0);
        assertThat(product().get("averageCost").asDouble()).as("sobra só a compra de R$ 7").isEqualTo(7.0);
    }

    @Test
    void entryWithoutCostKeepsAverageAndAdjustmentWithCostResetsIt() throws Exception {
        purchase(10, 5);
        movement("IN", 10, null);
        assertThat(product().get("averageCost").asDouble()).isEqualTo(5.0);

        movement("ADJUST", 8, 9.5);
        assertThat(product().get("currentQuantity").asDouble()).isEqualTo(8.0);
        assertThat(product().get("averageCost").asDouble()).isEqualTo(9.5);
    }

    @Test
    void productWithHistoryIsArchivedInsteadOfDeleted() throws Exception {
        purchase(10, 5);

        call(HttpMethod.DELETE, "/api/inventory/products/" + productId, owner, null, 422);
        post("/api/inventory/products/" + productId + "/archive", owner, null);

        assertThat(get("/api/inventory/products", owner)).isEmpty();
        assertThat(get("/api/inventory/products?includeArchived=true", owner).get(0).get("archived").asBoolean()).isTrue();
        call(HttpMethod.POST, "/api/purchases", owner, Map.of("supplierId", supplierId, "purchaseDate", "2026-09-10",
                "items", List.of(Map.of("productId", productId, "quantity", 1, "unitPrice", 1))), 422);
        assertThat(page("/api/inventory/movements?from=2026-09-01&to=2026-09-30", owner)).as("histórico continua")
                .hasSize(1);

        post("/api/inventory/products/" + productId + "/unarchive", owner, null);
        assertThat(get("/api/inventory/products", owner)).hasSize(1);
    }

    @Test
    void productWithoutHistoryCanBeDeleted() throws Exception {
        call(HttpMethod.DELETE, "/api/inventory/products/" + productId, owner, null, 204);
    }

    @Test
    void onlyManagersArchive() throws Exception {
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");
        call(HttpMethod.POST, "/api/inventory/products/" + productId + "/archive", worker, null, 403);
    }

    @Test
    void movementsCanBeFilteredByProductAndPeriod() throws Exception {
        long other = post("/api/inventory/products", owner, Map.of("name", "Sal", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        purchase(10, 5);
        post("/api/inventory/movements", owner, Map.of("productId", other, "type", "IN", "quantity", 3,
                "movementDate", "2026-08-15"));

        assertThat(page("/api/inventory/movements?from=2026-08-01&to=2026-09-30", owner)).hasSize(2);
        assertThat(page("/api/inventory/movements?from=2026-09-01&to=2026-09-30", owner)).hasSize(1);
        assertThat(page("/api/inventory/movements?productId=" + other + "&from=2026-08-01&to=2026-09-30", owner))
                .hasSize(1);
    }

    private JsonNode product() throws Exception {
        return get("/api/inventory/products", owner).get(0);
    }

    private long purchase(int quantity, double unitPrice) throws Exception {
        return post("/api/purchases", owner, Map.of("supplierId", supplierId, "purchaseDate", "2026-09-10",
                "items", List.of(Map.of("productId", productId, "quantity", quantity, "unitPrice", unitPrice))))
                .get("id").asLong();
    }

    private long sell(int quantity, double unitPrice) throws Exception {
        return post("/api/sales", owner, Map.of("customerId", customerId, "saleDate", "2026-09-12",
                "items", List.of(Map.of("productId", productId, "quantity", quantity, "unitPrice", unitPrice))))
                .get("id").asLong();
    }

    private void movement(String type, double quantity, Double unitCost) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("productId", productId, "type", type,
                "quantity", quantity, "movementDate", "2026-09-15"));
        if (unitCost != null) {
            body.put("unitCost", unitCost);
        }
        post("/api/inventory/movements", owner, body);
    }
}
