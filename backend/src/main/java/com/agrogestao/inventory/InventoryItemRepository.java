package com.agrogestao.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByPropertyIdAndProductId(Long propertyId, Long productId);

    List<InventoryItem> findByPropertyId(Long propertyId);
}
