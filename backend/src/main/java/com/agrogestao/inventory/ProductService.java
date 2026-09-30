package com.agrogestao.inventory;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.inventory.dto.ProductRequest;
import com.agrogestao.inventory.dto.ProductResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository inventoryMovementRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<ProductResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        Map<Long, InventoryItem> items = itemsByProduct(propertyId);
        return productRepository.findByPropertyId(propertyId).stream()
                .filter(product -> includeArchived || !product.isArchived())
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .map(product -> toResponse(product, items.get(product.getId())))
                .toList();
    }

    public ProductResponse create(ProductRequest request) {
        tenantContext.requireWriteAccess();
        Product product = new Product();
        product.setPropertyId(tenantContext.requirePropertyId());
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setMinStock(request.getMinStock());
        productRepository.save(product);
        auditService.record("Product", product.getId(), AuditAction.CREATE, product.getName());
        return toResponse(product, null);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        tenantContext.requireWriteAccess();
        Product product = findOwned(id);
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setMinStock(request.getMinStock());
        productRepository.save(product);
        auditService.record("Product", product.getId(), AuditAction.UPDATE, product.getName());
        return toResponse(product, itemsByProduct(product.getPropertyId()).get(product.getId()));
    }

    /** Só exclui produto sem nenhuma movimentação; com histórico, o caminho é arquivar. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Product product = findOwned(id);
        if (inventoryMovementRepository.existsByProductId(product.getId())) {
            throw new BusinessException(
                    "Este produto já tem movimentações e não pode ser excluído. Arquive-o para escondê-lo das listas.");
        }
        inventoryItemRepository.findByPropertyIdAndProductId(product.getPropertyId(), product.getId())
                .ifPresent(inventoryItemRepository::delete);
        productRepository.delete(product);
        auditService.record("Product", product.getId(), AuditAction.DELETE, product.getName());
    }

    public ProductResponse archive(Long id) {
        return setArchived(id, true);
    }

    public ProductResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private ProductResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Product product = findOwned(id);
        product.setArchivedAt(archived ? LocalDateTime.now() : null);
        productRepository.save(product);
        auditService.record("Product", product.getId(), AuditAction.UPDATE,
                (archived ? "arquivado: " : "reativado: ") + product.getName());
        return toResponse(product, itemsByProduct(product.getPropertyId()).get(product.getId()));
    }

    private Product findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        if (!product.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Produto não encontrado");
        }
        return product;
    }

    public List<ProductResponse> listLowStock() {
        return list(false).stream()
                .filter(response -> response.getCurrentQuantity().compareTo(response.getMinStock()) < 0)
                .toList();
    }

    private Map<Long, InventoryItem> itemsByProduct(Long propertyId) {
        return inventoryItemRepository.findByPropertyId(propertyId).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
    }

    private ProductResponse toResponse(Product product, InventoryItem item) {
        BigDecimal quantity = item != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal averageCost = item != null ? item.getAverageCost() : BigDecimal.ZERO;
        return new ProductResponse(product.getId(), product.getName(), product.getCategory(),
                product.getUnit(), product.getMinStock(), quantity, averageCost,
                quantity.multiply(averageCost).setScale(2, RoundingMode.HALF_UP), product.isArchived());
    }
}
