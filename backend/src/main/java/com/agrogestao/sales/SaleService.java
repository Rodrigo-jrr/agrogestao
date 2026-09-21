package com.agrogestao.sales;

import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.inventory.InventoryMovementService;
import com.agrogestao.inventory.MovementType;
import com.agrogestao.inventory.ProductRepository;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.sales.dto.SaleRequest;
import com.agrogestao.sales.dto.SaleResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final TenantContext tenantContext;

    public SaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryMovementService inventoryMovementService,
            FinancialTransactionService financialTransactionService,
            TenantContext tenantContext) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.tenantContext = tenantContext;
    }

    public List<SaleResponse> list() {
        return saleRepository.findByPropertyIdOrderBySaleDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();

        if (!customerRepository.existsByIdAndPropertyId(request.getCustomerId(), propertyId)) {
            throw new BusinessException("Cliente inválido para esta propriedade");
        }
        request.getItems().forEach(item -> {
            if (!productRepository.existsByIdAndPropertyId(item.getProductId(), propertyId)) {
                throw new BusinessException("Produto inválido para esta propriedade");
            }
        });

        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale sale = new Sale();
        sale.setPropertyId(propertyId);
        sale.setCustomerId(request.getCustomerId());
        sale.setSaleDate(request.getSaleDate());
        sale.setTotalAmount(total);
        saleRepository.save(sale);

        request.getItems().forEach(item -> {
            SaleItem saleItem = new SaleItem();
            saleItem.setSaleId(sale.getId());
            saleItem.setProductId(item.getProductId());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItemRepository.save(saleItem);

            inventoryMovementService.registerMovement(propertyId, item.getProductId(), MovementType.OUT,
                    item.getQuantity(), item.getUnitPrice(), request.getSaleDate(), "Venda #" + sale.getId());
        });

        financialTransactionService.registerFromOperation(propertyId, TransactionType.INCOME,
                "Venda #" + sale.getId(), total, request.getSaleDate(), null);

        return toResponse(sale);
    }

    private SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getCustomerId(), sale.getSaleDate(), sale.getTotalAmount());
    }
}
