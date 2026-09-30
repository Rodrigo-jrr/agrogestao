package com.agrogestao.sales;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
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
import com.agrogestao.sales.dto.SaleDetailResponse;
import com.agrogestao.sales.dto.SaleRequest;
import com.agrogestao.sales.dto.SaleResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final AccountReceivableService accountReceivableService;
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
            AccountReceivableService accountReceivableService,
            TenantContext tenantContext,
            AuditService auditService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementService = inventoryMovementService;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.accountReceivableService = accountReceivableService;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<SaleResponse> list() {
        return saleRepository.findByPropertyIdOrderBySaleDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public SaleDetailResponse detail(Long id) {
        Sale sale = findOwned(id, tenantContext.requirePropertyId());
        List<SaleItem> items = saleItemRepository.findBySaleId(id);
        Map<Long, Product> products = productRepository.findAllById(items.stream().map(SaleItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        String customerName = customerRepository.findById(sale.getCustomerId()).map(Customer::getName).orElse(null);

        boolean allCosted = items.stream().allMatch(item -> item.getUnitCost() != null);
        BigDecimal totalCost = allCosted
                ? items.stream().map(item -> item.getUnitCost().multiply(item.getQuantity()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP)
                : null;

        return new SaleDetailResponse(sale.getId(), sale.getCustomerId(), customerName, sale.getSaleDate(),
                sale.getTotalAmount(), totalCost, totalCost != null ? sale.getTotalAmount().subtract(totalCost) : null,
                sale.getPaymentType(), sale.getCanceledAt() != null,
                items.stream().map(item -> {
                    Product product = products.get(item.getProductId());
                    return new SaleDetailResponse.Item(item.getProductId(),
                            product != null ? product.getName() : null, product != null ? product.getUnit() : null,
                            item.getQuantity(), item.getUnitPrice(), item.getUnitCost(),
                            item.getUnitPrice().multiply(item.getQuantity()));
                }).toList(),
                accountReceivableService.installmentsOf(id));
    }

    @Transactional
    public SaleResponse create(SaleRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();

        if (!customerRepository.existsByIdAndPropertyId(request.getCustomerId(), propertyId)) {
            throw new BusinessException("Cliente inválido para esta propriedade");
        }
        request.getItems().forEach(item -> inventoryMovementService.requireActiveProduct(propertyId, item.getProductId()));

        BigDecimal total = request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(MAX_TOTAL) > 0) {
            throw new BusinessException("O valor total da operação ultrapassa o limite permitido.");
        }
        PaymentType paymentType = InstallmentPlan.validate(
                request.getPaymentType(), request.getInstallments(), total, request.getSaleDate());

        Sale sale = new Sale();
        sale.setPropertyId(propertyId);
        sale.setCustomerId(request.getCustomerId());
        sale.setSaleDate(request.getSaleDate());
        sale.setTotalAmount(total);
        sale.setPaymentType(paymentType);
        sale.setInstallmentCount(paymentType == PaymentType.INSTALLMENTS ? request.getInstallments().size() : 0);
        saleRepository.save(sale);

        request.getItems().forEach(item -> {
            // A saída grava o custo médio do momento; guardamos no item para estornar pelo mesmo custo.
            InventoryMovement movement = inventoryMovementService.registerMovement(propertyId, item.getProductId(),
                    MovementType.OUT, item.getQuantity(), null, request.getSaleDate(), "Venda #" + sale.getId());

            SaleItem saleItem = new SaleItem();
            saleItem.setSaleId(sale.getId());
            saleItem.setProductId(item.getProductId());
            saleItem.setQuantity(item.getQuantity());
            saleItem.setUnitPrice(item.getUnitPrice());
            saleItem.setUnitCost(movement.getUnitCost());
            saleItemRepository.save(saleItem);
        });

        if (paymentType == PaymentType.CASH) {
            FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                    TransactionType.INCOME, "Venda #" + sale.getId(), total, request.getSaleDate(), null);
            sale.setFinancialTransactionId(transaction.getId());
            saleRepository.save(sale);
        } else {
            accountReceivableService.createInstallments(propertyId, request.getCustomerId(), sale.getId(),
                    request.getInstallments());
        }
        auditService.record("Sale", sale.getId(), AuditAction.CREATE, describe(sale));

        return toResponse(sale);
    }

    /**
     * Cancela a venda mantendo o histórico: devolve os itens ao estoque pelo custo registrado e desfaz o
     * financeiro — remove o lançamento (à vista) ou as parcelas ainda não recebidas (a prazo).
     */
    @Transactional
    public SaleResponse cancel(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Sale sale = findOwned(id, propertyId);
        if (sale.getCanceledAt() != null) {
            throw new BusinessException("Esta venda já foi cancelada.");
        }
        if (sale.getPaymentType() == PaymentType.INSTALLMENTS) {
            accountReceivableService.removeInstallments(id);
        }

        saleItemRepository.findBySaleId(id).forEach(item -> inventoryMovementService.registerMovement(propertyId,
                item.getProductId(), MovementType.IN, item.getQuantity(), item.getUnitCost(), LocalDate.now(),
                "Cancelamento da venda #" + id));

        Long transactionId = sale.getFinancialTransactionId();
        sale.setFinancialTransactionId(null);
        sale.setCanceledAt(LocalDateTime.now());
        saleRepository.saveAndFlush(sale);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        } else if (sale.getPaymentType() == PaymentType.CASH) {
            financialTransactionService.registerFromOperation(propertyId, TransactionType.EXPENSE,
                    "Estorno da venda #" + id, sale.getTotalAmount(), LocalDate.now(), null);
        }
        auditService.record("Sale", sale.getId(), AuditAction.CANCEL, describe(sale));
        return toResponse(sale);
    }

    private Sale findOwned(Long id, Long propertyId) {
        return saleRepository.findById(id)
                .filter(found -> found.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada"));
    }

    private static String describe(Sale sale) {
        return "total " + sale.getTotalAmount()
                + (sale.getPaymentType() == PaymentType.INSTALLMENTS
                        ? " em " + sale.getInstallmentCount() + " parcela(s)" : " à vista");
    }

    private SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getCustomerId(), sale.getSaleDate(), sale.getTotalAmount(),
                sale.getCanceledAt() != null, sale.getPaymentType(), sale.getInstallmentCount());
    }
}
