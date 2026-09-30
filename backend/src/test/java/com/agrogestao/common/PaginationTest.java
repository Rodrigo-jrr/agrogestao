package com.agrogestao.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class PaginationTest extends ApiTestSupport {

    private static final String SEPT = "from=2026-09-01&to=2026-09-30";

    private Session owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
        transaction("INCOME", "Venda de leite", 100, "2026-09-01");
        transaction("INCOME", "Leite cooperativa", 200, "2026-09-05");
        transaction("EXPENSE", "Ração", 50, "2026-09-10");
        transaction("EXPENSE", "Energia 100% renovável", 30, "2026-09-15");
        transaction("EXPENSE", "Diesel", 20, "2026-09-20");
    }

    @Test
    void pagesCarryTotalsAndMostRecentFirst() throws Exception {
        JsonNode first = get("/api/finance/transactions?" + SEPT + "&size=2", owner);
        assertThat(first.get("totalElements").asLong()).isEqualTo(5);
        assertThat(first.get("totalPages").asInt()).isEqualTo(3);
        assertThat(first.get("content")).hasSize(2);
        assertThat(first.get("content").get(0).get("description").asString()).isEqualTo("Diesel");

        JsonNode last = get("/api/finance/transactions?" + SEPT + "&size=2&page=2", owner);
        assertThat(last.get("content")).hasSize(1);
        assertThat(last.get("content").get(0).get("description").asString()).isEqualTo("Venda de leite");
    }

    @Test
    void searchIsCaseInsensitiveAndTreatsWildcardsLiterally() throws Exception {
        assertThat(page("/api/finance/transactions?" + SEPT + "&q=LEITE", owner)).hasSize(2);
        assertThat(page("/api/finance/transactions?" + SEPT + "&q=100%25", owner)).hasSize(1);
        assertThat(page("/api/finance/transactions?" + SEPT + "&q=%25", owner)).hasSize(1);
        assertThat(page("/api/finance/transactions?" + SEPT + "&type=EXPENSE", owner)).hasSize(3);
    }

    @Test
    void totalsCoverTheWholePeriodNotJustThePage() throws Exception {
        JsonNode totals = get("/api/finance/transactions/totals?" + SEPT, owner);
        assertThat(totals.get("income").asDouble()).isEqualTo(300.0);
        assertThat(totals.get("expense").asDouble()).isEqualTo(100.0);
        assertThat(totals.get("result").asDouble()).isEqualTo(200.0);
    }

    @Test
    void pageSizeIsCappedAndInvalidQueriesRejected() throws Exception {
        assertThat(get("/api/finance/transactions?" + SEPT + "&size=5000", owner).get("size").asInt()).isEqualTo(200);
        call(HttpMethod.GET, "/api/finance/transactions?" + SEPT + "&q=" + "x".repeat(101), owner, null, 400);
    }

    @Test
    void accountsCanBeFilteredByStatus() throws Exception {
        long first = post("/api/finance/payables", owner, Map.of("description", "Energia", "amount", 10, "dueDate", "2026-10-01"))
                .get("id").asLong();
        post("/api/finance/payables", owner, Map.of("description", "Água", "amount", 10, "dueDate", "2026-10-02"));
        call(HttpMethod.PATCH, "/api/finance/payables/" + first + "/pay", owner, null, 200);

        assertThat(page("/api/finance/payables?status=OPEN", owner)).hasSize(1);
        assertThat(page("/api/finance/payables?status=SETTLED", owner)).hasSize(1);
        assertThat(page("/api/finance/payables", owner)).hasSize(2);
    }

    @Test
    void purchasesCanBeFilteredBySupplierAndProductionBySearch() throws Exception {
        long product = post("/api/inventory/products", owner, Map.of("name", "Sal", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        long a = post("/api/purchases/suppliers", owner, Map.of("name", "A")).get("id").asLong();
        long b = post("/api/purchases/suppliers", owner, Map.of("name", "B")).get("id").asLong();
        for (long supplier : List.of(a, b, b)) {
            post("/api/purchases", owner, Map.of("supplierId", supplier, "purchaseDate", "2026-09-10",
                    "items", List.of(Map.of("productId", product, "quantity", 1, "unitPrice", 1))));
        }
        assertThat(page("/api/purchases?supplierId=" + b, owner)).hasSize(2);

        post("/api/production", owner, Map.of("productName", "Leite", "quantity", 10, "unit", "L", "productionDate", "2026-09-01"));
        post("/api/production", owner, Map.of("productName", "Ovos", "quantity", 30, "unit", "dz", "productionDate", "2026-09-02"));
        assertThat(page("/api/production?q=ovo", owner)).hasSize(1);
        assertThat(page("/api/production", owner)).hasSize(2);
    }

    private void transaction(String type, String description, double amount, String date) throws Exception {
        post("/api/finance/transactions", owner, Map.of("type", type, "description", description, "amount", amount,
                "transactionDate", date));
    }
}
