package com.agrogestao.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class PeriodFilterTest extends ApiTestSupport {

    private Session owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
        post("/api/finance/transactions", owner, Map.of("type", "INCOME", "description", "Leite julho",
                "amount", 1000, "transactionDate", "2026-07-10"));
        post("/api/finance/transactions", owner, Map.of("type", "EXPENSE", "description", "Ração agosto",
                "amount", 300, "transactionDate", "2026-08-05"));
    }

    @Test
    void dashboardUsesRequestedPeriod() throws Exception {
        JsonNode july = get("/api/dashboard/summary?from=2026-07-01&to=2026-07-31", owner);
        assertThat(july.get("totalIncome").asDouble()).isEqualTo(1000.0);
        assertThat(july.get("totalExpense").asDouble()).isZero();

        JsonNode both = get("/api/dashboard/summary?from=2026-07-01&to=2026-08-31", owner);
        assertThat(both.get("result").asDouble()).isEqualTo(700.0);
        assertThat(both.get("periodFrom").asString()).isEqualTo("2026-07-01");
    }

    @Test
    void transactionsCanBeListedForPastMonths() throws Exception {
        assertThat(page("/api/finance/transactions?from=2026-08-01&to=2026-08-31", owner)).hasSize(1);
        assertThat(page("/api/finance/transactions?from=2026-07-01&to=2026-08-31", owner)).hasSize(2);
    }

    @Test
    void invalidPeriodsAreRejected() throws Exception {
        call(HttpMethod.GET, "/api/dashboard/summary?from=2026-08-31&to=2026-08-01", owner, null, 422);
        call(HttpMethod.GET, "/api/finance/transactions?from=2015-01-01&to=2026-08-01", owner, null, 422);
        call(HttpMethod.GET, "/api/dashboard/summary?from=31-08-2026", owner, null, 400);
    }
}
