package com.agrogestao.livestock;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

class SpeciesCatalogTest extends ApiTestSupport {

    private JsonNode create(Session s, String name, boolean allowsIndividual) throws Exception {
        return post("/api/livestock/species", s, Map.of("name", name, "allowsIndividual", allowsIndividual));
    }

    @Test
    void ownerCreatesListsArchivesAndDeletesASpeciesNotInUse() throws Exception {
        Session owner = registerOwnerWithProperty("Catálogo");
        JsonNode bovino = create(owner, "Bovino", true);
        assertThat(bovino.get("allowsIndividual").asBoolean()).isTrue();
        assertThat(bovino.get("archived").asBoolean()).isFalse();

        assertThat(get("/api/livestock/species", owner)).hasSize(1);

        JsonNode archived = post("/api/livestock/species/" + bovino.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
        assertThat(get("/api/livestock/species", owner)).isEmpty();
        assertThat(get("/api/livestock/species?includeArchived=true", owner)).hasSize(1);

        JsonNode unarchived = post("/api/livestock/species/" + bovino.get("id").asLong() + "/unarchive", owner, null);
        assertThat(unarchived.get("archived").asBoolean()).isFalse();

        call(HttpMethod.DELETE, "/api/livestock/species/" + bovino.get("id").asLong(), owner, null, 204);
        assertThat(get("/api/livestock/species", owner)).isEmpty();
    }

    @Test
    void rejectsDuplicateNamesAndBlocksArchivedSpeciesFromNewAnimals() throws Exception {
        Session owner = registerOwnerWithProperty("Duplicada");
        JsonNode ave = create(owner, "Ave", false);
        call(HttpMethod.POST, "/api/livestock/species", owner, Map.of("name", " ave ", "allowsIndividual", true), 422);

        post("/api/livestock/species/" + ave.get("id").asLong() + "/archive", owner, null);
        call(HttpMethod.POST, "/api/livestock/animals", owner,
                Map.of("tag", "1", "speciesId", ave.get("id").asLong(), "sex", "FEMEA"), 422);
    }

    @Test
    void cannotDeleteASpeciesAlreadyInUseByAnAnimalOrBatch() throws Exception {
        Session owner = registerOwnerWithProperty("EmUso");
        JsonNode bovino = create(owner, "Bovino", true);
        post("/api/livestock/animals", owner, Map.of("tag", "1", "speciesId", bovino.get("id").asLong(), "sex", "FEMEA"));

        call(HttpMethod.DELETE, "/api/livestock/species/" + bovino.get("id").asLong(), owner, null, 422);
        // Mas pode ser arquivada mesmo em uso.
        JsonNode archived = post("/api/livestock/species/" + bovino.get("id").asLong() + "/archive", owner, null);
        assertThat(archived.get("archived").asBoolean()).isTrue();
    }

    @Test
    void workerCreatesButOnlyManagersArchiveOrDelete() throws Exception {
        Session owner = registerOwnerWithProperty("Papéis");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        Session viewer = addMember(owner, registerUser("Visita"), "VIEWER");

        JsonNode bovino = create(worker, "Bovino", true);
        call(HttpMethod.POST, "/api/livestock/species", viewer, Map.of("name", "Ovino", "allowsIndividual", true), 403);
        call(HttpMethod.POST, "/api/livestock/species/" + bovino.get("id").asLong() + "/archive", worker, null, 403);
        post("/api/livestock/species/" + bovino.get("id").asLong() + "/archive", owner, null);
    }

    @Test
    void catalogIsIsolatedByProperty() throws Exception {
        Session owner = registerOwnerWithProperty("Um");
        Session other = registerOwnerWithProperty("Outro");
        JsonNode bovino = create(owner, "Bovino", true);

        assertThat(get("/api/livestock/species", other)).isEmpty();
        call(HttpMethod.POST, "/api/livestock/species/" + bovino.get("id").asLong() + "/archive", other, null, 404);
        call(HttpMethod.POST, "/api/livestock/animals", other,
                Map.of("tag", "1", "speciesId", bovino.get("id").asLong(), "sex", "FEMEA"), 422);
    }
}
