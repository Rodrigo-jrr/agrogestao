package com.agrogestao.livestock;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class HerdTest extends ApiTestSupport {

    private static final String TODAY = LocalDate.now().toString();

    private long species(Session s, String name, boolean allowsIndividual) throws Exception {
        return post("/api/livestock/species", s, Map.of("name", name, "allowsIndividual", allowsIndividual))
                .get("id").asLong();
    }

    private long batch(Session s, String name, long speciesId, String tracking) throws Exception {
        return post("/api/livestock/batches", s, Map.of("name", name, "speciesId", speciesId, "tracking", tracking))
                .get("id").asLong();
    }

    private JsonNode animal(Session s, String tag, String sex, long speciesId, Map<String, Object> extra) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("tag", tag, "speciesId", speciesId, "sex", sex));
        body.putAll(extra);
        return post("/api/livestock/animals", s, body);
    }

    @Test
    void registersAnimalsInBatchesWithTimelineAndMother() throws Exception {
        Session owner = registerOwnerWithProperty("Pecuarista");
        long bovino = species(owner, "Bovino", true);
        long cows = batch(owner, "Vacas", bovino, "INDIVIDUAL");
        JsonNode mother = animal(owner, "101", "FEMEA", bovino, Map.of("batchId", cows, "weight", 450,
                "birthDate", "2020-03-01", "breed", "Nelore"));
        JsonNode calf = animal(owner, "201", "MACHO", bovino, Map.of("origin", "NASCIMENTO", "birthDate", TODAY,
                "motherId", mother.get("id").asLong(), "sire", "Touro Valente"));

        assertThat(calf.get("motherTag").asString()).isEqualTo("101");
        assertThat(calf.get("entryDate").asString()).isEqualTo(TODAY);
        JsonNode list = get("/api/livestock/batches", owner);
        assertThat(list.get(0).get("headCount").asInt()).isEqualTo(1);

        JsonNode detail = get("/api/livestock/animals/" + mother.get("id").asLong(), owner);
        assertThat(detail.get("animal").get("currentWeight").asDouble()).isEqualTo(450.0);
        assertThat(detail.get("events").get(0).get("type").asString()).isEqualTo("ENTRADA");
        assertThat(detail.get("events").get(0).get("weight").asDouble()).isEqualTo(450.0);
        assertThat(detail.get("offspring").get(0).get("tag").asString()).isEqualTo("201");

        JsonNode summary = get("/api/livestock/summary", owner);
        assertThat(summary.get("totalHeads").asLong()).isEqualTo(2);
    }

    @Test
    void enforcesTagSpeciesAndMotherRules() throws Exception {
        Session owner = registerOwnerWithProperty("Regras");
        long bovino = species(owner, "Bovino", true);
        long ovino = species(owner, "Ovino", true);
        long ave = species(owner, "Ave", false);
        long sheepBatch = batch(owner, "Ovelhas", ovino, "INDIVIDUAL");
        JsonNode bull = animal(owner, "1", "MACHO", bovino, Map.of());

        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", " 1 ", "speciesId", bovino, "sex", "FEMEA"), 422); // brinco repetido
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "2", "speciesId", bovino, "sex", "FEMEA", "batchId", sheepBatch), 422); // espécie
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "3", "speciesId", bovino, "sex", "FEMEA", "motherId", bull.get("id").asLong()), 422);
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "4", "speciesId", ave, "sex", "FEMEA"), 422); // aves só por lote
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "5", "speciesId", bovino, "sex", "FEMEA", "origin", "COMPRA"), 422); // compra tem fluxo próprio
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "6", "speciesId", bovino, "sex", "FEMEA",
                        "birthDate", LocalDate.now().plusDays(1).toString()), 422);
        call(HttpMethod.POST, "/api/livestock/batches", owner,
                Map.of("name", "Frangos", "speciesId", ave, "tracking", "INDIVIDUAL"), 422);
    }

    @Test
    void movesBetweenBatchesAndRecordsExitAndReactivation() throws Exception {
        Session owner = registerOwnerWithProperty("Manejo");
        long bovino = species(owner, "Bovino", true);
        long a = batch(owner, "Recria", bovino, "INDIVIDUAL");
        long b = batch(owner, "Engorda", bovino, "INDIVIDUAL");
        long id1 = animal(owner, "10", "MACHO", bovino, Map.of("batchId", a)).get("id").asLong();
        long id2 = animal(owner, "11", "MACHO", bovino, Map.of("batchId", a)).get("id").asLong();

        call(HttpMethod.POST, "/api/livestock/animals/move", owner,
                Map.of("animalIds", List.of(id1, id2), "toBatchId", b, "date", TODAY), 200);
        JsonNode events = get("/api/livestock/animals/" + id1, owner).get("events");
        assertThat(events.get(0).get("type").asString()).isEqualTo("MUDANCA_LOTE");
        assertThat(events.get(0).get("fromBatchName").asString()).isEqualTo("Recria");
        assertThat(events.get(0).get("toBatchName").asString()).isEqualTo("Engorda");

        JsonNode dead = post("/api/livestock/animals/" + id1 + "/exit", owner,
                Map.of("type", "MORTE", "date", TODAY, "reason", "picada de cobra"));
        assertThat(dead.get("status").asString()).isEqualTo("MORTO");
        assertThat(dead.get("exitReason").asString()).isEqualTo("Morte: picada de cobra");
        assertThat(page("/api/livestock/animals?status=ATIVO", owner)).hasSize(1);

        // Com o 10 fora do rebanho, o brinco pode ser reutilizado — e aí não dá para reativar o antigo.
        long reused = animal(owner, "10", "FEMEA", bovino, Map.of()).get("id").asLong();
        call(HttpMethod.POST, "/api/livestock/animals/" + id1 + "/reactivate", owner, null, 422);
        call(HttpMethod.DELETE, "/api/livestock/animals/" + reused, owner, null, 204);
        JsonNode back = post("/api/livestock/animals/" + id1 + "/reactivate", owner, null);
        assertThat(back.get("status").asString()).isEqualTo("ATIVO");
        assertThat(get("/api/livestock/animals/" + id1, owner).get("events").get(0).get("type").asString())
                .isEqualTo("MUDANCA_LOTE");
    }

    @Test
    void aggregateBatchCountsHeadsWithoutTags() throws Exception {
        Session owner = registerOwnerWithProperty("Granja");
        long ave = species(owner, "Ave", false);
        long chickens = batch(owner, "Galpão 1", ave, "AGGREGATE");

        post("/api/livestock/batches/" + chickens + "/entries", owner,
                Map.of("headCount", 500, "date", TODAY, "description", "Pintinhos de um dia"));
        call(HttpMethod.POST, "/api/livestock/batches/" + chickens + "/exits", owner,
                Map.of("headCount", 501, "date", TODAY, "exitType", "MORTE"), 422);
        JsonNode after = post("/api/livestock/batches/" + chickens + "/exits", owner,
                Map.of("headCount", 12, "date", TODAY, "exitType", "MORTE"));
        assertThat(after.get("headCount").asInt()).isEqualTo(488);

        call(HttpMethod.POST, "/api/livestock/batches/" + chickens + "/archive", owner, null, 422); // ainda tem aves
        call(HttpMethod.DELETE, "/api/livestock/batches/" + chickens, owner, null, 422); // tem histórico
        assertThat(get("/api/livestock/summary", owner).get("bySpecies").get(0).get("heads").asLong()).isEqualTo(488);
    }

    @Test
    void workerRegistersButOnlyManagersDeleteOrReactivate() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        long bovino = species(owner, "Bovino", true);
        Session worker = addMember(owner, registerUser("Vaqueiro"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        long id = post("/api/livestock/animals", worker, Map.of("tag", "7", "speciesId", bovino, "sex", "FEMEA"))
                .get("id").asLong();
        call(HttpMethod.POST, "/api/livestock/animals", viewer,
                Map.of("tag", "8", "speciesId", bovino, "sex", "FEMEA"), 403);
        call(HttpMethod.DELETE, "/api/livestock/animals/" + id, worker, null, 403);
        post("/api/livestock/animals/" + id + "/exit", worker, Map.of("type", "DESCARTE", "date", TODAY));
        call(HttpMethod.POST, "/api/livestock/animals/" + id + "/reactivate", worker, null, 403);
        assertThat(page("/api/livestock/animals", viewer)).hasSize(1);
    }

    @Test
    void herdIsIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        long bovino = species(owner, "Bovino", true);
        long otherBovino = species(other, "Bovino", true);
        long id = animal(owner, "99", "FEMEA", bovino, Map.of()).get("id").asLong();
        long batchId = batch(owner, "Lote A", bovino, "INDIVIDUAL");

        call(HttpMethod.GET, "/api/livestock/animals/" + id, other, null, 404);
        assertThat(page("/api/livestock/animals", other)).isEmpty();
        call(HttpMethod.POST, "/api/livestock/animals", other,
                Map.of("tag", "1", "speciesId", otherBovino, "sex", "FEMEA", "batchId", batchId), 422);
        call(HttpMethod.POST, "/api/livestock/animals", other,
                Map.of("tag", "2", "speciesId", otherBovino, "sex", "MACHO", "motherId", id), 422);
        // O mesmo brinco pode existir em outra propriedade.
        animal(other, "99", "FEMEA", otherBovino, Map.of());
    }
}
