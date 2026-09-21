package com.agrogestao.finance;

import com.agrogestao.finance.dto.CostCenterRequest;
import com.agrogestao.finance.dto.CostCenterResponse;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CostCenterService {

    private final CostCenterRepository costCenterRepository;
    private final TenantContext tenantContext;

    public CostCenterService(CostCenterRepository costCenterRepository, TenantContext tenantContext) {
        this.costCenterRepository = costCenterRepository;
        this.tenantContext = tenantContext;
    }

    public List<CostCenterResponse> list() {
        return costCenterRepository.findByPropertyId(tenantContext.requirePropertyId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CostCenterResponse create(CostCenterRequest request) {
        CostCenter costCenter = new CostCenter();
        costCenter.setPropertyId(tenantContext.requirePropertyId());
        costCenter.setName(request.getName());
        costCenter.setActivityId(request.getActivityId());
        return toResponse(costCenterRepository.save(costCenter));
    }

    private CostCenterResponse toResponse(CostCenter costCenter) {
        return new CostCenterResponse(costCenter.getId(), costCenter.getName(), costCenter.getActivityId());
    }
}
