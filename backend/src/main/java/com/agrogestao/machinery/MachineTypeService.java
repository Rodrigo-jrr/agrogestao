package com.agrogestao.machinery;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.machinery.dto.MachineTypeRequest;
import com.agrogestao.machinery.dto.MachineTypeResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tipos de máquina que cada propriedade cadastra livremente (catálogo, sem enum fixo do sistema). */
@Service
public class MachineTypeService {

    private final MachineTypeRepository machineTypeRepository;
    private final MachineRepository machineRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public MachineTypeService(MachineTypeRepository machineTypeRepository, MachineRepository machineRepository,
            TenantContext tenantContext, AuditService auditService) {
        this.machineTypeRepository = machineTypeRepository;
        this.machineRepository = machineRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<MachineTypeResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        return machineTypeRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(type -> includeArchived || !type.isArchived())
                .map(MachineTypeService::toResponse)
                .toList();
    }

    @Transactional
    public MachineTypeResponse create(MachineTypeRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (machineTypeRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(propertyId, name)) {
            throw new BusinessException("Já existe um tipo de máquina chamado " + name + ".");
        }
        MachineType type = new MachineType();
        type.setPropertyId(propertyId);
        type.setName(name);
        machineTypeRepository.save(type);
        auditService.record("MachineType", type.getId(), AuditAction.CREATE, type.getName());
        return toResponse(type);
    }

    @Transactional
    public MachineTypeResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public MachineTypeResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private MachineTypeResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        MachineType type = findOwned(id);
        type.setArchivedAt(archived ? LocalDateTime.now() : null);
        machineTypeRepository.save(type);
        auditService.record("MachineType", type.getId(), AuditAction.UPDATE,
                (archived ? "arquivado: " : "reativado: ") + type.getName());
        return toResponse(type);
    }

    /** Só exclui tipo sem nenhuma máquina cadastrada. Já em uso, o caminho é arquivar. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        MachineType type = findOwned(id);
        if (machineRepository.existsByMachineTypeId(id)) {
            throw new BusinessException("O tipo " + type.getName()
                    + " já tem máquinas cadastradas e não pode ser excluído. Arquive-o para escondê-lo das listas.");
        }
        machineTypeRepository.delete(type);
        auditService.record("MachineType", type.getId(), AuditAction.DELETE, type.getName());
    }

    private MachineType findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        return machineTypeRepository.findById(id)
                .filter(type -> type.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de máquina não encontrado"));
    }

    private static MachineTypeResponse toResponse(MachineType type) {
        return new MachineTypeResponse(type.getId(), type.getName(), type.isArchived());
    }
}
