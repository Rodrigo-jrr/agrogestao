package com.agrogestao.inventory;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByPropertyIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
            Long propertyId, LocalDate from, LocalDate to);

    List<InventoryMovement> findByPropertyIdAndProductIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
            Long propertyId, Long productId, LocalDate from, LocalDate to);

    boolean existsByProductId(Long productId);
}
