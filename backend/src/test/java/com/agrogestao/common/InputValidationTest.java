package com.agrogestao.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** Erros de quem usa a API devem voltar como 4xx com mensagem clara, nunca como erro interno. */
class InputValidationTest extends ApiTestSupport {

    private Session owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Dona");
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/activities")
                        .header("Authorization", "Bearer " + owner.token())
                        .header("X-Property-Id", owner.propertyId())
                        .contentType(MediaType.APPLICATION_JSON).content("{name:"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEnumNamesTheField() throws Exception {
        JsonNode error = call(HttpMethod.POST, "/api/finance/transactions", owner, Map.of("type", "XYZ",
                "description", "x", "amount", 1, "transactionDate", "2026-09-01"), 400);
        assertThat(error.get("message").asString()).isEqualTo("Valor inválido para o campo Tipo.");
    }

    @Test
    void nonNumericIdIsBadRequestAndUnknownRouteIsNotFound() throws Exception {
        call(HttpMethod.DELETE, "/api/activities/abc", owner, null, 400);
        call(HttpMethod.GET, "/api/nao-existe", owner, null, 404);
    }

    @Test
    void textLongerThanColumnIsRejectedWithFieldLabel() throws Exception {
        JsonNode error = call(HttpMethod.POST, "/api/activities", owner,
                Map.of("name", "x".repeat(151), "type", "PECUARIA"), 400);
        assertThat(error.get("message").asString()).startsWith("Nome:");
    }

    @Test
    void hugeOrNegativeNumbersAreRejected() throws Exception {
        call(HttpMethod.POST, "/api/finance/transactions", owner, Map.of("type", "INCOME", "description", "x",
                "amount", new java.math.BigDecimal("99999999999999999999"), "transactionDate", "2026-09-01"), 400);
        call(HttpMethod.POST, "/api/finance/transactions", owner, Map.of("type", "INCOME", "description", "x",
                "amount", 10.555, "transactionDate", "2026-09-01"), 400);
        call(HttpMethod.POST, "/api/activities", owner, Map.of("name", "Neg", "type", "X", "areaHa", -50), 400);
    }

    @Test
    void purchaseTotalAboveColumnLimitIsRejected() throws Exception {
        long product = post("/api/inventory/products", owner, Map.of("name", "Boi", "unit", "cab", "minStock", 0))
                .get("id").asLong();
        long supplier = post("/api/purchases/suppliers", owner, Map.of("name", "Leilão")).get("id").asLong();
        call(HttpMethod.POST, "/api/purchases", owner, Map.of("supplierId", supplier, "purchaseDate", "2026-09-01",
                "items", List.of(Map.of("productId", product, "quantity", 999_999_999, "unitPrice", 999_999_999))), 422);
    }

    @Test
    void zeroQuantityOnlyAllowedForAdjustments() throws Exception {
        long product = post("/api/inventory/products", owner, Map.of("name", "Sal", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        call(HttpMethod.POST, "/api/inventory/movements", owner, Map.of("productId", product, "type", "IN",
                "quantity", 0, "movementDate", "2026-09-01"), 422);
        call(HttpMethod.POST, "/api/inventory/movements", owner, Map.of("productId", product, "type", "ADJUST",
                "quantity", 0, "movementDate", "2026-09-01"), 200);
    }

    @Test
    void passwordLongerThanBcryptLimitIsRejectedOnRegister() throws Exception {
        call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("name", "Longa", "email", "longa@teste.dev", "password", "a".repeat(73)), 400);
    }
}
