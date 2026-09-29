package com.agrogestao.finance;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.activities.ActivityRepository;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.dto.CostCenterRequest;
import com.agrogestao.finance.dto.CostCenterResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final ActivityRepository activityRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CostCenterService(
            CostCenterRepository costCenterRepository,
            ActivityRepository activityRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.costCenterRepository = costCenterRepository;
        this.activityRepository = activityRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<CostCenterResponse> list() {
        return costCenterRepository.findByPropertyId(tenantContext.requirePropertyId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CostCenterResponse create(CostCenterRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        CostCenter costCenter = new CostCenter();
        costCenter.setPropertyId(propertyId);
        costCenter.setName(request.getName());
        costCenter.setActivityId(request.getActivityId());
        costCenterRepository.save(costCenter);
        auditService.record("CostCenter", costCenter.getId(), AuditAction.CREATE, costCenter.getName());
        return toResponse(costCenter);
    }

    public CostCenterResponse update(Long id, CostCenterRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        validateActivity(propertyId, request.getActivityId());
        CostCenter costCenter = findOwned(id, propertyId);
        costCenter.setName(request.getName());
        costCenter.setActivityId(request.getActivityId());
        costCenterRepository.save(costCenter);
        auditService.record("CostCenter", costCenter.getId(), AuditAction.UPDATE, costCenter.getName());
        return toResponse(costCenter);
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        CostCenter costCenter = findOwned(id, tenantContext.requirePropertyId());
        costCenterRepository.delete(costCenter);
        auditService.record("CostCenter", costCenter.getId(), AuditAction.DELETE, costCenter.getName());
    }

    private void validateActivity(Long propertyId, Long activityId) {
        if (activityId != null && !activityRepository.existsByIdAndPropertyId(activityId, propertyId)) {
            throw new BusinessException("Atividade inválida para esta propriedade");
        }
    }

    private CostCenter findOwned(Long id, Long propertyId) {
        CostCenter costCenter = costCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de custo não encontrado"));
        if (!costCenter.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Centro de custo não encontrado");
        }
        return costCenter;
    }

    private CostCenterResponse toResponse(CostCenter costCenter) {
        return new CostCenterResponse(costCenter.getId(), costCenter.getName(), costCenter.getActivityId());
    }
}
