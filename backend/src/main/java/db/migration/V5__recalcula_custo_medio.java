package db.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Recalcula o custo médio ponderado de cada produto repassando as movimentações na ordem em que foram
 * registradas. Saídas passam a guardar o custo médio do momento (antes guardavam o preço de venda).
 * A regra aqui é uma cópia congelada da de InventoryMovementService, de propósito: migrações não mudam.
 */
public class V5__recalcula_custo_medio extends BaseJavaMigration {

    private record Balance(BigDecimal quantity, BigDecimal averageCost) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, Balance> balances = new HashMap<>();

        try (PreparedStatement select = connection.prepareStatement(
                     "SELECT id, property_id, product_id, type, quantity, unit_cost FROM inventory_movements ORDER BY id");
             PreparedStatement updateMovement = connection.prepareStatement(
                     "UPDATE inventory_movements SET unit_cost = ? WHERE id = ?");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String key = rows.getLong("property_id") + ":" + rows.getLong("product_id");
                Balance balance = balances.getOrDefault(key, new Balance(BigDecimal.ZERO, BigDecimal.ZERO));
                BigDecimal quantity = rows.getBigDecimal("quantity");
                BigDecimal unitCost = rows.getBigDecimal("unit_cost");
                String type = rows.getString("type");

                if ("IN".equals(type)) {
                    BigDecimal newQuantity = balance.quantity().add(quantity);
                    BigDecimal average = balance.averageCost();
                    if (unitCost != null && newQuantity.signum() > 0) {
                        average = balance.quantity().multiply(balance.averageCost()).add(quantity.multiply(unitCost))
                                .divide(newQuantity, 2, RoundingMode.HALF_UP);
                    }
                    balance = new Balance(newQuantity, average);
                } else if ("OUT".equals(type)) {
                    updateMovement.setBigDecimal(1, balance.averageCost());
                    updateMovement.setLong(2, rows.getLong("id"));
                    updateMovement.executeUpdate();
                    balance = new Balance(balance.quantity().subtract(quantity), balance.averageCost());
                } else {
                    balance = new Balance(quantity, unitCost != null ? unitCost : balance.averageCost());
                }
                balances.put(key, balance);
            }
        }

        try (PreparedStatement updateItem = connection.prepareStatement(
                "UPDATE inventory_items SET average_cost = ? WHERE property_id = ? AND product_id = ?")) {
            for (Map.Entry<String, Balance> entry : balances.entrySet()) {
                String[] ids = entry.getKey().split(":");
                updateItem.setBigDecimal(1, entry.getValue().averageCost());
                updateItem.setLong(2, Long.parseLong(ids[0]));
                updateItem.setLong(3, Long.parseLong(ids[1]));
                updateItem.executeUpdate();
            }
        }
    }
}
