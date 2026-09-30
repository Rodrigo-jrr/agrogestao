package com.agrogestao.purchases;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

/** Compras e vendas movimentam estoque e caixa; o cancelamento precisa desfazer os dois. */
class PurchaseSaleCancellationTest extends ApiTestSupport {

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
        customerId = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();
    }

    @Test
    void cancelingSaleReturnsStockAndRemovesIncome() throws Exception {
        long purchaseId = purchase(10);
        long saleId = sell(3);
        assertThat(stock()).isEqualTo(7.0);
        assertThat(transactionDescriptions()).containsExactlyInAnyOrder("Compra #" + purchaseId, "Venda #" + saleId);

        JsonNode canceled = post("/api/sales/" + saleId + "/cancel", owner, null);

        assertThat(canceled.get("canceled").asBoolean()).isTrue();
        assertThat(stock()).isEqualTo(10.0);
        assertThat(transactionDescriptions()).containsExactly("Compra #" + purchaseId);
        call(HttpMethod.POST, "/api/sales/" + saleId + "/cancel", owner, null, 422);
    }

    @Test
    void cannotCancelPurchaseWhoseStockWasAlreadyConsumed() throws Exception {
        long purchaseId = purchase(10);
        sell(3);

        call(HttpMethod.POST, "/api/purchases/" + purchaseId + "/cancel", owner, null, 422);

        assertThat(stock()).isEqualTo(7.0);
        assertThat(page("/api/purchases", owner).get(0).get("canceled").asBoolean()).isFalse();
    }

    @Test
    void cancelingPurchaseRemovesStockAndExpense() throws Exception {
        long purchaseId = purchase(10);

        post("/api/purchases/" + purchaseId + "/cancel", owner, null);

        assertThat(stock()).isEqualTo(0.0);
        assertThat(transactionDescriptions()).isEmpty();
    }

    @Test
    void onlyOwnersAndManagersCancel() throws Exception {
        long purchaseId = purchase(10);
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");
        call(HttpMethod.POST, "/api/purchases/" + purchaseId + "/cancel", worker, null, 403);
    }

    @Test
    void transactionGeneratedByPurchaseCannotBeEditedOrDeletedDirectly() throws Exception {
        purchase(10);
        long transactionId = page("/api/finance/transactions?from=2026-01-01&to=2026-12-31", owner).get(0).get("id").asLong();
        call(HttpMethod.DELETE, "/api/finance/transactions/" + transactionId, owner, null, 422);
        call(HttpMethod.PUT, "/api/finance/transactions/" + transactionId, owner, Map.of("type", "EXPENSE",
                "description", "x", "amount", 1, "transactionDate", "2026-09-10"), 422);
    }

    private long purchase(int quantity) throws Exception {
        return post("/api/purchases", owner, Map.of("supplierId", supplierId, "purchaseDate", "2026-09-10",
                "items", List.of(Map.of("productId", productId, "quantity", quantity, "unitPrice", 5)))).get("id").asLong();
    }

    private long sell(int quantity) throws Exception {
        return post("/api/sales", owner, Map.of("customerId", customerId, "saleDate", "2026-09-12",
                "items", List.of(Map.of("productId", productId, "quantity", quantity, "unitPrice", 9)))).get("id").asLong();
    }

    private double stock() throws Exception {
        return get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble();
    }

    private List<String> transactionDescriptions() throws Exception {
        List<String> descriptions = new ArrayList<>();
        page("/api/finance/transactions?from=2026-01-01&to=2026-12-31", owner)
                .forEach(t -> descriptions.add(t.get("description").asString()));
        return descriptions;
    }
}
