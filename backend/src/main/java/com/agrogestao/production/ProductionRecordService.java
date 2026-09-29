package com.agrogestao.production;

import com.agrogestao.activities.ActivityRepository;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.production.dto.ProductionRecordRequest;
import com.agrogestao.production.dto.ProductionRecordResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductionRecordService {

    private final ProductionRecordRepository productionRecordRepository;
    private final ActivityRepository activityRepository;
    private final TenantContext tenantContext;

    public ProductionRecordService(
            ProductionRecordRepository productionRecordRepository,
            ActivityRepository activityRepository,
            TenantContext tenantContext) {
        this.productionRecordRepository = productionRecordRepository;
        this.activityRepository = activityRepository;
        this.tenantContext = tenantContext;
    }

    public List<ProductionRecordResponse> list() {
        return productionRecordRepository
                .findByPropertyIdOrderByProductionDateDesc(tenantContext.requirePropertyId())
                .stream().map(this::toResponse).toList();
    }

    public ProductionRecordResponse create(ProductionRecordRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        ProductionRecord record = new ProductionRecord();
        record.setPropertyId(propertyId);
        applyRequest(record, request);
        return toResponse(productionRecordRepository.save(record));
    }

    public ProductionRecordResponse update(Long id, ProductionRecordRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        ProductionRecord record = findOwned(id, propertyId);
        applyRequest(record, request);
        return toResponse(productionRecordRepository.save(record));
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        productionRecordRepository.delete(findOwned(id, tenantContext.requirePropertyId()));
    }

    private void validateActivity(Long propertyId, Long activityId) {
        if (activityId != null && !activityRepository.existsByIdAndPropertyId(activityId, propertyId)) {
            throw new BusinessException("Atividade inválida para esta propriedade");
        }
    }

    private ProductionRecord findOwned(Long id, Long propertyId) {
        ProductionRecord record = productionRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de produção não encontrado"));
        if (!record.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Registro de produção não encontrado");
        }
        return record;
    }

    private void applyRequest(ProductionRecord record, ProductionRecordRequest request) {
        record.setProductName(request.getProductName());
        record.setQuantity(request.getQuantity());
        record.setUnit(request.getUnit());
        record.setProductionDate(request.getProductionDate());
        record.setActivityId(request.getActivityId());
    }

    private ProductionRecordResponse toResponse(ProductionRecord record) {
        return new ProductionRecordResponse(record.getId(), record.getProductName(), record.getQuantity(),
                record.getUnit(), record.getProductionDate(), record.getActivityId());
    }
}
