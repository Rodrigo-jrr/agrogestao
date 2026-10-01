package com.agrogestao.livestock;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class LivestockIndicatorsTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();

    private long species(Session s, String name, boolean allowsIndividual) throws Exception {
        return post("/api/livestock/species", s, Map.of("name", name, "allowsIndividual", allowsIndividual))
                .get("id").asLong();
    }

    private long animal(Session s, String tag, String sex, long speciesId, LocalDate entryDate) throws Exception {
        return post("/api/livestock/animals", s, Map.of("tag", tag, "sex", sex, "speciesId", speciesId,
                "entryDate", entryDate.toString())).get("id").asLong();
    }

    @Test
    void costsAreSplitByHeadcountAndLiveWeightAndScopedToTheBatch() throws Exception {
        Session owner = registerOwnerWithProperty("Custos");
        long activityId = post("/api/activities", owner, Map.of("name", "Corte", "type", "PECUARIA")).get("id").asLong();
        long costCenterId = post("/api/finance/cost-centers", owner, Map.of("name", "Ração", "activityId", activityId))
                .get("id").asLong();
        long bovino = species(owner, "Bovino", true);
        long batchId = post("/api/livestock/batches", owner, Map.of("name", "Recria", "speciesId", bovino,
                "tracking", "INDIVIDUAL", "activityId", activityId)).get("id").asLong();
        long supplierId = post("/api/purchases/suppliers", owner, Map.of("name", "Leilão")).get("id").asLong();

        post("/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY.minusDays(60).toString(),
                "totalAmount", 1000, "batchId", batchId, "animals", List.of(
                        Map.of("tag", "A1", "sex", "FEMEA", "weight", 100),
                        Map.of("tag", "A2", "sex", "FEMEA", "weight", 100))));
        post("/api/livestock/health", owner, Map.of("kind", "VACINA", "date", TODAY.minusDays(50).toString(),
                "batchId", batchId, "productName", "Vermífugo", "cost", 100));
        post("/api/finance/transactions", owner, Map.of("type", "EXPENSE", "description", "Ração",
                "amount", 300, "transactionDate", TODAY.minusDays(40).toString(), "costCenterId", costCenterId));

        // Animal fora do lote: entra na contagem da propriedade, mas não na do lote.
        animal(owner, "Solto", "FEMEA", bovino, TODAY.minusDays(10));

        JsonNode batchIndicators = get("/api/livestock/indicators?batchId=" + batchId, owner);
        assertThat(batchIndicators.get("headCount").asLong()).isEqualTo(2);
        assertThat(batchIndicators.get("purchaseCost").asDouble()).isEqualTo(1000.0);
        assertThat(batchIndicators.get("healthCost").asDouble()).isEqualTo(100.0);
        assertThat(batchIndicators.get("generalCost").asDouble()).isEqualTo(300.0);
        assertThat(batchIndicators.get("totalCost").asDouble()).isEqualTo(1400.0);
        assertThat(batchIndicators.get("costPerAnimal").asDouble()).isEqualTo(700.0);
        assertThat(batchIndicators.get("costPerKg").asDouble()).isEqualTo(7.0);

        JsonNode propertyIndicators = get("/api/livestock/indicators", owner);
        assertThat(propertyIndicators.get("headCount").asLong()).isEqualTo(3);
    }

    @Test
    void pregnancyBirthAndMortalityRatesAndThePeriodFilter() throws Exception {
        Session owner = registerOwnerWithProperty("Taxas");
        long bovino = species(owner, "Bovino", true);
        LocalDate longAgo = TODAY.minusDays(500);
        long f1 = animal(owner, "F1", "FEMEA", bovino, longAgo);
        long f2 = animal(owner, "F2", "FEMEA", bovino, longAgo);
        long m1 = animal(owner, "M1", "MACHO", bovino, longAgo);

        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(400).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.minusDays(300).toString()));
        post("/api/livestock/reproduction", owner, Map.of("animalId", f2, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(390).toString(), "pregnancyResult", "NEGATIVO"));
        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "PARTO",
                "date", TODAY.minusDays(300).toString(), "offspring", List.of(
                        Map.of("tag", "C1", "sex", "MACHO"), Map.of("tag", "C2", "sex", "MACHO"))));
        post("/api/livestock/animals/" + m1 + "/exit", owner, Map.of("type", "MORTE", "date", TODAY.minusDays(100).toString()));

        JsonNode all = get("/api/livestock/indicators", owner);
        assertThat(all.get("pregnancyDiagnoses").asInt()).isEqualTo(2);
        assertThat(all.get("positivePregnancyDiagnoses").asInt()).isEqualTo(1);
        assertThat(all.get("pregnancyRate").asDouble()).isEqualTo(50.0);
        assertThat(all.get("births").asInt()).isEqualTo(2);
        assertThat(all.get("birthRate").asDouble()).isEqualTo(100.0); // 2 crias / 2 fêmeas ativas (F1, F2)
        assertThat(all.get("deaths").asInt()).isEqualTo(1);
        assertThat(all.get("headCount").asLong()).isEqualTo(4); // F1, F2, C1, C2 (M1 morreu)
        assertThat(all.get("mortalityRate").asDouble()).isEqualTo(20.0); // 1 / (4 + 1)

        // Período sem nenhum desses eventos: as taxas do período somem, mas a contagem atual não.
        JsonNode narrow = get("/api/livestock/indicators?from=" + TODAY.minusDays(10) + "&to=" + TODAY, owner);
        assertThat(narrow.get("pregnancyDiagnoses").asInt()).isEqualTo(0);
        assertThat(narrow.get("pregnancyRate").isNull()).isTrue();
        assertThat(narrow.get("births").asInt()).isEqualTo(0);
        assertThat(narrow.get("deaths").asInt()).isEqualTo(0);
        assertThat(narrow.get("headCount").asLong()).isEqualTo(4);
    }

    @Test
    void averageCalvingIntervalAndDailyGain() throws Exception {
        Session owner = registerOwnerWithProperty("Ciclo");
        long bovino = species(owner, "Bovino", true);
        long f1 = animal(owner, "F1", "FEMEA", bovino, TODAY.minusDays(800));

        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(700).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.minusDays(600).toString()));
        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "PARTO",
                "date", TODAY.minusDays(600).toString(), "offspring", List.of(Map.of("tag", "X1", "sex", "MACHO"))));
        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(500).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.minusDays(400).toString()));
        post("/api/livestock/reproduction", owner, Map.of("animalId", f1, "kind", "PARTO",
                "date", TODAY.minusDays(350).toString(), "offspring", List.of(Map.of("tag", "X2", "sex", "MACHO"))));

        post("/api/livestock/weighings", owner, Map.of("date", TODAY.minusDays(100).toString(),
                "entries", List.of(Map.of("animalId", f1, "weight", 200))));
        post("/api/livestock/weighings", owner, Map.of("date", TODAY.minusDays(50).toString(),
                "entries", List.of(Map.of("animalId", f1, "weight", 230))));

        JsonNode indicators = get("/api/livestock/indicators", owner);
        assertThat(indicators.get("averageCalvingIntervalDays").asDouble()).isEqualTo(250.0);
        assertThat(indicators.get("averageDailyGain").asDouble()).isEqualTo(0.6);
    }
}
