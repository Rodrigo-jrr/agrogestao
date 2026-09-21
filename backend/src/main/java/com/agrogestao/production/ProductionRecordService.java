package com.agrogestao.production;

import com.agrogestao.production.dto.ProductionRecordRequest;
import com.agrogestao.production.dto.ProductionRecordResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductionRecordService {

    private final ProductionRecordRepository productionRecordRepository;
    private final TenantContext tenantContext;

    public ProductionRecordService(
            ProductionRecordRepository productionRecordRepository, TenantContext tenantContext) {
        this.productionRecordRepository = productionRecordRepository;
        this.tenantContext = tenantContext;
    }

    public List<ProductionRecordResponse> list() {
        return productionRecordRepository
                .findByPropertyIdOrderByProductionDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public ProductionRecordResponse create(ProductionRecordRequest request) {
        ProductionRecord record = new ProductionRecord();
        record.setPropertyId(tenantContext.requirePropertyId());
        record.setProductName(request.getProductName());
        record.setQuantity(request.getQuantity());
        record.setUnit(request.getUnit());
        record.setProductionDate(request.getProductionDate());
        record.setActivityId(request.getActivityId());
        return toResponse(productionRecordRepository.save(record));
    }

    private ProductionRecordResponse toResponse(ProductionRecord record) {
        return new ProductionRecordResponse(record.getId(), record.getProductName(), record.getQuantity(),
                record.getUnit(), record.getProductionDate(), record.getActivityId());
    }
}
