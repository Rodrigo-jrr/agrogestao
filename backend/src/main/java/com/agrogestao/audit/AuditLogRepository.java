package com.agrogestao.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPropertyIdOrderByIdAsc(Long propertyId);

    /** [propertyId, última atividade] */
    @Query("SELECT a.propertyId, MAX(a.createdAt) FROM AuditLog a WHERE a.propertyId IS NOT NULL GROUP BY a.propertyId")
    List<Object[]> lastActivityByProperty();

    /** [userId, última atividade] — ignora acessos ao próprio painel de administração. */
    @Query("SELECT a.userId, MAX(a.createdAt) FROM AuditLog a WHERE a.userId IS NOT NULL "
            + "AND a.action <> com.agrogestao.audit.AuditAction.VIEW GROUP BY a.userId")
    List<Object[]> lastActivityByUser();
}
