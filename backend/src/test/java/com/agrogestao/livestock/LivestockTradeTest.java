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

class LivestockTradeTest extends ApiTestSupport {

    private static final String TODAY = LocalDate.now().toString();
    private Session owner;
    private long supplierId;
    private long customerId;
    private long bovino;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerOwnerWithProperty("Criador");
        supplierId = post("/api/purchases/suppliers", owner, Map.of("name", "Leilão Central")).get("id").asLong();
        customerId = post("/api/sales/customers", owner, Map.of("name", "Frigorífico Sul")).get("id").asLong();
        bovino = post("/api/livestock/species", owner, Map.of("name", "Bovino", "allowsIndividual", true))
                .get("id").asLong();
    }

    private JsonNode buyCash(List<Map<String, Object>> animals, double total) throws Exception {
        return post("/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", total, "speciesId", bovino, "animals", animals));
    }

    private double cashTotal(String type) throws Exception {
        JsonNode totals = get("/api/finance/transactions/totals", owner);
        return totals.get(type.equals("INCOME") ? "income" : "expense").asDouble();
    }

    @Test
    void cashPurchaseCreatesAnimalsSplitsCostAndHitsTheCash() throws Exception {
        JsonNode trade = buyCash(List.of(
                Map.of("tag", "B1", "sex", "MACHO", "weight", 180),
                Map.of("tag", "B2", "sex", "MACHO"),
                Map.of("tag", "B3", "sex", "FEMEA")), 1000);

        assertThat(trade.get("headCount").asInt()).isEqualTo(3);
        assertThat(trade.get("partyName").asString()).isEqualTo("Leilão Central");
        assertThat(cashTotal("EXPENSE")).isEqualTo(1000.0);

        JsonNode detail = get("/api/livestock/trades/" + trade.get("id").asLong(), owner);
        assertThat(detail.get("animals")).hasSize(3);
        long b3 = detail.get("animals").findValues("tag").stream().filter(t -> t.asString().equals("B3")).count();
        assertThat(b3).isEqualTo(1);

        // O custo é dividido em centavos: 333,33 + 333,33 + 333,34.
        double sum = 0;
        for (JsonNode a : detail.get("animals")) {
            JsonNode entry = get("/api/livestock/animals/" + a.get("id").asLong(), owner).get("events").get(0);
            assertThat(a.get("origin").asString()).isEqualTo("COMPRA");
            sum += entry.get("amount").asDouble();
        }
        assertThat(sum).isEqualTo(1000.0);
    }

    @Test
    void creditSaleMarksAnimalsSoldAndCreatesReceivables() throws Exception {
        JsonNode bought = buyCash(List.of(Map.of("tag", "S1", "sex", "MACHO"), Map.of("tag", "S2", "sex", "MACHO")), 4000);
        List<Long> ids = get("/api/livestock/trades/" + bought.get("id").asLong(), owner).get("animals")
                .findValues("id").stream().map(JsonNode::asLong).toList();
        String due = LocalDate.now().plusMonths(1).toString();

        JsonNode sale = post("/api/livestock/sales", owner, Map.of("customerId", customerId, "tradeDate", TODAY,
                "totalAmount", 6000, "animalIds", ids, "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", due, "amount", 3000), Map.of("dueDate", due, "amount", 3000))));

        assertThat(sale.get("installmentCount").asInt()).isEqualTo(2);
        assertThat(page("/api/livestock/animals?status=VENDIDO", owner)).hasSize(2);
        JsonNode receivables = page("/api/finance/receivables", owner);
        assertThat(receivables).hasSize(2);
        assertThat(receivables.get(0).get("livestockTradeId").asLong()).isEqualTo(sale.get("id").asLong());
        // Parcela de venda de animais não se exclui pelo financeiro.
        call(HttpMethod.DELETE, "/api/finance/receivables/" + receivables.get(0).get("id").asLong(), owner, null, 422);
        // Animal vendido não é vendido de novo.
        call(HttpMethod.POST, "/api/livestock/sales", owner, Map.of("customerId", customerId, "tradeDate", TODAY,
                "totalAmount", 10, "animalIds", List.of(ids.get(0))), 422);
    }

    @Test
    void cancelingSaleBringsAnimalsBackAndRemovesTheIncome() throws Exception {
        JsonNode bought = buyCash(List.of(Map.of("tag", "C1", "sex", "FEMEA")), 2000);
        long animalId = get("/api/livestock/trades/" + bought.get("id").asLong(), owner).get("animals").get(0)
                .get("id").asLong();
        JsonNode sale = post("/api/livestock/sales", owner, Map.of("customerId", customerId, "tradeDate", TODAY,
                "totalAmount", 2500, "animalIds", List.of(animalId)));
        assertThat(cashTotal("INCOME")).isEqualTo(2500.0);

        // O lançamento da venda não se apaga direto pelo financeiro.
        long transactionId = page("/api/finance/transactions?type=INCOME", owner).get(0).get("id").asLong();
        call(HttpMethod.DELETE, "/api/finance/transactions/" + transactionId, owner, null, 422);

        JsonNode canceled = post("/api/livestock/trades/" + sale.get("id").asLong() + "/cancel", owner, null);
        assertThat(canceled.get("canceled").asBoolean()).isTrue();
        assertThat(cashTotal("INCOME")).isEqualTo(0.0);
        assertThat(get("/api/livestock/animals/" + animalId, owner).get("animal").get("status").asString())
                .isEqualTo("ATIVO");
        call(HttpMethod.POST, "/api/livestock/trades/" + sale.get("id").asLong() + "/cancel", owner, null, 422);
    }

    @Test
    void purchaseCancelRemovesAnimalsUnlessTheyAlreadyHaveHistory() throws Exception {
        JsonNode first = buyCash(List.of(Map.of("tag", "P1", "sex", "MACHO")), 1500);
        post("/api/livestock/trades/" + first.get("id").asLong() + "/cancel", owner, null);
        assertThat(page("/api/livestock/animals", owner)).isEmpty();
        assertThat(cashTotal("EXPENSE")).isEqualTo(0.0);

        JsonNode second = buyCash(List.of(Map.of("tag", "P2", "sex", "MACHO")), 1500);
        long animalId = get("/api/livestock/trades/" + second.get("id").asLong(), owner).get("animals").get(0)
                .get("id").asLong();
        post("/api/livestock/animals/" + animalId + "/exit", owner, Map.of("type", "MORTE", "date", TODAY));
        call(HttpMethod.POST, "/api/livestock/trades/" + second.get("id").asLong() + "/cancel", owner, null, 422);
    }

    @Test
    void aggregateBatchTradesMoveTheHeadCount() throws Exception {
        long peixe = post("/api/livestock/species", owner, Map.of("name", "Peixe", "allowsIndividual", false))
                .get("id").asLong();
        long batch = post("/api/livestock/batches", owner, Map.of("name", "Tanque 2", "speciesId", peixe,
                "tracking", "AGGREGATE")).get("id").asLong();
        post("/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 900, "batchId", batch, "headCount", 3000));
        JsonNode sale = post("/api/livestock/sales", owner, Map.of("customerId", customerId, "tradeDate", TODAY,
                "totalAmount", 5000, "batchId", batch, "headCount", 1000));
        assertThat(get("/api/livestock/batches", owner).get(0).get("headCount").asInt()).isEqualTo(2000);

        call(HttpMethod.POST, "/api/livestock/sales", owner, Map.of("customerId", customerId, "tradeDate", TODAY,
                "totalAmount", 5000, "batchId", batch, "headCount", 2001), 422);
        post("/api/livestock/trades/" + sale.get("id").asLong() + "/cancel", owner, null);
        assertThat(get("/api/livestock/batches", owner).get(0).get("headCount").asInt()).isEqualTo(3000);
    }

    @Test
    void rejectsInconsistentRequests() throws Exception {
        // Nem animais nem cabeças; os dois ao mesmo tempo; brinco repetido; parcelas que não fecham.
        call(HttpMethod.POST, "/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 100, "speciesId", bovino), 422);
        call(HttpMethod.POST, "/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 100, "speciesId", bovino, "headCount", 2,
                "animals", List.of(Map.of("tag", "X", "sex", "MACHO"))), 422);
        call(HttpMethod.POST, "/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 100, "speciesId", bovino,
                "animals", List.of(Map.of("tag", "X", "sex", "MACHO"), Map.of("tag", "x", "sex", "FEMEA"))), 422);
        call(HttpMethod.POST, "/api/livestock/purchases", owner, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 100, "speciesId", bovino, "paymentType", "INSTALLMENTS",
                "installments", List.of(Map.of("dueDate", TODAY, "amount", 90)),
                "animals", List.of(Map.of("tag", "Y", "sex", "MACHO"))), 422);
        assertThat(page("/api/livestock/animals", owner)).isEmpty();
    }

    @Test
    void onlyManagersCancelTrades() throws Exception {
        Session worker = addMember(owner, registerUser("Peão"), "WORKER");
        long ovino = post("/api/livestock/species", owner, Map.of("name", "Ovino", "allowsIndividual", true))
                .get("id").asLong();
        JsonNode trade = post("/api/livestock/purchases", worker, Map.of("supplierId", supplierId, "tradeDate", TODAY,
                "totalAmount", 700, "speciesId", ovino, "animals", List.of(Map.of("tag", "O1", "sex", "FEMEA"))));
        call(HttpMethod.POST, "/api/livestock/trades/" + trade.get("id").asLong() + "/cancel", worker, null, 403);
    }
}
