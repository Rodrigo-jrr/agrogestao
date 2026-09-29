package com.agrogestao.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.agrogestao.ApiTestSupport;
import com.agrogestao.auth.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

class AuditTrailTest extends ApiTestSupport {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void recordsWhoDidWhatInEachModule() throws Exception {
        Session owner = registerOwnerWithProperty("Dona");
        Session worker = addMember(owner, registerUser("Peao"), "WORKER");

        long supplierId = post("/api/purchases/suppliers", worker, Map.of("name", "Agro Loja")).get("id").asLong();
        long productId = post("/api/inventory/products", worker, Map.of("name", "Ração", "unit", "kg", "minStock", 0))
                .get("id").asLong();
        long purchaseId = post("/api/purchases", worker, Map.of("supplierId", supplierId, "purchaseDate", "2026-09-10",
                "items", List.of(Map.of("productId", productId, "quantity", 2, "unitPrice", 5)))).get("id").asLong();
        post("/api/purchases/" + purchaseId + "/cancel", owner, null);
        call(HttpMethod.PUT, "/api/purchases/suppliers/" + supplierId, worker, Map.of("name", "Agro Loja Ltda"), 200);

        List<AuditLog> logs = auditLogRepository.findByPropertyIdOrderByIdAsc(owner.propertyId());
        Long workerId = userRepository.findByEmail(worker.email()).orElseThrow().getId();
        Long ownerId = userRepository.findByEmail(owner.email()).orElseThrow().getId();

        assertThat(logs).extracting(AuditLog::getEntityName, AuditLog::getAction, AuditLog::getUserId).containsExactly(
                tuple("Property", AuditAction.CREATE, ownerId),
                tuple("PropertyMember", AuditAction.CREATE, ownerId),
                tuple("Supplier", AuditAction.CREATE, workerId),
                tuple("Product", AuditAction.CREATE, workerId),
                tuple("Purchase", AuditAction.CREATE, workerId),
                tuple("Purchase", AuditAction.CANCEL, ownerId),
                tuple("Supplier", AuditAction.UPDATE, workerId));
        assertThat(logs.get(6).getDetails()).isEqualTo("Agro Loja Ltda");
    }
}
