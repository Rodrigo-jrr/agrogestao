package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropHarvestRequest;
import com.agrogestao.agriculture.dto.CropHarvestResponse;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.fields.Field;
import com.agrogestao.fields.FieldRepository;
import com.agrogestao.finance.AccountReceivableService;
import com.agrogestao.finance.FinancialTransaction;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.InstallmentPlan;
import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.InventoryMovement;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.Product;
import com.agrogestao.inventory.ProductRepository;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.Customer;
import com.agrogestao.sales.CustomerRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Colheita de uma safra: vai para o estoque (produto já cadastrado, ao custo informado) ou vira receita
 * direto — à vista ou a prazo — sem passar pelo módulo Vendas, mesmo padrão de venda de animais. Evento
 * pontual: sem arquivamento, exclusão desfaz o estoque ou o financeiro antes de remover o registro.
 */
@Service
public class CropHarvestService {

    private final CropHarvestRepository harvestRepository;
    private final CropCycleRepository cropCycleRepository;
    private final FieldRepository fieldRepository;
    private final CropRepository cropRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AccountReceivableService accountReceivableService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CropHarvestService(CropHarvestRepository harvestRepository, CropCycleRepository cropCycleRepository,
            FieldRepository fieldRepository, CropRepository cropRepository, CustomerRepository customerRepository,
            ProductRepository productRepository, InventoryMovementService inventoryMovementService,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            AccountReceivableService accountReceivableService, TenantContext tenantContext, AuditService auditService) {
        this.harvestRepository = harvestRepository;
        this.cropCycleRepository = cropCycleRepository;
        this.fieldRepository = fieldRepository;
        this.cropRepository = cropRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.accountReceivableService = accountReceivableService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<CropHarvestResponse> list(Long cropCycleId, Long fieldId, LocalDate from, LocalDate to,
            Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<CropHarvest> page = harvestRepository.search(propertyId, cropCycleId, fieldId, from, to, pageable);
        return PageResponse.of(page, harvest -> toResponse(harvest, requireCycle(propertyId, harvest.getCropCycleId())));
    }

    @Transactional
    public CropHarvestResponse create(CropHarvestRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        if (request.harvestDate().isAfter(LocalDate.now())) {
            throw new BusinessException("A data não pode ser futura.");
        }
        CropCycle cycle = requireCycle(propertyId, request.cropCycleId());
        if (cycle.isArchived()) {
            throw new BusinessException("Esta safra está arquivada e não recebe novas colheitas.");
        }

        CropHarvest harvest = new CropHarvest();
        harvest.setPropertyId(propertyId);
        harvest.setCropCycleId(cycle.getId());
        harvest.setHarvestDate(request.harvestDate());
        harvest.setQuantity(request.quantity());
        harvest.setDestination(request.destination());

        if (request.destination() == CropHarvestDestination.ESTOQUE) {
            if (request.productId() == null) {
                throw new BusinessException("Escolha o produto do estoque que recebe a colheita.");
            }
            if (request.unitCost() == null) {
                throw new BusinessException("Informe o custo unitário para dar entrada no estoque.");
            }
            Product product = inventoryMovementService.requireActiveProduct(propertyId, request.productId());
            InventoryMovement movement = inventoryMovementService.registerMovement(propertyId, product.getId(),
                    MovementType.IN, request.quantity(), request.unitCost(),
                    request.harvestDate(), "Colheita — " + describeCycle(cycle));
            harvest.setQuantityUnit(product.getUnit());
            harvest.setProductId(product.getId());
            harvest.setUnitCost(request.unitCost());
            harvest.setInventoryMovementId(movement.getId());
            harvestRepository.save(harvest);
        } else {
            if (request.customerId() == null) {
                throw new BusinessException("Escolha o cliente da venda.");
            }
            if (request.totalAmount() == null) {
                throw new BusinessException("Informe o valor total da venda.");
            }
            Customer customer = customerRepository.findById(request.customerId())
                    .filter(c -> c.getPropertyId().equals(propertyId))
                    .orElseThrow(() -> new BusinessException("Cliente inválido para esta propriedade"));
            PaymentType paymentType = InstallmentPlan.validate(request.paymentType(), request.installments(),
                    request.totalAmount(), request.harvestDate());
            harvest.setQuantityUnit(request.quantityUnit().trim());
            harvest.setCustomerId(customer.getId());
            harvest.setTotalAmount(request.totalAmount());
            harvest.setPaymentType(paymentType);
            harvestRepository.save(harvest);

            if (paymentType == PaymentType.CASH) {
                FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                        TransactionType.INCOME, "Venda de colheita #" + harvest.getId(), request.totalAmount(),
                        request.harvestDate(), null);
                harvest.setFinancialTransactionId(transaction.getId());
                harvestRepository.save(harvest);
            } else {
                accountReceivableService.createCropHarvestInstallments(propertyId, customer.getId(), harvest.getId(),
                        request.installments());
            }
        }

        auditService.record("CropHarvest", harvest.getId(), AuditAction.CREATE,
                describe(harvest) + " em " + describeCycle(cycle));
        return toResponse(harvest, cycle);
    }

