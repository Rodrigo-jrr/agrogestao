package com.agrogestao.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class CashFlowAndSettlementTest extends ApiTestSupport {

    private Session owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
    }

    @Test
    void cashFlowAccumulatesRealizedAndProjectedBalances() throws Exception {
        post("/api/finance/transactions", owner, Map.of("type", "INCOME", "description", "Saldo anterior",
                "amount", 500, "transactionDate", "2026-07-15"));
        post("/api/finance/transactions", owner, Map.of("type", "INCOME", "description", "Venda avulsa",
                "amount", 1000, "transactionDate", "2026-08-20"));
        post("/api/finance/transactions", owner, Map.of("type", "EXPENSE", "description", "Diesel",
                "amount", 300, "transactionDate", "2026-08-25"));
        post("/api/finance/payables", owner, Map.of("description", "Energia", "amount", 200, "dueDate", "2026-10-15"));
        post("/api/finance/payables", owner, Map.of("description", "Atrasada", "amount", 50, "dueDate", "2026-06-01"));
        post("/api/finance/receivables", owner, Map.of("description", "Leite", "amount", 1500, "dueDate", "2026-11-05"));

        JsonNode cashFlow = get("/api/finance/cash-flow?from=2026-08&to=2026-11", owner);

        assertThat(cashFlow.get("openingBalance").asDouble()).isEqualTo(500.0);
        assertThat(cashFlow.get("overduePayables").asDouble()).isEqualTo(50.0);
        JsonNode months = cashFlow.get("months");
        assertThat(months).hasSize(4);
        assertThat(months.get(0).get("month").asString()).isEqualTo("2026-08");
        assertThat(months.get(0).get("net").asDouble()).isEqualTo(700.0);
        assertThat(months.get(0).get("balance").asDouble()).isEqualTo(1200.0);
        assertThat(months.get(0).get("projectedBalance").asDouble()).isEqualTo(1150.0);
        assertThat(months.get(2).get("pendingPayables").asDouble()).isEqualTo(200.0);
        assertThat(months.get(3).get("balance").asDouble()).isEqualTo(1200.0);
        assertThat(months.get(3).get("projectedBalance").asDouble()).isEqualTo(2450.0);
    }

    @Test
    void cashFlowRejectsInvalidRanges() throws Exception {
        call(HttpMethod.GET, "/api/finance/cash-flow?from=2026-10&to=2026-08", owner, null, 422);
        call(HttpMethod.GET, "/api/finance/cash-flow?from=2026-01&to=2028-12", owner, null, 422);
    }

    @Test
    void settlingPayableCreatesExpenseAndUndoingRemovesIt() throws Exception {
        long payableId = post("/api/finance/payables", owner,
                Map.of("description", "Energia", "amount", 200, "dueDate", "2026-10-15")).get("id").asLong();

        call(HttpMethod.PATCH, "/api/finance/payables/" + payableId + "/pay", owner, null, 200);
        JsonNode transactions = allTransactions();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).get("type").asString()).isEqualTo("EXPENSE");
        assertThat(transactions.get(0).get("description").asString()).isEqualTo("Pagamento: Energia");

        long transactionId = transactions.get(0).get("id").asLong();
        call(HttpMethod.DELETE, "/api/finance/transactions/" + transactionId, owner, null, 422);
        call(HttpMethod.PUT, "/api/finance/payables/" + payableId, owner,
                Map.of("description", "Energia", "amount", 250, "dueDate", "2026-10-15"), 422);

        call(HttpMethod.PATCH, "/api/finance/payables/" + payableId + "/unpay", owner, null, 200);
        assertThat(allTransactions()).isEmpty();
    }

    @Test
    void settlingReceivableTwiceDoesNotDuplicateIncome() throws Exception {
        long receivableId = post("/api/finance/receivables", owner,
                Map.of("description", "Leite", "amount", 1500, "dueDate", "2026-11-05")).get("id").asLong();

        call(HttpMethod.PATCH, "/api/finance/receivables/" + receivableId + "/receive", owner, null, 200);
        call(HttpMethod.PATCH, "/api/finance/receivables/" + receivableId + "/receive", owner, null, 200);

        JsonNode transactions = allTransactions();
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).get("type").asString()).isEqualTo("INCOME");
    }

    private JsonNode allTransactions() throws Exception {
        return get("/api/finance/transactions?from=2000-01-01&to=2100-12-31", owner);
    }
}
