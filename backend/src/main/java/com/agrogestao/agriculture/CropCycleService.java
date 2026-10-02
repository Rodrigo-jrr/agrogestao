package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropCycleRequest;
import com.agrogestao.agriculture.dto.CropCycleResponse;
import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.fields.Field;
import com.agrogestao.fields.FieldRepository;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Safras/ciclos de cultivo: talhão + cultura da propriedade, com variedade e datas previstas. */
@Service
public class CropCycleService {

    private final CropCycleRepository cropCycleRepository;
    private final FieldRepository fieldRepository;
    private final CropRepository cropRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CropCycleService(CropCycleRepository cropCycleRepository, FieldRepository fieldRepository,
            CropRepository cropRepository, TenantContext tenantContext, AuditService auditService) {
        this.cropCycleRepository = cropCycleRepository;
        this.fieldRepository = fieldRepository;
        this.cropRepository = cropRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<CropCycleResponse> list(boolean includeArchived, Long fieldId) {
        Long propertyId = tenantContext.requirePropertyId();
        List<CropCycle> cycles = fieldId == null
                ? cropCycleRepository.findByPropertyIdOrderByPlannedPlantingDateDescIdDesc(propertyId)
                : cropCycleRepository.findByPropertyIdAndFieldIdOrderByPlannedPlantingDateDescIdDesc(propertyId, fieldId);
        return cycles.stream()
                .filter(cycle -> includeArchived || !cycle.isArchived())
                .map(cycle -> toResponse(cycle, requireField(propertyId, cycle.getFieldId()),
                        requireCrop(propertyId, cycle.getCropId())))
                .toList();
    }

    @Transactional
    public CropCycleResponse create(CropCycleRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Field field = requireActiveField(propertyId, request.fieldId());
        Crop crop = requireActiveCrop(propertyId, request.cropId());
        if (request.plannedHarvestDate() != null
                && request.plannedHarvestDate().isBefore(request.plannedPlantingDate())) {
            throw new BusinessException("A data prevista de colheita não pode ser anterior à do plantio.");
        }
        CropCycle cycle = new CropCycle();
        cycle.setPropertyId(propertyId);
        cycle.setFieldId(field.getId());
        cycle.setCropId(crop.getId());
        cycle.setVariety(blankToNull(request.variety()));
        cycle.setPlannedPlantingDate(request.plannedPlantingDate());
        cycle.setPlannedHarvestDate(request.plannedHarvestDate());
        cropCycleRepository.save(cycle);
        auditService.record("CropCycle", cycle.getId(), AuditAction.CREATE, describe(cycle, field, crop));
        return toResponse(cycle, field, crop);
    }

    @Transactional
    public CropCycleResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public CropCycleResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private CropCycleResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        CropCycle cycle = findOwned(propertyId, id);
        Field field = requireField(propertyId, cycle.getFieldId());
        Crop crop = requireCrop(propertyId, cycle.getCropId());
        cycle.setArchivedAt(archived ? LocalDateTime.now() : null);
        cropCycleRepository.save(cycle);
        auditService.record("CropCycle", cycle.getId(), AuditAction.UPDATE,
                (archived ? "arquivada: " : "reativada: ") + describe(cycle, field, crop));
        return toResponse(cycle, field, crop);
    }

    private CropCycle findOwned(Long propertyId, Long id) {
        return cropCycleRepository.findById(id)
                .filter(cycle -> cycle.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Safra não encontrada"));
    }

    /** Talhão da propriedade pelo id, mesmo arquivado — para exibir safras já cadastradas. */
    private Field requireField(Long propertyId, Long fieldId) {
        return fieldRepository.findById(fieldId)
                .filter(field -> field.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Talhão inválido para esta propriedade"));
    }

    /** Talhão ativo da propriedade — para registrar uma safra nova. */
    private Field requireActiveField(Long propertyId, Long fieldId) {
        Field field = requireField(propertyId, fieldId);
        if (field.isArchived()) {
            throw new BusinessException("O talhão " + field.getName() + " está arquivado.");
        }
        return field;
    }

    /** Cultura do catálogo da propriedade pelo id, mesmo arquivada — para exibir safras já cadastradas. */
    private Crop requireCrop(Long propertyId, Long cropId) {
        return cropRepository.findById(cropId)
                .filter(crop -> crop.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Cultura inválida para esta propriedade"));
    }

    /** Cultura ativa do catálogo da propriedade — para registrar uma safra nova. */
    private Crop requireActiveCrop(Long propertyId, Long cropId) {
        Crop crop = requireCrop(propertyId, cropId);
        if (crop.isArchived()) {
            throw new BusinessException("A cultura " + crop.getName() + " está arquivada.");
        }
        return crop;
    }

    private static CropCycleResponse toResponse(CropCycle cycle, Field field, Crop crop) {
        return new CropCycleResponse(cycle.getId(), field.getId(), field.getName(), crop.getId(), crop.getName(),
                cycle.getVariety(), cycle.getPlannedPlantingDate(), cycle.getPlannedHarvestDate(), cycle.isArchived());
    }

    private static String describe(CropCycle cycle, Field field, Crop crop) {
        return crop.getName() + " em " + field.getName()
                + (cycle.getVariety() != null ? " (" + cycle.getVariety() + ")" : "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
