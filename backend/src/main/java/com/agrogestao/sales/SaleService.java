package com.agrogestao.sales;

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
import com.agrogestao.sales.dto.SaleRequest;
import com.agrogestao.sales.dto.SaleResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

    /** Limite da coluna DECIMAL(15,2). */
    private static final BigDecimal MAX_TOTAL = new BigDecimal("9999999999999.99");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService inventoryMovementService;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public SaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryMovementService inventoryMovementService,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
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
        if (total.compareTo(MAX_TOTAL) > 0) {
            throw new BusinessException("O valor total da operação ultrapassa o limite permitido.");
        }

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

        FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                TransactionType.INCOME, "Venda #" + sale.getId(), total, request.getSaleDate(), null);
        sale.setFinancialTransactionId(transaction.getId());
        saleRepository.save(sale);
        auditService.record("Sale", sale.getId(), AuditAction.CREATE, "total " + sale.getTotalAmount());

        return toResponse(sale);
    }

    /**
     * Cancela a venda mantendo o histórico: estorna o estoque com um movimento inverso e remove o
     * lançamento financeiro gerado (ou registra um estorno, para registros antigos sem vínculo).
     */
    @Transactional
    public SaleResponse cancel(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Sale sale = saleRepository.findById(id)
                .filter(found -> found.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada"));
        if (sale.getCanceledAt() != null) {
            throw new BusinessException("Esta venda já foi cancelada.");
        }

        saleItemRepository.findBySaleId(id).forEach(item -> {
            inventoryMovementService.registerMovement(propertyId, item.getProductId(), MovementType.IN,
                    item.getQuantity(), item.getUnitPrice(), LocalDate.now(), "Cancelamento da venda #" + id);
        });

        Long transactionId = sale.getFinancialTransactionId();
        sale.setFinancialTransactionId(null);
        sale.setCanceledAt(LocalDateTime.now());
        saleRepository.saveAndFlush(sale);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        } else {
            financialTransactionService.registerFromOperation(propertyId, TransactionType.EXPENSE,
                    "Estorno da venda #" + id, sale.getTotalAmount(), LocalDate.now(), null);
        }
        auditService.record("Sale", sale.getId(), AuditAction.CANCEL, "total " + sale.getTotalAmount());
        return toResponse(sale);
    }

    private SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getCustomerId(), sale.getSaleDate(), sale.getTotalAmount(),
                sale.getCanceledAt() != null);
    }
}
