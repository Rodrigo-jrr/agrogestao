package com.agrogestao.inventory;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByPropertyIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
            Long propertyId, LocalDate from, LocalDate to, Pageable pageable);

    Page<InventoryMovement> findByPropertyIdAndProductIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
            Long propertyId, Long productId, LocalDate from, LocalDate to, Pageable pageable);

    boolean existsByProductId(Long productId);
}
