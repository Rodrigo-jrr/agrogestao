package com.agrogestao.inventory;

import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.inventory.dto.ProductRequest;
import com.agrogestao.inventory.dto.ProductResponse;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final TenantContext tenantContext;

    public ProductService(
            ProductRepository productRepository,
            InventoryItemRepository inventoryItemRepository,
            TenantContext tenantContext) {
        this.productRepository = productRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.tenantContext = tenantContext;
    }

    public List<ProductResponse> list() {
        Long propertyId = tenantContext.requirePropertyId();
        return productRepository.findByPropertyId(propertyId).stream()
                .map(product -> toResponse(product, propertyId))
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
        return toResponse(product, product.getPropertyId());
    }

    public ProductResponse update(Long id, ProductRequest request) {
        tenantContext.requireWriteAccess();
        Product product = findOwned(id);
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setUnit(request.getUnit());
        product.setMinStock(request.getMinStock());
        productRepository.save(product);
        return toResponse(product, product.getPropertyId());
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        productRepository.delete(findOwned(id));
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
        Long propertyId = tenantContext.requirePropertyId();
        return productRepository.findByPropertyId(propertyId).stream()
                .map(product -> toResponse(product, propertyId))
                .filter(response -> response.getCurrentQuantity().compareTo(response.getMinStock()) < 0)
                .toList();
    }

    private ProductResponse toResponse(Product product, Long propertyId) {
        BigDecimal quantity = inventoryItemRepository.findByPropertyIdAndProductId(propertyId, product.getId())
                .map(InventoryItem::getQuantity)
                .orElse(BigDecimal.ZERO);
        return new ProductResponse(product.getId(), product.getName(), product.getCategory(),
                product.getUnit(), product.getMinStock(), quantity);
    }
}
