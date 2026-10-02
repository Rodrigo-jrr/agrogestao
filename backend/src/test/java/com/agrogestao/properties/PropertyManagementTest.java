package com.agrogestao.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.agrogestao.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

class PropertyManagementTest extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ownerAndManagerEditPropertyWorkerCannot() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");

        JsonNode updated = call(HttpMethod.PUT, "/api/properties/current", manager,
                Map.of("name", "Fazenda Nova", "location", "Uberaba - MG", "totalAreaHa", 120.5), 200);
        assertThat(updated.get("name").asString()).isEqualTo("Fazenda Nova");
        assertThat(updated.get("role").asString()).isEqualTo("MANAGER");

        call(HttpMethod.PUT, "/api/properties/current", worker, Map.of("name", "Outra"), 403);

        JsonNode mine = get("/api/properties/me", owner);
        assertThat(mine.get(0).get("name").asString()).isEqualTo("Fazenda Nova");
        assertThat(mine.get(0).get("location").asString()).isEqualTo("Uberaba - MG");
    }

    @Test
    void deletingRequiresOwnerExactNameAndPassword() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session manager = addMember(owner, registerUser("Gerente"), "MANAGER");
        String name = "Sítio Dono";

        call(HttpMethod.DELETE, "/api/properties/current", manager,
                Map.of("confirmName", name, "password", "senha1234"), 403);
        call(HttpMethod.DELETE, "/api/properties/current", owner,
                Map.of("confirmName", "Sitio Dono", "password", "senha1234"), 422);
        call(HttpMethod.DELETE, "/api/properties/current", owner,
                Map.of("confirmName", name, "password", "errada123"), 422);

        assertThat(get("/api/properties/me", owner)).hasSize(1);
    }

    @Test
    void deletingErasesEveryRowOfThePropertyAndKeepsOthers() throws Exception {
        Session owner = registerOwnerWithProperty("Dono");
        Session member = addMember(owner, registerUser("Colega"), "WORKER");
        Session neighbor = registerOwnerWithProperty("Vizinho");
        fillWithData(owner);
        fillWithData(neighbor);
        post("/api/properties/current/invitations", owner, Map.of("email", "convidado@teste.dev", "role", "VIEWER"));
        Long organizationId = get("/api/properties/me", owner).get(0).get("organizationId").asLong();

        call(HttpMethod.DELETE, "/api/properties/current", owner,
                Map.of("confirmName", "Sítio Dono", "password", "senha1234"), 204);

        assertThat(get("/api/properties/me", owner)).isEmpty();
        assertThat(get("/api/properties/me", member)).isEmpty();
        call(HttpMethod.GET, "/api/inventory/products", owner, null, 403);
        assertThat(rowsWithPropertyId(owner.propertyId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organizations WHERE id = ?", Integer.class,
                organizationId)).isZero();

        // O vizinho não perde nada.
        assertThat(get("/api/inventory/products", neighbor)).isNotEmpty();
        assertThat(page("/api/sales", neighbor)).isNotEmpty();
    }

    /** Todas as tabelas com coluna property_id — inclusive as que surgirem depois — ficam vazias para a propriedade. */
    private List<String> rowsWithPropertyId(Long propertyId) {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.columns WHERE LOWER(column_name) = 'property_id' "
                        + "AND LOWER(table_schema) = 'public'", String.class);
        assertThat(tables).contains("products", "sales", "audit_logs", "property_invitations");
        return tables.stream()
                .filter(table -> jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table + " WHERE property_id = ?", Integer.class, propertyId) > 0)
                .toList();
    }

    /** Um pouco de tudo: atividade, centro de custo, produção, produto, compra e venda (à vista e a prazo). */
    private void fillWithData(Session session) throws Exception {
        String today = java.time.LocalDate.now().toString();
        String nextMonth = java.time.LocalDate.now().plusMonths(1).toString();
        Long activityId = post("/api/activities", session, Map.of("name", "Milho", "type", "AGRICULTURA")).get("id").asLong();
        post("/api/finance/cost-centers", session, Map.of("name", "Lavoura", "activityId", activityId));
        // Agricultura: talhão, cultura e a safra que liga os dois.
        Long fieldId = post("/api/fields", session, Map.of("name", "Talhão 1", "areaHa", 10)).get("id").asLong();
        Long cropId = post("/api/crops", session, Map.of("name", "Milho")).get("id").asLong();
        Long cropCycleId = post("/api/crop-cycles", session,
                Map.of("fieldId", fieldId, "cropId", cropId, "plannedPlantingDate", today)).get("id").asLong();
        post("/api/crop-operations", session, Map.of("cropCycleId", cropCycleId, "kind", "PREPARO",
                "operationDate", today, "cost", 80));
        post("/api/production", session, Map.of("productName", "Milho", "quantity", 10, "unit", "sc",
                "productionDate", today, "activityId", activityId));
        post("/api/finance/transactions", session, Map.of("type", "EXPENSE", "description", "Diesel",
                "amount", 50, "transactionDate", today));
        Long productId = post("/api/inventory/products", session, Map.of("name", "Ração", "unit", "kg", "minStock", 0)).get("id").asLong();
        Long supplierId = post("/api/purchases/suppliers", session, Map.of("name", "Agropecuária")).get("id").asLong();
        Long customerId = post("/api/sales/customers", session, Map.of("name", "Laticínio")).get("id").asLong();
        // Colheita vendida a prazo: exercita accounts_receivable.crop_harvest_id.
        post("/api/crop-harvests", session, Map.of("cropCycleId", cropCycleId, "harvestDate", today,
                "quantity", 100, "quantityUnit", "sc", "destination", "VENDA", "customerId", customerId,
                "totalAmount", 500, "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", nextMonth, "amount", 500))));
        post("/api/purchases", session, Map.of("supplierId", supplierId, "purchaseDate", today,
                "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", nextMonth, "amount", 100)),
                "items", List.of(Map.of("productId", productId, "quantity", 10, "unitPrice", 10))));
        post("/api/sales", session, Map.of("customerId", customerId, "saleDate", today,
                "items", List.of(Map.of("productId", productId, "quantity", 2, "unitPrice", 20))));
        post("/api/finance/payables", session, Map.of("description", "Frete", "amount", 30, "dueDate", nextMonth,
                "supplierId", supplierId));
        post("/api/finance/receivables", session, Map.of("description", "Leite", "amount", 40, "dueDate", nextMonth,
                "customerId", customerId));
        // Rebanho: lote, mãe e cria (auto-referência), compra e venda a prazo e lote por contagem.
        Long bovinoId = post("/api/livestock/species", session, Map.of("name", "Bovino", "allowsIndividual", true))
                .get("id").asLong();
        Long aveId = post("/api/livestock/species", session, Map.of("name", "Ave", "allowsIndividual", false))
                .get("id").asLong();
        Long batchId = post("/api/livestock/batches", session, Map.of("name", "Vacas", "speciesId", bovinoId,
                "tracking", "INDIVIDUAL", "activityId", activityId)).get("id").asLong();
        Long cowId = post("/api/livestock/animals", session, Map.of("tag", "1", "speciesId", bovinoId, "sex", "FEMEA",
                "batchId", batchId, "birthDate", "2020-01-01")).get("id").asLong();
        post("/api/livestock/animals", session, Map.of("tag", "2", "speciesId", bovinoId, "sex", "MACHO",
                "origin", "NASCIMENTO", "birthDate", today, "motherId", cowId));
        post("/api/livestock/purchases", session, Map.of("supplierId", supplierId, "tradeDate", today, "totalAmount", 500,
                "batchId", batchId, "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", nextMonth, "amount", 500)),
                "animals", List.of(Map.of("tag", "3", "sex", "MACHO"))));
        post("/api/livestock/sales", session, Map.of("customerId", customerId, "tradeDate", today, "totalAmount", 800,
                "animalIds", List.of(cowId), "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", nextMonth, "amount", 800))));
        Long flock = post("/api/livestock/batches", session, Map.of("name", "Galinhas", "speciesId", aveId,
                "tracking", "AGGREGATE")).get("id").asLong();
        post("/api/livestock/batches/" + flock + "/entries", session, Map.of("headCount", 30, "date", today));
        // Manejo: peso médio do lote e vacina do estoque (liga aplicação a produto e movimentação).
        post("/api/livestock/weighings", session, Map.of("date", today, "batchId", flock, "averageWeight", 2,
                "sampleSize", 5));
        post("/api/livestock/health", session, Map.of("kind", "VACINA", "date", today, "batchId", flock,
                "productId", productId, "dose", 0.1, "withdrawalDays", 10, "nextDate", nextMonth));
    }
}
