package com.agrogestao.inventory;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.inventory.dto.InventoryMovementRequest;
import com.agrogestao.inventory.dto.InventoryMovementResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final TenantContext tenantContext;

    public InventoryMovementService(
            InventoryMovementRepository inventoryMovementRepository,
            InventoryItemRepository inventoryItemRepository,
            TenantContext tenantContext) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.tenantContext = tenantContext;
    }

    public List<InventoryMovementResponse> list() {
        return inventoryMovementRepository
                .findByPropertyIdOrderByMovementDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public InventoryMovementResponse create(InventoryMovementRequest request) {
        Long propertyId = tenantContext.requirePropertyId();
        InventoryMovement movement = registerMovement(propertyId, request.getProductId(), request.getType(),
                request.getQuantity(), request.getUnitCost(), request.getMovementDate(), request.getNotes());
        return toResponse(movement);
    }

    @Transactional
    public InventoryMovement registerMovement(Long propertyId, Long productId, MovementType type,
            BigDecimal quantity, BigDecimal unitCost, LocalDate date, String notes) {
        InventoryMovement movement = new InventoryMovement();
        movement.setPropertyId(propertyId);
        movement.setProductId(productId);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setMovementDate(date);
        movement.setNotes(notes);
        inventoryMovementRepository.save(movement);

        applyToBalance(propertyId, productId, type, quantity);
        return movement;
    }

    private void applyToBalance(Long propertyId, Long productId, MovementType type, BigDecimal quantity) {
        InventoryItem item = inventoryItemRepository.findByPropertyIdAndProductId(propertyId, productId)
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setPropertyId(propertyId);
                    newItem.setProductId(productId);
                    return newItem;
                });

        switch (type) {
            case IN -> item.setQuantity(item.getQuantity().add(quantity));
            case OUT -> {
                BigDecimal resulting = item.getQuantity().subtract(quantity);
                if (resulting.signum() < 0) {
                    throw new BusinessException("Estoque insuficiente para esta saída");
                }
                item.setQuantity(resulting);
            }
            case ADJUST -> item.setQuantity(quantity);
        }
        inventoryItemRepository.save(item);
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(movement.getId(), movement.getProductId(), movement.getType(),
                movement.getQuantity(), movement.getUnitCost(), movement.getMovementDate(), movement.getNotes());
    }
}
