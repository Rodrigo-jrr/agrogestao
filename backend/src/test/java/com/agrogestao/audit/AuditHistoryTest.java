package com.agrogestao.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class AuditHistoryTest extends ApiTestSupport {

    private static final String AUDIT = "/api/properties/current/audit";

    @Test
    void ownerSeesWhoDidWhatNewestFirst() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        post("/api/activities", worker, Map.of("name", "Soja", "type", "AGRICULTURA"));

        JsonNode entries = page(AUDIT, owner);
        JsonNode newest = entries.get(0);
        assertThat(newest.get("entityName").asString()).isEqualTo("Activity");
        assertThat(newest.get("action").asString()).isEqualTo("CREATE");
        assertThat(newest.get("userName").asString()).isEqualTo("Peão");
        assertThat(newest.get("details").asString()).isEqualTo("Soja");

        List<String> authors = new ArrayList<>();
        get(AUDIT + "/authors", owner).forEach(a -> authors.add(a.get("name").asString()));
        assertThat(authors).containsExactly("Dona", "Peão");
    }

    @Test
    void filtersByPersonAndArea() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        post("/api/activities", worker, Map.of("name", "Soja", "type", "AGRICULTURA"));
        post("/api/finance/transactions", owner, Map.of("type", "EXPENSE", "description", "Diesel", "amount", 80,
                "transactionDate", java.time.LocalDate.now().toString()));
        long workerId = get("/api/me", worker).get("userId").asLong();

        JsonNode byWorker = page(AUDIT + "?userId=" + workerId, owner);
        assertThat(byWorker).hasSize(1);

        JsonNode finance = page(AUDIT + "?entity=FinancialTransaction&entity=AccountPayable", owner);
        assertThat(finance).hasSize(1);
        assertThat(finance.get(0).get("details").asString()).isEqualTo("Despesa: Diesel (R$ 80,00)");

        assertThat(page(AUDIT + "?from=2000-01-01&to=2000-01-31", owner)).isEmpty();
    }

    @Test
    void onlyOwnersSeeTheHistoryOfTheirProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        Session other = registerOwnerWithProperty("Outra");

        call(HttpMethod.GET, AUDIT, manager, null, 403);
        call(HttpMethod.GET, AUDIT + "/authors", manager, null, 403);
        for (JsonNode entry : page(AUDIT, other)) {
            assertThat(entry.get("userName").asString()).isEqualTo("Outra");
        }
    }
}
