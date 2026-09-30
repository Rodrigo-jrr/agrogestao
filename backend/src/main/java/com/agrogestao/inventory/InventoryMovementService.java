package com.agrogestao.inventory;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.inventory.dto.InventoryMovementRequest;
import com.agrogestao.inventory.dto.InventoryMovementResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Movimentações de estoque com custo médio ponderado:
 * - entrada com custo recalcula a média: (saldo × média + qtd × custo) / (saldo + qtd);
 * - saída não altera a média e registra o custo médio do momento (custo da mercadoria);
 * - ajuste redefine o saldo (e a média, se um custo for informado).
 */
@Service
public class InventoryMovementService {

    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public InventoryMovementService(
            InventoryMovementRepository inventoryMovementRepository,
            InventoryItemRepository inventoryItemRepository,
            ProductRepository productRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.productRepository = productRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<InventoryMovementResponse> list(Long productId, Period period, Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<InventoryMovement> movements = productId != null
                ? inventoryMovementRepository.findByPropertyIdAndProductIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
                        propertyId, productId, period.from(), period.to(), pageable)
                : inventoryMovementRepository.findByPropertyIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(
                        propertyId, period.from(), period.to(), pageable);
        Map<Long, Product> products = productRepository.findByPropertyId(propertyId).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return PageResponse.of(movements, movement -> toResponse(movement, products.get(movement.getProductId())));
    }

    @Transactional
    public InventoryMovementResponse create(InventoryMovementRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Product product = requireActiveProduct(propertyId, request.getProductId());
        if (request.getType() != MovementType.ADJUST && request.getQuantity().signum() == 0) {
            throw new BusinessException("Informe uma quantidade maior que zero para entradas e saídas.");
        }
        InventoryMovement movement = registerMovement(propertyId, product.getId(), request.getType(),
                request.getQuantity(), request.getUnitCost(), request.getMovementDate(), request.getNotes());
        auditService.record("InventoryMovement", movement.getId(), AuditAction.CREATE,
                "%s de %s %s de %s".formatted(switch (movement.getType()) {
                    case IN -> "Entrada";
                    case OUT -> "Saída";
                    case ADJUST -> "Ajuste para";
                }, movement.getQuantity().stripTrailingZeros().toPlainString(), product.getUnit(), product.getName()));
        return toResponse(movement, product);
    }

    /** Produto da propriedade e não arquivado — exigido para qualquer operação nova. */
    public Product requireActiveProduct(Long propertyId, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Produto inválido para esta propriedade"));
        if (product.isArchived()) {
            throw new BusinessException("O produto " + product.getName() + " está arquivado. Reative-o para usar.");
        }
        return product;
    }

    /**
     * Registra a movimentação e atualiza saldo e custo médio. Em saídas, o custo informado é ignorado:
     * vale o custo médio do momento, que fica gravado na movimentação.
     */
    @Transactional
    public InventoryMovement registerMovement(Long propertyId, Long productId, MovementType type,
            BigDecimal quantity, BigDecimal unitCost, LocalDate date, String notes) {
        InventoryItem item = findOrCreateItem(propertyId, productId);
        BigDecimal movementCost = unitCost;

        switch (type) {
            case IN -> {
                BigDecimal newQuantity = item.getQuantity().add(quantity);
                if (unitCost != null && newQuantity.signum() > 0) {
                    item.setAverageCost(item.getQuantity().multiply(item.getAverageCost())
                            .add(quantity.multiply(unitCost))
                            .divide(newQuantity, 2, RoundingMode.HALF_UP));
                }
                item.setQuantity(newQuantity);
            }
            case OUT -> {
                BigDecimal resulting = item.getQuantity().subtract(quantity);
                if (resulting.signum() < 0) {
                    throw new BusinessException("Estoque insuficiente para esta saída");
                }
                item.setQuantity(resulting);
                movementCost = item.getAverageCost();
            }
            case ADJUST -> {
                item.setQuantity(quantity);
                if (unitCost != null) {
                    item.setAverageCost(unitCost);
                }
            }
        }
        inventoryItemRepository.save(item);
        return saveMovement(propertyId, productId, type, quantity, movementCost, date, notes);
    }

    /**
     * Desfaz uma entrada (cancelamento de compra): tira a quantidade e remove o efeito dela na média,
     * usando o custo original da entrada.
     */
    @Transactional
    public InventoryMovement reverseEntry(Long propertyId, Long productId, BigDecimal quantity, BigDecimal unitCost,
            LocalDate date, String notes) {
        InventoryItem item = findOrCreateItem(propertyId, productId);
        BigDecimal resulting = item.getQuantity().subtract(quantity);
        if (resulting.signum() < 0) {
            throw new BusinessException("Estoque insuficiente para esta saída");
        }
        if (resulting.signum() > 0) {
            BigDecimal average = item.getQuantity().multiply(item.getAverageCost())
                    .subtract(quantity.multiply(unitCost))
                    .divide(resulting, 2, RoundingMode.HALF_UP);
            // Se houve ajustes manuais depois da compra, a conta pode ficar negativa: mantém a média atual.
            if (average.signum() >= 0) {
                item.setAverageCost(average);
            }
        }
        item.setQuantity(resulting);
        inventoryItemRepository.save(item);
        return saveMovement(propertyId, productId, MovementType.OUT, quantity, unitCost, date, notes);
    }

    private InventoryItem findOrCreateItem(Long propertyId, Long productId) {
        return inventoryItemRepository.findByPropertyIdAndProductId(propertyId, productId)
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setPropertyId(propertyId);
                    newItem.setProductId(productId);
                    return newItem;
                });
    }

    private InventoryMovement saveMovement(Long propertyId, Long productId, MovementType type, BigDecimal quantity,
            BigDecimal unitCost, LocalDate date, String notes) {
        InventoryMovement movement = new InventoryMovement();
        movement.setPropertyId(propertyId);
        movement.setProductId(productId);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setMovementDate(date);
        movement.setNotes(notes);
        return inventoryMovementRepository.save(movement);
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement, Product product) {
        return new InventoryMovementResponse(movement.getId(), movement.getProductId(),
                product != null ? product.getName() : null, product != null ? product.getUnit() : null,
                movement.getType(), movement.getQuantity(), movement.getUnitCost(), movement.getMovementDate(),
                movement.getNotes());
    }
}