    /** Desfaz uma colheita lançada errada: devolve o estoque ou remove a receita/parcelas antes de apagar. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        CropHarvest harvest = findOwned(propertyId, id);
        Long financialTransactionId = harvest.getFinancialTransactionId();
        if (harvest.getDestination() == CropHarvestDestination.ESTOQUE) {
            if (harvest.getInventoryMovementId() != null) {
                inventoryMovementService.reverseEntry(propertyId, harvest.getProductId(), harvest.getQuantity(),
                        harvest.getUnitCost(), LocalDate.now(), "Colheita desfeita");
            }
        } else if (financialTransactionId == null) {
            // accounts_receivable referencia esta colheita: remove as parcelas antes de apagar a linha.
            accountReceivableService.removeCropHarvestInstallments(id);
        }
        harvestRepository.delete(harvest);
        if (financialTransactionId != null) {
            // Esta colheita referenciava a transação: só apaga a transação depois de apagar a colheita.
            harvestRepository.flush();
            financialTransactionRepository.deleteById(financialTransactionId);
        }
        auditService.record("CropHarvest", id, AuditAction.DELETE, "colheita desfeita: " + describe(harvest));
    }

    private CropHarvest findOwned(Long propertyId, Long id) {
        return harvestRepository.findById(id)
                .filter(harvest -> harvest.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Colheita não encontrada"));
    }

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

    /** "3.000 kg → estoque" ou "receita de R$ 12.000,00 (venda)" */
    private static String describe(CropHarvest harvest) {
        String quantity = harvest.getQuantity().stripTrailingZeros().toPlainString() + " " + harvest.getQuantityUnit();
        return harvest.getDestination() == CropHarvestDestination.ESTOQUE
                ? quantity + " para o estoque"
                : quantity + " vendida por " + Money.brl(harvest.getTotalAmount());
    }

    private CropHarvestResponse toResponse(CropHarvest harvest, CropCycle cycle) {
        Field field = fieldOf(cycle);
        Crop crop = cropOf(cycle);
        String productName = harvest.getProductId() != null
                ? productRepository.findById(harvest.getProductId()).map(Product::getName).orElse(null) : null;
        String customerName = harvest.getCustomerId() != null
                ? customerRepository.findById(harvest.getCustomerId()).map(Customer::getName).orElse(null) : null;
        return new CropHarvestResponse(harvest.getId(), harvest.getCropCycleId(), field.getId(), field.getName(),
                crop.getId(), crop.getName(), harvest.getHarvestDate(), harvest.getQuantity(), harvest.getQuantityUnit(),
                harvest.getDestination(), harvest.getProductId(), productName, harvest.getUnitCost(),
                harvest.getCustomerId(), customerName, harvest.getTotalAmount(), harvest.getPaymentType());
    }
}
