package com.agrogestao.purchases;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.FinancialTransaction;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.ProductRepository;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.dto.PurchaseRequest;
import com.agrogestao.purchases.dto.PurchaseResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            InventoryMovementService inventoryMovementService,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<PurchaseResponse> list() {
        return purchaseRepository.findByPropertyIdOrderByPurchaseDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();

        if (!supplierRepository.existsByIdAndPropertyId(request.getSupplierId(), propertyId)) {
            throw new BusinessException("Fornecedor inválido para esta propriedade");
        }
        request.getItems().forEach(item -> {
            if (!productRepository.existsByIdAndPropertyId(item.getProductId(), propertyId)) {
                throw new BusinessException("Produto inválido para esta propriedade");
            }
        });

        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Purchase purchase = new Purchase();
        purchase.setPropertyId(propertyId);
        purchase.setSupplierId(request.getSupplierId());
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setTotalAmount(total);
        purchaseRepository.save(purchase);

        request.getItems().forEach(item -> {
            PurchaseItem purchaseItem = new PurchaseItem();
            purchaseItem.setPurchaseId(purchase.getId());
            purchaseItem.setProductId(item.getProductId());
            purchaseItem.setQuantity(item.getQuantity());
            purchaseItem.setUnitPrice(item.getUnitPrice());
            purchaseItemRepository.save(purchaseItem);

            inventoryMovementService.registerMovement(propertyId, item.getProductId(), MovementType.IN,
                    item.getQuantity(), item.getUnitPrice(), request.getPurchaseDate(),
                    "Compra #" + purchase.getId());
        });

        FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                TransactionType.EXPENSE, "Compra #" + purchase.getId(), total, request.getPurchaseDate(), null);
        purchase.setFinancialTransactionId(transaction.getId());
        purchaseRepository.save(purchase);
        auditService.record("Purchase", purchase.getId(), AuditAction.CREATE, "total " + purchase.getTotalAmount());

        return toResponse(purchase);
    }

    /**
     * Cancela a compra mantendo o histórico: estorna o estoque com um movimento inverso e remove o
     * lançamento financeiro gerado (ou registra um estorno, para registros antigos sem vínculo).
     */
    @Transactional
    public PurchaseResponse cancel(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Purchase purchase = purchaseRepository.findById(id)
                .filter(found -> found.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Compra não encontrada"));
        if (purchase.getCanceledAt() != null) {
            throw new BusinessException("Esta compra já foi cancelada.");
        }

        purchaseItemRepository.findByPurchaseId(id).forEach(item -> {
            try {
                inventoryMovementService.registerMovement(propertyId, item.getProductId(), MovementType.OUT,
                        item.getQuantity(), item.getUnitPrice(), LocalDate.now(), "Cancelamento da compra #" + id);
            } catch (BusinessException ex) {
                throw new BusinessException(
                        "Não é possível cancelar: parte do estoque desta compra já foi consumida.");
            }
        });

        Long transactionId = purchase.getFinancialTransactionId();
        purchase.setFinancialTransactionId(null);
        purchase.setCanceledAt(LocalDateTime.now());
        purchaseRepository.saveAndFlush(purchase);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        } else {
            financialTransactionService.registerFromOperation(propertyId, TransactionType.INCOME,
                    "Estorno da compra #" + id, purchase.getTotalAmount(), LocalDate.now(), null);
        }
        auditService.record("Purchase", purchase.getId(), AuditAction.CANCEL, "total " + purchase.getTotalAmount());
        return toResponse(purchase);
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getSupplierId(),
                purchase.getPurchaseDate(), purchase.getTotalAmount(),
                purchase.getCanceledAt() != null);
    }
}
