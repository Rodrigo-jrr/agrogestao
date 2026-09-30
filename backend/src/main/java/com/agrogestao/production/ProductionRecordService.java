package com.agrogestao.production;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.activities.ActivityRepository;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.production.dto.ProductionRecordRequest;
import com.agrogestao.production.dto.ProductionRecordResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductionRecordService {

    private final ProductionRecordRepository productionRecordRepository;
    private final ActivityRepository activityRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public ProductionRecordService(
            ProductionRecordRepository productionRecordRepository,
            ActivityRepository activityRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.productionRecordRepository = productionRecordRepository;
        this.activityRepository = activityRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    /** Sem datas: todo o histórico. */
    public PageResponse<ProductionRecordResponse> list(LocalDate from, LocalDate to, String query, Pageable pageable) {
        if (from != null || to != null) {
            Period period = Period.resolve(from, to);
            from = period.from();
            to = period.to();
        }
        return PageResponse.of(productionRecordRepository.search(tenantContext.requirePropertyId(), from, to,
                PageResponse.likePattern(query), pageable), this::toResponse);
    }

    public ProductionRecordResponse create(ProductionRecordRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        ProductionRecord record = new ProductionRecord();
        record.setPropertyId(propertyId);
        applyRequest(record, request);
        productionRecordRepository.save(record);
        auditService.record("ProductionRecord", record.getId(), AuditAction.CREATE, describe(record));
        return toResponse(record);
    }

    public ProductionRecordResponse update(Long id, ProductionRecordRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        ProductionRecord record = findOwned(id, propertyId);
        applyRequest(record, request);
        productionRecordRepository.save(record);
        auditService.record("ProductionRecord", record.getId(), AuditAction.UPDATE, describe(record));
        return toResponse(record);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        ProductionRecord record = findOwned(id, tenantContext.requirePropertyId());
        productionRecordRepository.delete(record);
        auditService.record("ProductionRecord", record.getId(), AuditAction.DELETE, describe(record));
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

    private static String describe(ProductionRecord record) {
        return "%s %s %s".formatted(record.getProductName(), record.getQuantity(), record.getUnit());
    }

    private ProductionRecordResponse toResponse(ProductionRecord record) {
        return new ProductionRecordResponse(record.getId(), record.getProductName(), record.getQuantity(),
                record.getUnit(), record.getProductionDate(), record.getActivityId());
    }
}
