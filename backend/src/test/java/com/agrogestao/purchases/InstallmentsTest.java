package com.agrogestao.purchases;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

/** Compra e venda a prazo: parcelas viram contas a pagar/receber ligadas à operação. */
class InstallmentsTest extends ApiTestSupport {

    private Session owner;
    private long productId;
    private long supplierId;
    private long customerId;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
        productId = post("/api/inventory/products", owner, Map.of("name", "Adubo", "unit", "sc", "minStock", 0))
                .get("id").asLong();
        supplierId = post("/api/purchases/suppliers", owner, Map.of("name", "Cooperativa")).get("id").asLong();
        customerId = post("/api/sales/customers", owner, Map.of("name", "Laticínio")).get("id").asLong();
    }

    @Test
    void purchaseInInstallmentsCreatesPayablesInsteadOfCashExpense() throws Exception {
        JsonNode purchase = purchase(10, 30, installments("2026-10-10", 100, "2026-11-10", 100, "2026-12-10", 100));

        assertThat(purchase.get("paymentType").asString()).isEqualTo("INSTALLMENTS");
        assertThat(purchase.get("installmentCount").asInt()).isEqualTo(3);
        assertThat(transactions()).as("a prazo não lança despesa no caixa na hora").isEmpty();

        JsonNode payables = get("/api/finance/payables", owner);
        assertThat(payables).hasSize(3);
        assertThat(payables.get(0).get("description").asString()).isEqualTo("Compra #" + purchase.get("id") + " — parcela 1/3");
        assertThat(payables.get(0).get("purchaseId").asLong()).isEqualTo(purchase.get("id").asLong());
        assertThat(payables.get(2).get("dueDate").asString()).isEqualTo("2026-12-10");

        JsonNode detail = get("/api/purchases/" + purchase.get("id"), owner);
        assertThat(detail.get("supplierName").asString()).isEqualTo("Cooperativa");
        assertThat(detail.get("items").get(0).get("productName").asString()).isEqualTo("Adubo");
        assertThat(detail.get("installments")).hasSize(3);
    }

    @Test
    void singleDueDateIsOneInstallment() throws Exception {
        JsonNode purchase = purchase(2, 50, installments("2026-10-30", 100));
        assertThat(get("/api/finance/payables", owner).get(0).get("description").asString())
                .isEqualTo("Compra #" + purchase.get("id"));
    }

    @Test
    void invalidPlansAreRejected() throws Exception {
        call(HttpMethod.POST, "/api/purchases", owner, purchaseBody(10, 30, installments("2026-10-10", 100, "2026-11-10", 150)), 422);
        call(HttpMethod.POST, "/api/purchases", owner, purchaseBody(10, 30, installments("2026-08-01", 300)), 422);
        call(HttpMethod.POST, "/api/purchases", owner, purchaseBody(10, 30, List.of()), 422);
        Map<String, Object> cashWithInstallments = new java.util.HashMap<>(purchaseBody(10, 30, installments("2026-10-10", 300)));
        cashWithInstallments.put("paymentType", "CASH");
        call(HttpMethod.POST, "/api/purchases", owner, cashWithInstallments, 422);
    }

    @Test
    void cancellingRequiresNoSettledInstallmentAndRemovesThem() throws Exception {
        long id = purchase(10, 30, installments("2026-10-10", 150, "2026-11-10", 150)).get("id").asLong();
        long firstPayable = get("/api/finance/payables", owner).get(0).get("id").asLong();

        call(HttpMethod.PATCH, "/api/finance/payables/" + firstPayable + "/pay", owner, null, 200);
        call(HttpMethod.POST, "/api/purchases/" + id + "/cancel", owner, null, 422);

        call(HttpMethod.PATCH, "/api/finance/payables/" + firstPayable + "/unpay", owner, null, 200);
        post("/api/purchases/" + id + "/cancel", owner, null);

        assertThat(get("/api/finance/payables", owner)).isEmpty();
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isZero();
    }

    @Test
    void installmentCanBeRescheduledButNotResizedOrDeleted() throws Exception {
        purchase(10, 30, installments("2026-10-10", 150, "2026-11-10", 150));
        JsonNode payable = get("/api/finance/payables", owner).get(0);
        String path = "/api/finance/payables/" + payable.get("id").asLong();

        call(HttpMethod.PUT, path, owner, Map.of("description", "Parcela renegociada", "amount", 150,
                "dueDate", "2026-10-20", "supplierId", supplierId), 200);
        call(HttpMethod.PUT, path, owner, Map.of("description", "x", "amount", 100,
                "dueDate", "2026-10-20", "supplierId", supplierId), 422);
        call(HttpMethod.DELETE, path, owner, null, 422);
    }

    @Test
    void saleInInstallmentsCreatesReceivablesAndDetailShowsMargin() throws Exception {
        purchase(10, 30, null);
        JsonNode sale = post("/api/sales", owner, Map.of("customerId", customerId, "saleDate", "2026-09-20",
                "items", List.of(Map.of("productId", productId, "quantity", 4, "unitPrice", 50)),
                "paymentType", "INSTALLMENTS", "installments", installments("2026-10-20", 100, "2026-11-20", 100)));

        assertThat(get("/api/finance/receivables", owner)).hasSize(2);

        JsonNode detail = get("/api/sales/" + sale.get("id"), owner);
        assertThat(detail.get("customerName").asString()).isEqualTo("Laticínio");
        assertThat(detail.get("totalCost").asDouble()).isEqualTo(120.0);
        assertThat(detail.get("margin").asDouble()).isEqualTo(80.0);
        assertThat(detail.get("items").get(0).get("unitCost").asDouble()).isEqualTo(30.0);
    }

    @Test
    void cashFlowProjectsPendingInstallments() throws Exception {
        LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(10);
        post("/api/purchases", owner, Map.of("supplierId", supplierId, "purchaseDate", LocalDate.now().toString(),
                "items", List.of(Map.of("productId", productId, "quantity", 1, "unitPrice", 500)),
                "paymentType", "INSTALLMENTS", "installments", List.of(Map.of("dueDate", nextMonth.toString(), "amount", 500))));

        JsonNode cashFlow = get("/api/finance/cash-flow", owner);
        String month = nextMonth.toString().substring(0, 7);
        for (JsonNode m : cashFlow.get("months")) {
            if (m.get("month").asString().equals(month)) {
                assertThat(m.get("pendingPayables").asDouble()).isEqualTo(500.0);
                return;
            }
        }
        throw new AssertionError("mês " + month + " não encontrado no fluxo de caixa");
    }

    private JsonNode transactions() throws Exception {
        LocalDate today = LocalDate.now();
        return get("/api/finance/transactions?from=" + today.minusYears(1) + "&to=" + today.plusYears(1), owner);
    }

    private JsonNode purchase(int quantity, double unitPrice, List<Map<String, Object>> installments) throws Exception {
        return post("/api/purchases", owner, purchaseBody(quantity, unitPrice, installments));
    }

    private Map<String, Object> purchaseBody(int quantity, double unitPrice, List<Map<String, Object>> installments) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("supplierId", supplierId, "purchaseDate", "2026-09-10",
                "items", List.of(Map.of("productId", productId, "quantity", quantity, "unitPrice", unitPrice))));
        if (installments != null) {
            body.put("paymentType", "INSTALLMENTS");
            body.put("installments", installments);
        }
        return body;
    }

    private static List<Map<String, Object>> installments(Object... dateAmountPairs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < dateAmountPairs.length; i += 2) {
            list.add(Map.of("dueDate", dateAmountPairs[i], "amount", dateAmountPairs[i + 1]));
        }
        return list;
    }
}
