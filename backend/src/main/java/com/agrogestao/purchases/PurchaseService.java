package com.agrogestao.purchases;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.AccountPayableService;
import com.agrogestao.finance.FinancialTransaction;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.InstallmentPlan;
import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.Product;
import com.agrogestao.inventory.ProductRepository;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.dto.PurchaseDetailResponse;
import com.agrogestao.purchases.dto.PurchaseRequest;
import com.agrogestao.purchases.dto.PurchaseResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    /** Limite da coluna DECIMAL(15,2). */
    private static final BigDecimal MAX_TOTAL = new BigDecimal("9999999999999.99");

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AccountPayableService accountPayableService;
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
            AccountPayableService accountPayableService,
            TenantContext tenantContext,
            AuditService auditService) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.accountPayableService = accountPayableService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<PurchaseResponse> list() {
        return purchaseRepository.findByPropertyIdOrderByPurchaseDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public PurchaseDetailResponse detail(Long id) {
        Purchase purchase = findOwned(id, tenantContext.requirePropertyId());
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseId(id);
        Map<Long, Product> products = productRepository.findAllById(items.stream().map(PurchaseItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        String supplierName = supplierRepository.findById(purchase.getSupplierId()).map(Supplier::getName).orElse(null);
        return new PurchaseDetailResponse(purchase.getId(), purchase.getSupplierId(), supplierName,
                purchase.getPurchaseDate(), purchase.getTotalAmount(), purchase.getPaymentType(),
                purchase.getCanceledAt() != null,
                items.stream().map(item -> {
                    Product product = products.get(item.getProductId());
                    return new PurchaseDetailResponse.Item(item.getProductId(),
                            product != null ? product.getName() : null, product != null ? product.getUnit() : null,
                            item.getQuantity(), item.getUnitPrice(), item.getUnitPrice().multiply(item.getQuantity()));
                }).toList(),
                accountPayableService.installmentsOf(id));
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();

        if (!supplierRepository.existsByIdAndPropertyId(request.getSupplierId(), propertyId)) {
            throw new BusinessException("Fornecedor inválido para esta propriedade");
        }
        request.getItems().forEach(item -> inventoryMovementService.requireActiveProduct(propertyId, item.getProductId()));

        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(MAX_TOTAL) > 0) {
            throw new BusinessException("O valor total da operação ultrapassa o limite permitido.");
        }
        PaymentType paymentType = InstallmentPlan.validate(
                request.getPaymentType(), request.getInstallments(), total, request.getPurchaseDate());

        Purchase purchase = new Purchase();
        purchase.setPropertyId(propertyId);
        purchase.setSupplierId(request.getSupplierId());
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setTotalAmount(total);
        purchase.setPaymentType(paymentType);
        purchase.setInstallmentCount(paymentType == PaymentType.INSTALLMENTS ? request.getInstallments().size() : 0);
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

        if (paymentType == PaymentType.CASH) {
            FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                    TransactionType.EXPENSE, "Compra #" + purchase.getId(), total, request.getPurchaseDate(), null);
            purchase.setFinancialTransactionId(transaction.getId());
            purchaseRepository.save(purchase);
        } else {
            accountPayableService.createInstallments(propertyId, request.getSupplierId(), purchase.getId(),
                    request.getInstallments());
        }
        auditService.record("Purchase", purchase.getId(), AuditAction.CREATE, describe(purchase));

        return toResponse(purchase);
    }

    /**
     * Cancela a compra mantendo o histórico: estorna o estoque e desfaz o financeiro — remove o lançamento
     * (à vista) ou as parcelas ainda não pagas (a prazo). Registros antigos sem vínculo recebem um estorno.
     */
    @Transactional
    public PurchaseResponse cancel(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Purchase purchase = findOwned(id, propertyId);
        if (purchase.getCanceledAt() != null) {
            throw new BusinessException("Esta compra já foi cancelada.");
        }
        if (purchase.getPaymentType() == PaymentType.INSTALLMENTS) {
            accountPayableService.removeInstallments(id);
        }

        purchaseItemRepository.findByPurchaseId(id).forEach(item -> {
            try {
                inventoryMovementService.reverseEntry(propertyId, item.getProductId(), item.getQuantity(),
                        item.getUnitPrice(), LocalDate.now(), "Cancelamento da compra #" + id);
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
        } else if (purchase.getPaymentType() == PaymentType.CASH) {
            financialTransactionService.registerFromOperation(propertyId, TransactionType.INCOME,
                    "Estorno da compra #" + id, purchase.getTotalAmount(), LocalDate.now(), null);
        }
        auditService.record("Purchase", purchase.getId(), AuditAction.CANCEL, describe(purchase));
        return toResponse(purchase);
    }

    private Purchase findOwned(Long id, Long propertyId) {
        return purchaseRepository.findById(id)
                .filter(found -> found.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Compra não encontrada"));
    }

    private static String describe(Purchase purchase) {
        return "total " + purchase.getTotalAmount()
                + (purchase.getPaymentType() == PaymentType.INSTALLMENTS
                        ? " em " + purchase.getInstallmentCount() + " parcela(s)" : " à vista");
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getSupplierId(),
                purchase.getPurchaseDate(), purchase.getTotalAmount(),
                purchase.getCanceledAt() != null, purchase.getPaymentType(), purchase.getInstallmentCount());
    }
}
