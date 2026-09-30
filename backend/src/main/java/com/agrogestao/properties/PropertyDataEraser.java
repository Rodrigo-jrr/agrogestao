package com.agrogestao.properties;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apaga de vez todos os dados de uma propriedade, na ordem que as chaves estrangeiras exigem.
 * Ao criar uma tabela com property_id, inclua-a aqui — o PropertyDeletionTest confere que nenhuma sobra.
 */
@Component
public class PropertyDataEraser {

    private static final List<String> STATEMENTS = List.of(
            "DELETE FROM purchase_items WHERE purchase_id IN (SELECT id FROM purchases WHERE property_id = ?)",
            "DELETE FROM sale_items WHERE sale_id IN (SELECT id FROM sales WHERE property_id = ?)",
            "DELETE FROM accounts_payable WHERE property_id = ?",
            "DELETE FROM accounts_receivable WHERE property_id = ?",
            "DELETE FROM purchases WHERE property_id = ?",
            "DELETE FROM sales WHERE property_id = ?",
            "DELETE FROM financial_transactions WHERE property_id = ?",
            "DELETE FROM inventory_movements WHERE property_id = ?",
            "DELETE FROM inventory_items WHERE property_id = ?",
            "DELETE FROM products WHERE property_id = ?",
            "DELETE FROM production_records WHERE property_id = ?",
            "DELETE FROM cost_centers WHERE property_id = ?",
            "DELETE FROM activities WHERE property_id = ?",
            "DELETE FROM suppliers WHERE property_id = ?",
            "DELETE FROM customers WHERE property_id = ?",
            "DELETE FROM property_invitations WHERE property_id = ?",
            "DELETE FROM property_members WHERE property_id = ?",
            "DELETE FROM audit_logs WHERE property_id = ?",
            "DELETE FROM properties WHERE id = ?");

    private final JdbcTemplate jdbcTemplate;

    public PropertyDataEraser(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void eraseProperty(Long propertyId) {
        STATEMENTS.forEach(sql -> jdbcTemplate.update(sql, propertyId));
    }

    /** Organização sem nenhuma propriedade não serve para nada: sai junto com os vínculos. */
    @Transactional
    public void eraseOrganizationIfEmpty(Long organizationId) {
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM properties WHERE organization_id = ?", Integer.class, organizationId);
        if (remaining != null && remaining == 0) {
            jdbcTemplate.update("DELETE FROM organization_members WHERE organization_id = ?", organizationId);
            jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
        }
    }
}
