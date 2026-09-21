package com.agrogestao.purchases;

import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.dto.PurchaseRequest;
import com.agrogestao.purchases.dto.PurchaseResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final TenantContext tenantContext;

    public PurchaseService(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            InventoryMovementService inventoryMovementService,
            FinancialTransactionService financialTransactionService,
            TenantContext tenantContext) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.tenantContext = tenantContext;
    }

    public List<PurchaseResponse> list() {
        return purchaseRepository.findByPropertyIdOrderByPurchaseDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        Long propertyId = tenantContext.requirePropertyId();

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

        financialTransactionService.registerFromOperation(propertyId, TransactionType.EXPENSE,
                "Compra #" + purchase.getId(), total, request.getPurchaseDate(), null);

        return toResponse(purchase);
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getSupplierId(),
                purchase.getPurchaseDate(), purchase.getTotalAmount());
    }
}
