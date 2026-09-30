package com.agrogestao.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;

/** Garante que um usuário nunca lê nem altera dados de propriedades das quais não é membro. */
class TenantIsolationTest extends ApiTestSupport {

    private Session alice;
    private Session bob;

    @BeforeEach
    void setUp() throws Exception {
        alice = registerOwnerWithProperty("Alice");
        bob = registerOwnerWithProperty("Bob");
    }

    @Test
    void requestWithoutPropertyHeaderIsRejected() throws Exception {
        call(HttpMethod.GET, "/api/activities", alice.on(null), null, 403);
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        call(HttpMethod.GET, "/api/activities", null, null, 401);
    }

    @Test
    void cannotUseAnotherUsersPropertyIdInHeader() throws Exception {
        Session bobOnAlicesProperty = bob.on(alice.propertyId());
        call(HttpMethod.GET, "/api/activities", bobOnAlicesProperty, null, 403);
        call(HttpMethod.GET, "/api/finance/transactions", bobOnAlicesProperty, null, 403);
        call(HttpMethod.GET, "/api/properties/current/members", bobOnAlicesProperty, null, 403);
        call(HttpMethod.POST, "/api/production", bobOnAlicesProperty, Map.of(
                "productName", "Leite", "quantity", 10, "unit", "L", "productionDate", "2026-09-01"), 403);
    }

    @Test
    void cannotReadOrModifyRecordsOfAnotherPropertyById() throws Exception {
        JsonNode record = post("/api/production", alice, Map.of(
                "productName", "Leite", "quantity", 10, "unit", "L", "productionDate", "2026-09-01"));
        String path = "/api/production/" + record.get("id").asLong();

        call(HttpMethod.PUT, path, bob, Map.of(
                "productName", "Hack", "quantity", 1, "unit", "L", "productionDate", "2026-09-01"), 404);
        call(HttpMethod.DELETE, path, bob, null, 404);

        assertThat(page("/api/production", bob)).isEmpty();
        assertThat(page("/api/production", alice).get(0).get("productName").asString()).isEqualTo("Leite");
    }

    @Test
    void cannotReferenceAnotherPropertysRecordsWhenCreating() throws Exception {
        long alicesSupplier = post("/api/purchases/suppliers", alice, Map.of("name", "Loja da Alice"))
                .get("id").asLong();
        long alicesProduct = post("/api/inventory/products", alice, Map.of("name", "Ração", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        long bobsProduct = post("/api/inventory/products", bob, Map.of("name", "Ração", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        long bobsSupplier = post("/api/purchases/suppliers", bob, Map.of("name", "Loja do Bob")).get("id").asLong();

        call(HttpMethod.POST, "/api/purchases", bob, Map.of("supplierId", alicesSupplier, "purchaseDate", "2026-09-01",
                "items", List.of(Map.of("productId", bobsProduct, "quantity", 1, "unitPrice", 1))), 422);
        call(HttpMethod.POST, "/api/purchases", bob, Map.of("supplierId", bobsSupplier, "purchaseDate", "2026-09-01",
                "items", List.of(Map.of("productId", alicesProduct, "quantity", 1, "unitPrice", 1))), 422);
        call(HttpMethod.POST, "/api/finance/payables", bob, Map.of("description", "x", "amount", 1,
                "dueDate", "2026-09-01", "supplierId", alicesSupplier), 422);
    }

    @Test
    void propertyListOnlyShowsOwnMemberships() throws Exception {
        JsonNode properties = get("/api/properties/me", bob.on(null));
        assertThat(properties).hasSize(1);
        assertThat(properties.get(0).get("id").asLong()).isEqualTo(bob.propertyId());
    }
}
