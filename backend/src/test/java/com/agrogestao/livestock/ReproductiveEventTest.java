package com.agrogestao.livestock;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class ReproductiveEventTest extends ApiTestSupport {

    private static final LocalDate TODAY = LocalDate.now();

    private Session owner;
    private long bovino;
    private long cow;
    private long bull;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Cria");
        bovino = post("/api/livestock/species", owner, Map.of("name", "Bovino", "allowsIndividual", true))
                .get("id").asLong();
        LocalDate longAgo = TODAY.minusDays(1000);
        cow = post("/api/livestock/animals", owner,
                Map.of("tag", "V1", "speciesId", bovino, "sex", "FEMEA", "entryDate", longAgo.toString())).get("id").asLong();
        bull = post("/api/livestock/animals", owner,
                Map.of("tag", "T1", "speciesId", bovino, "sex", "MACHO", "entryDate", longAgo.toString())).get("id").asLong();
    }

    @Test
    void fullCycleFromHeatToCalvingCreatesTheCalfAndClosesThePregnancy() throws Exception {
        LocalDate heatDate = TODAY.minusDays(400);
        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "CIO", "date", heatDate.toString()));
        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "COBERTURA",
                "date", heatDate.toString(), "sire", "Touro Valente"));

        LocalDate diagnosisDate = heatDate.plusDays(35);
        LocalDate calvingDate = TODAY.minusDays(100);
        JsonNode diagnosis = post("/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", diagnosisDate.toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", calvingDate.toString()));
        assertThat(diagnosis.get("pregnancyResult").asString()).isEqualTo("POSITIVO");

        // Novo diagnóstico não entra com gestação já em aberto.
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", diagnosisDate.toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", calvingDate.toString()), 422);

        JsonNode birth = post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "PARTO",
                "date", calvingDate.toString(), "offspring", List.of(
                        Map.of("tag", "B1", "sex", "FEMEA", "weight", 32))));
        assertThat(birth.get("offspringCount").asInt()).isEqualTo(1);
        long calfId = birth.get("offspringAnimalIds").get(0).asLong();

        JsonNode calf = get("/api/livestock/animals/" + calfId, owner).get("animal");
        assertThat(calf.get("motherId").asLong()).isEqualTo(cow);
        assertThat(calf.get("birthDate").asString()).isEqualTo(calvingDate.toString());
        assertThat(calf.get("speciesId").asLong()).isEqualTo(bovino);

        // Gestação encerrada: dá para diagnosticar de novo.
        JsonNode newDiagnosis = post("/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", calvingDate.plusDays(60).toString(), "pregnancyResult", "NEGATIVO"));
        assertThat(newDiagnosis.get("pregnancyResult").asString()).isEqualTo("NEGATIVO");
    }

    @Test
    void abortionClosesThePregnancyWithoutCreatingOffspring() throws Exception {
        LocalDate diagnosisDate = TODAY.minusDays(60);
        LocalDate calvingDate = TODAY.minusDays(30);
        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", diagnosisDate.toString(), "pregnancyResult", "POSITIVO", "expectedCalvingDate", calvingDate.toString()));

        JsonNode abortion = post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "ABORTO",
                "date", diagnosisDate.plusDays(40).toString()));
        assertThat(abortion.get("kind").asString()).isEqualTo("ABORTO");
        assertThat(abortion.get("offspringAnimalIds")).isEmpty();

        // Sem gestação em aberto, um novo parto não entra.
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "PARTO",
                "date", TODAY.toString(), "offspring", List.of(Map.of("tag", "X", "sex", "FEMEA"))), 422);
    }

    @Test
    void birthRequiresAnOpenPositiveDiagnosisAndAtLeastOneCalf() throws Exception {
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "PARTO",
                "date", TODAY.toString(), "offspring", List.of(Map.of("tag", "X", "sex", "FEMEA"))), 422);

        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(60).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.minusDays(10).toString()));
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "PARTO",
                "date", TODAY.toString(), "offspring", List.of()), 422);
    }

    @Test
    void diagnosisValidatesResultAndExpectedCalvingDate() throws Exception {
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", TODAY.toString()), 422); // sem resultado
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", TODAY.toString(), "pregnancyResult", "POSITIVO"), 422); // sem previsão
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", TODAY.toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.toString()), 422); // previsão não é depois do diagnóstico
    }

    @Test
    void onlyActiveFemalesCanHaveReproductiveEvents() throws Exception {
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", bull, "kind", "CIO",
                "date", TODAY.toString()), 422);

        long other = post("/api/livestock/animals", owner,
                Map.of("tag", "V2", "speciesId", bovino, "sex", "FEMEA")).get("id").asLong();
        post("/api/livestock/animals/" + other + "/exit", owner, Map.of("type", "MORTE", "date", TODAY.toString()));
        call(HttpMethod.POST, "/api/livestock/reproduction", owner, Map.of("animalId", other, "kind", "CIO",
                "date", TODAY.toString()), 422);
    }

    @Test
    void deleteUndoesSimpleEventsButBlocksBirthsAbortionsAndClosedDiagnoses() throws Exception {
        JsonNode heat = post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "CIO",
                "date", TODAY.toString()));
        call(HttpMethod.DELETE, "/api/livestock/reproduction/" + heat.get("id").asLong(), owner, null, 204);

        JsonNode diagnosis = post("/api/livestock/reproduction", owner, Map.of("animalId", cow,
                "kind", "DIAGNOSTICO_PRENHEZ", "date", TODAY.minusDays(60).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.minusDays(10).toString()));
        JsonNode birth = post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "PARTO",
                "date", TODAY.toString(), "offspring", List.of(Map.of("tag", "C1", "sex", "MACHO"))));

        call(HttpMethod.DELETE, "/api/livestock/reproduction/" + birth.get("id").asLong(), owner, null, 422);
        call(HttpMethod.DELETE, "/api/livestock/reproduction/" + diagnosis.get("id").asLong(), owner, null, 422);
    }

    @Test
    void alertsShowUpcomingCalvingsAndPendingDiagnoses() throws Exception {
        LocalDate breedingDate = TODAY.minusDays(35);
        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "COBERTURA",
                "date", breedingDate.toString(), "sire", "Touro Valente"));

        long other = post("/api/livestock/animals", owner, Map.of("tag", "V3", "speciesId", bovino, "sex", "FEMEA",
                "entryDate", TODAY.minusDays(1000).toString())).get("id").asLong();
        post("/api/livestock/reproduction", owner, Map.of("animalId", other, "kind", "DIAGNOSTICO_PRENHEZ",
                "date", TODAY.minusDays(5).toString(), "pregnancyResult", "POSITIVO",
                "expectedCalvingDate", TODAY.plusDays(3).toString()));

        JsonNode alerts = get("/api/livestock/alerts", owner);
        assertThat(alerts.get("pendingDiagnoses")).hasSize(1);
        assertThat(alerts.get("pendingDiagnoses").get(0).get("animalId").asLong()).isEqualTo(cow);
        assertThat(alerts.get("upcomingCalvings")).hasSize(1);
        assertThat(alerts.get("upcomingCalvings").get(0).get("animalId").asLong()).isEqualTo(other);
    }

    @Test
    void workerRegistersButOnlyManagersDeleteAndPropertyIsIsolated() throws Exception {
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode heat = post("/api/livestock/reproduction", worker, Map.of("animalId", cow, "kind", "CIO",
                "date", TODAY.toString()));
        call(HttpMethod.POST, "/api/livestock/reproduction", viewer, Map.of("animalId", cow, "kind", "CIO",
                "date", TODAY.toString()), 403);
        call(HttpMethod.DELETE, "/api/livestock/reproduction/" + heat.get("id").asLong(), worker, null, 403);
        call(HttpMethod.DELETE, "/api/livestock/reproduction/" + heat.get("id").asLong(), owner, null, 204);

        Session other = registerOwnerWithProperty("Outro");
        call(HttpMethod.POST, "/api/livestock/reproduction", other, Map.of("animalId", cow, "kind", "CIO",
                "date", TODAY.toString()), 404);
    }

    @Test
    void listsReproductiveEventsFilteredByAnimal() throws Exception {
        long other = post("/api/livestock/animals", owner,
                Map.of("tag", "V4", "speciesId", bovino, "sex", "FEMEA")).get("id").asLong();
        post("/api/livestock/reproduction", owner, Map.of("animalId", cow, "kind", "CIO", "date", TODAY.toString()));
        post("/api/livestock/reproduction", owner, Map.of("animalId", other, "kind", "CIO", "date", TODAY.toString()));

        assertThat(page("/api/livestock/reproduction", owner)).hasSize(2);
        JsonNode filtered = page("/api/livestock/reproduction?animalId=" + cow, owner);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).get("animalId").asLong()).isEqualTo(cow);
    }
}
