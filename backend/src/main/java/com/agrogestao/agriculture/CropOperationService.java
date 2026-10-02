package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropOperationRequest;
import com.agrogestao.agriculture.dto.CropOperationResponse;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.fields.Field;
import com.agrogestao.fields.FieldRepository;
import com.agrogestao.inventory.InventoryMovement;
import com.agrogestao.inventory.InventoryMovementRepository;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.Product;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operações agrícolas (preparo, plantio, adubação, pulverização) numa safra. Um insumo do estoque opcional
 * sai pela quantidade informada, ao custo médio — sem produto do estoque, nome e custo ficam como digitados.
 * Evento pontual: sem arquivamento, exclusão desfaz o movimento de estoque (se houver) e remove o registro.
 */
@Service
public class CropOperationService {

    private final CropOperationRepository operationRepository;
    private final CropCycleRepository cropCycleRepository;
    private final FieldRepository fieldRepository;
    private final CropRepository cropRepository;
    private final InventoryMovementService inventoryMovementService;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CropOperationService(CropOperationRepository operationRepository, CropCycleRepository cropCycleRepository,
            FieldRepository fieldRepository, CropRepository cropRepository,
            InventoryMovementService inventoryMovementService, InventoryMovementRepository inventoryMovementRepository,
            TenantContext tenantContext, AuditService auditService) {
        this.operationRepository = operationRepository;
        this.cropCycleRepository = cropCycleRepository;
        this.fieldRepository = fieldRepository;
        this.cropRepository = cropRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<CropOperationResponse> list(Long cropCycleId, Long fieldId, LocalDate from, LocalDate to,
            Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<CropOperation> page = operationRepository.search(propertyId, cropCycleId, fieldId, from, to, pageable);
        return PageResponse.of(page, operation -> toResponse(operation, requireCycle(propertyId, operation.getCropCycleId())));
    }

    @Transactional
    public CropOperationResponse create(CropOperationRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        if (request.operationDate().isAfter(LocalDate.now())) {
            throw new BusinessException("A data não pode ser futura.");
        }
        CropCycle cycle = requireCycle(propertyId, request.cropCycleId());
        if (cycle.isArchived()) {
            throw new BusinessException("Esta safra está arquivada e não recebe novas operações.");
        }

        CropOperation operation = new CropOperation();
        operation.setPropertyId(propertyId);
        operation.setCropCycleId(cycle.getId());
        operation.setKind(request.kind());
        operation.setOperationDate(request.operationDate());
        operation.setResponsible(blankToNull(request.responsible()));
        operation.setMachine(blankToNull(request.machine()));
        operation.setNotes(blankToNull(request.notes()));

        if (request.productId() != null) {
            Product product = inventoryMovementService.requireActiveProduct(propertyId, request.productId());
            if (request.quantity() == null) {
                throw new BusinessException("Informe a quantidade usada (em " + product.getUnit() + ").");
            }
            InventoryMovement movement = inventoryMovementService.registerMovement(propertyId, product.getId(),
                    MovementType.OUT, request.quantity(), null, request.operationDate(),
                    request.kind().label() + " — " + describeCycle(cycle));
            BigDecimal unitCost = movement.getUnitCost() != null ? movement.getUnitCost() : BigDecimal.ZERO;
            operation.setProductId(product.getId());
            operation.setProductName(product.getName());
            operation.setQuantity(request.quantity());
            operation.setQuantityUnit(product.getUnit());
            operation.setTotalCost(unitCost.multiply(request.quantity()).setScale(2, RoundingMode.HALF_UP));
            operation.setInventoryMovementId(movement.getId());
        } else {
            operation.setProductName(blankToNull(request.productName()));
            operation.setQuantity(request.quantity());
            operation.setQuantityUnit(blankToNull(request.quantityUnit()));
            operation.setTotalCost(request.cost() != null ? request.cost().setScale(2, RoundingMode.HALF_UP) : null);
        }
        operationRepository.save(operation);

        auditService.record("CropOperation", operation.getId(), AuditAction.CREATE,
                describe(operation) + " em " + describeCycle(cycle)
                        + (operation.getTotalCost() != null ? " (" + Money.brl(operation.getTotalCost()) + ")" : ""));
        return toResponse(operation, cycle);
    }

    /** Desfaz uma operação lançada errada: o insumo volta ao estoque (se houver) e o registro é removido. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        CropOperation operation = findOwned(propertyId, id);
        if (operation.getInventoryMovementId() != null) {
            InventoryMovement out = inventoryMovementRepository.findById(operation.getInventoryMovementId()).orElseThrow();
            inventoryMovementService.registerMovement(propertyId, out.getProductId(), MovementType.IN, out.getQuantity(),
                    out.getUnitCost(), LocalDate.now(), "Operação desfeita: " + operation.getProductName());
        }
        operationRepository.delete(operation);
        auditService.record("CropOperation", id, AuditAction.DELETE, "operação desfeita: " + describe(operation));
    }

    private CropOperation findOwned(Long propertyId, Long id) {
        return operationRepository.findById(id)
                .filter(operation -> operation.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada"));
    }

    /** Safra da propriedade pelo id — precisa existir para criar ou listar operações. */
    private CropCycle requireCycle(Long propertyId, Long cropCycleId) {
        return cropCycleRepository.findById(cropCycleId)
                .filter(cycle -> cycle.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Safra inválida para esta propriedade"));
    }

    private Field fieldOf(CropCycle cycle) {
        return fieldRepository.findById(cycle.getFieldId()).orElseThrow();
    }

    private Crop cropOf(CropCycle cycle) {
        return cropRepository.findById(cycle.getCropId()).orElseThrow();
    }

    private String describeCycle(CropCycle cycle) {
        return cropOf(cycle).getName() + " em " + fieldOf(cycle).getName();
    }

    /** "Pulverização: Glifosato (2 L)" */
    private static String describe(CropOperation operation) {
        String quantity = operation.getQuantity() != null
                ? " (" + operation.getQuantity().stripTrailingZeros().toPlainString()
                        + (operation.getQuantityUnit() != null ? " " + operation.getQuantityUnit() : "") + ")"
                : "";
        return operation.getKind().label() + (operation.getProductName() != null ? ": " + operation.getProductName() : "") + quantity;
    }

    private CropOperationResponse toResponse(CropOperation operation, CropCycle cycle) {
        Field field = fieldOf(cycle);
        Crop crop = cropOf(cycle);
        return new CropOperationResponse(operation.getId(), operation.getCropCycleId(), field.getId(), field.getName(),
                crop.getId(), crop.getName(), operation.getKind(), operation.getOperationDate(), operation.getProductId(),
                operation.getProductName(), operation.getQuantity(), operation.getQuantityUnit(), operation.getTotalCost(),
                operation.getResponsible(), operation.getMachine(), operation.getNotes());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
