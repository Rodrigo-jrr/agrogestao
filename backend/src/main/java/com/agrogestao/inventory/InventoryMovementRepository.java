package com.agrogestao.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByPropertyIdOrderByMovementDateDesc(Long propertyId);
}
