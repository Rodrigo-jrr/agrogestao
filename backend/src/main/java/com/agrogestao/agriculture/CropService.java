package com.agrogestao.agriculture;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.agriculture.dto.CropRequest;
import com.agrogestao.agriculture.dto.CropResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Culturas que cada propriedade cadastra livremente (catálogo, sem enum fixo do sistema). */
@Service
public class CropService {

    private final CropRepository cropRepository;
    private final CropCycleRepository cropCycleRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public CropService(CropRepository cropRepository, CropCycleRepository cropCycleRepository,
            TenantContext tenantContext, AuditService auditService) {
        this.cropRepository = cropRepository;
        this.cropCycleRepository = cropCycleRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<CropResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        return cropRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(crop -> includeArchived || !crop.isArchived())
                .map(CropService::toResponse)
                .toList();
    }

    @Transactional
    public CropResponse create(CropRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (cropRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(propertyId, name)) {
            throw new BusinessException("Já existe uma cultura chamada " + name + ".");
        }
        Crop crop = new Crop();
        crop.setPropertyId(propertyId);
        crop.setName(name);
        cropRepository.save(crop);
        auditService.record("Crop", crop.getId(), AuditAction.CREATE, crop.getName());
        return toResponse(crop);
    }

    @Transactional
    public CropResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public CropResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private CropResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Crop crop = findOwned(id);
        crop.setArchivedAt(archived ? LocalDateTime.now() : null);
        cropRepository.save(crop);
        auditService.record("Crop", crop.getId(), AuditAction.UPDATE,
                (archived ? "arquivada: " : "reativada: ") + crop.getName());
        return toResponse(crop);
    }

    /** Só exclui cultura sem nenhuma safra cadastrada. Já em uso, o caminho é arquivar. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Crop crop = findOwned(id);
        if (cropCycleRepository.existsByCropId(id)) {
            throw new BusinessException("A cultura " + crop.getName()
                    + " já tem safras cadastradas e não pode ser excluída. Arquive-a para escondê-la das listas.");
        }
        cropRepository.delete(crop);
        auditService.record("Crop", crop.getId(), AuditAction.DELETE, crop.getName());
    }

    private Crop findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        return cropRepository.findById(id)
                .filter(crop -> crop.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Cultura não encontrada"));
    }

    private static CropResponse toResponse(Crop crop) {
        return new CropResponse(crop.getId(), crop.getName(), crop.isArchived());
    }
}
