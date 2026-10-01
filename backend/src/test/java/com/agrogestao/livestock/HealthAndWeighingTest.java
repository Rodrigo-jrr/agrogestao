package com.agrogestao.livestock;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class HealthAndWeighingTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();
    private Session owner;
    private long batchId;
    private long cow1;
    private long cow2;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Manejo");
        batchId = post("/api/livestock/batches", owner, Map.of("name", "Recria", "species", "BOVINO",
                "tracking", "INDIVIDUAL")).get("id").asLong();
        cow1 = animal("A1", TODAY.minusDays(60), 200);
        cow2 = animal("A2", TODAY.minusDays(60), 210);
    }

    private long animal(String tag, LocalDate entry, double weight) throws Exception {
        return post("/api/livestock/animals", owner, Map.of("tag", tag, "species", "BOVINO", "sex", "MACHO",
                "batchId", batchId, "entryDate", entry.toString(), "weight", weight)).get("id").asLong();
    }

    private Map<String, Object> application(Map<String, Object> fields) {
        Map<String, Object> body = new HashMap<>(Map.of("kind", "VACINA", "date", TODAY.toString()));
        body.putAll(fields);
        return body;
    }

    @Test
    void weighingUpdatesCurrentWeightToTheMostRecent() throws Exception {
        post("/api/livestock/weighings", owner, Map.of("date", TODAY.toString(),
                "entries", List.of(Map.of("animalId", cow1, "weight", 260), Map.of("animalId", cow2, "weight", 275.5))));
        // Pesagem mais antiga lançada depois não muda o peso atual.
        post("/api/livestock/weighings", owner, Map.of("date", TODAY.minusDays(30).toString(),
                "entries", List.of(Map.of("animalId", cow1, "weight", 230))));

        JsonNode detail = get("/api/livestock/animals/" + cow1, owner);
        assertThat(detail.get("animal").get("currentWeight").asDouble()).isEqualTo(260.0);
        long weighings = detail.get("events").findValues("type").stream().filter(t -> t.asString().equals("PESAGEM")).count();
        assertThat(weighings).isEqualTo(2);

        // Excluir a pesagem mais recente devolve o peso anterior.
        long latest = detail.get("events").get(0).get("id").asLong();
        call(HttpMethod.DELETE, "/api/livestock/weighings/" + latest, owner, null, 204);
        assertThat(get("/api/livestock/animals/" + cow1, owner).get("animal").get("currentWeight").asDouble()).isEqualTo(230.0);

        call(HttpMethod.POST, "/api/livestock/weighings", owner, Map.of("date", TODAY.minusDays(90).toString(),
                "entries", List.of(Map.of("animalId", cow1, "weight", 180))), 422); // antes da entrada
    }

    @Test
    void stockVaccineForTheWholeBatchDeductsDoseTimesHeadsAndSplitsCost() throws Exception {
        long vaccine = post("/api/inventory/products", owner, Map.of("name", "Vacina Aftosa", "unit", "mL", "minStock", 0))
                .get("id").asLong();
        post("/api/inventory/movements", owner, Map.of("productId", vaccine, "type", "IN", "quantity", 100,
                "unitCost", 0.5, "movementDate", TODAY.toString()));

        JsonNode applied = post("/api/livestock/health", owner, application(Map.of("batchId", batchId,
                "productId", vaccine, "dose", 5, "withdrawalDays", 30, "nextDate", TODAY.plusDays(3).toString(),
                "nextDescription", "Reforço")));

        assertThat(applied.get("headCount").asInt()).isEqualTo(2);
        assertThat(applied.get("totalQuantity").asDouble()).isEqualTo(10.0);
        assertThat(applied.get("totalCost").asDouble()).isEqualTo(5.0);
        assertThat(applied.get("target").asString()).isEqualTo("lote Recria");
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(90.0);

        JsonNode animalNow = get("/api/livestock/animals/" + cow1, owner).get("animal");
        assertThat(animalNow.get("withdrawalUntil").asString()).isEqualTo(TODAY.plusDays(30).toString());
        JsonNode healthEvent = get("/api/livestock/animals/" + cow1, owner).get("events").get(0);
        assertThat(healthEvent.get("type").asString()).isEqualTo("SANIDADE");
        assertThat(healthEvent.get("amount").asDouble()).isEqualTo(2.5);
        assertThat(healthEvent.get("description").asString()).isEqualTo("Vacina Aftosa (5 mL)");

        JsonNode alerts = get("/api/livestock/alerts", owner);
        assertThat(alerts.get("followUps").get(0).get("nextDescription").asString()).isEqualTo("Reforço");
        assertThat(alerts.get("withdrawals")).hasSize(2);

        // Desfazer devolve o produto ao estoque e tira o evento e a carência.
        call(HttpMethod.DELETE, "/api/livestock/health/" + applied.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/inventory/products", owner).get(0).get("currentQuantity").asDouble()).isEqualTo(100.0);
        assertThat(get("/api/livestock/animals/" + cow1, owner).get("animal").get("withdrawalUntil").isNull()).isTrue();
        assertThat(get("/api/livestock/alerts", owner).get("followUps")).isEmpty();
    }

    @Test
    void notEnoughStockBlocksTheApplication() throws Exception {
        long drug = post("/api/inventory/products", owner, Map.of("name", "Antibiótico", "unit", "mL", "minStock", 0))
                .get("id").asLong();
        post("/api/inventory/movements", owner, Map.of("productId", drug, "type", "IN", "quantity", 5,
                "movementDate", TODAY.toString()));
        call(HttpMethod.POST, "/api/livestock/health", owner, application(Map.of("kind", "MEDICAMENTO",
                "animalIds", List.of(cow1, cow2), "productId", drug, "dose", 3)), 422);
        assertThat(page("/api/livestock/health", owner)).isEmpty();
    }

    @Test
    void manualProductWithFollowUpThatIsDoneByTheNextApplication() throws Exception {
        JsonNode first = post("/api/livestock/health", owner, application(Map.of("kind", "MEDICAMENTO",
                "animalIds", List.of(cow1), "productName", "Ivermectina", "dose", 1, "doseUnit", "mL", "cost", 12,
                "nextDate", TODAY.plusDays(2).toString(), "nextDescription", "Fim do tratamento",
                "responsible", "Dr. Paulo")));
        assertThat(first.get("target").asString()).isEqualTo("animal A1");
        assertThat(get("/api/livestock/alerts", owner).get("followUps")).hasSize(1);

        post("/api/livestock/health", owner, application(Map.of("kind", "MEDICAMENTO", "animalIds", List.of(cow1),
                "productName", "Ivermectina", "followUpOfId", first.get("id").asLong())));
        assertThat(get("/api/livestock/alerts", owner).get("followUps")).isEmpty();

        // Sem produto e sem nome não dá; próxima data antes da aplicação também não.
        call(HttpMethod.POST, "/api/livestock/health", owner, application(Map.of("animalIds", List.of(cow1))), 422);
        call(HttpMethod.POST, "/api/livestock/health", owner, application(Map.of("animalIds", List.of(cow1),
                "productName", "X", "nextDate", TODAY.minusDays(1).toString())), 422);
    }

    @Test
    void overdueFollowUpsStayUntilDismissed() throws Exception {
        JsonNode applied = post("/api/livestock/health", owner, application(Map.of("batchId", batchId,
                "productName", "Brucelose", "date", TODAY.minusDays(20).toString(),
                "nextDate", TODAY.minusDays(1).toString())));
        // Fora da janela de 7 dias, mas não aparece se for daqui a 30 dias.
        post("/api/livestock/health", owner, application(Map.of("batchId", batchId, "productName", "Clostridiose",
                "nextDate", TODAY.plusDays(30).toString())));

        JsonNode followUps = get("/api/livestock/alerts", owner).get("followUps");
        assertThat(followUps).hasSize(1);
        assertThat(followUps.get(0).get("productName").asString()).isEqualTo("Brucelose");

        post("/api/livestock/health/" + applied.get("id").asLong() + "/follow-up-done", owner, null);
        assertThat(get("/api/livestock/alerts", owner).get("followUps")).isEmpty();
    }

    @Test
    void aggregateBatchGetsBatchLevelHealthAndAverageWeight() throws Exception {
        long flock = post("/api/livestock/batches", owner, Map.of("name", "Galpão 2", "species", "AVE",
                "tracking", "AGGREGATE")).get("id").asLong();
        post("/api/livestock/batches/" + flock + "/entries", owner, Map.of("headCount", 1000, "date", TODAY.toString()));

        JsonNode applied = post("/api/livestock/health", owner, application(Map.of("batchId", flock,
                "productName", "Newcastle", "withdrawalDays", 21)));
        assertThat(applied.get("headCount").asInt()).isEqualTo(1000);
        JsonNode weighed = post("/api/livestock/weighings", owner, Map.of("date", TODAY.toString(), "batchId", flock,
                "averageWeight", 1.85, "sampleSize", 50)).get(0);
        assertThat(weighed.get("weight").asDouble()).isEqualTo(1.85);

        JsonNode batches = get("/api/livestock/batches", owner);
        for (JsonNode b : batches) {
            if (b.get("id").asLong() == flock) {
                assertThat(b.get("withdrawalUntil").asString()).isEqualTo(TODAY.plusDays(21).toString());
            }
        }
        call(HttpMethod.DELETE, "/api/livestock/batches/" + flock, owner, null, 422); // tem histórico
    }

    @Test
    void workerRegistersButOnlyManagersUndo() throws Exception {
        Session worker = addMember(owner, registerUser("Vaqueiro"), "WORKER");
        JsonNode applied = post("/api/livestock/health", worker, application(Map.of("animalIds", List.of(cow2),
                "productName", "Raiva")));
        post("/api/livestock/weighings", worker, Map.of("date", TODAY.toString(),
                "entries", List.of(Map.of("animalId", cow2, "weight", 300))));
        call(HttpMethod.DELETE, "/api/livestock/health/" + applied.get("id").asLong(), worker, null, 403);
        long weighing = get("/api/livestock/animals/" + cow2, worker).get("events").get(0).get("id").asLong();
        call(HttpMethod.DELETE, "/api/livestock/weighings/" + weighing, worker, null, 403);

        Session other = registerOwnerWithProperty("Outro");
        call(HttpMethod.DELETE, "/api/livestock/health/" + applied.get("id").asLong(), other, null, 404);
        call(HttpMethod.POST, "/api/livestock/health", other, application(Map.of("animalIds", List.of(cow2),
                "productName", "Raiva")), 422);
    }
}
