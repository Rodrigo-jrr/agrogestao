package com.agrogestao.machinery;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.machinery.dto.MachineReadingRequest;
import com.agrogestao.machinery.dto.MachineRequest;
import com.agrogestao.machinery.dto.MachineResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro de máquinas e implementos de uma propriedade. */
@Service
public class MachineService {

    private final MachineRepository machineRepository;
    private final MachineTypeRepository machineTypeRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public MachineService(MachineRepository machineRepository, MachineTypeRepository machineTypeRepository,
            TenantContext tenantContext, AuditService auditService) {
        this.machineRepository = machineRepository;
        this.machineTypeRepository = machineTypeRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<MachineResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        List<Machine> machines = machineRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(machine -> includeArchived || !machine.isArchived())
                .toList();
        Map<Long, MachineType> types = machineTypeRepository.findAllById(
                machines.stream().map(Machine::getMachineTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(MachineType::getId, Function.identity()));
        return machines.stream().map(machine -> toResponse(machine, types.get(machine.getMachineTypeId()))).toList();
    }

    @Transactional
    public MachineResponse create(MachineRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (machineRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(propertyId, name)) {
            throw new BusinessException("Já existe uma máquina chamada " + name + ".");
        }
        MachineType type = requireActiveType(propertyId, request.machineTypeId());
        Machine machine = new Machine();
        machine.setPropertyId(propertyId);
        machine.setName(name);
        machine.setMachineTypeId(type.getId());
        machine.setIdentification(blankToNull(request.identification()));
        machine.setMeasurementType(request.measurementType());
        machine.setCurrentReading(request.currentReading() != null ? request.currentReading() : java.math.BigDecimal.ZERO);
        machineRepository.save(machine);
        auditService.record("Machine", machine.getId(), AuditAction.CREATE, describe(machine, type));
        return toResponse(machine, type);
    }

    @Transactional
    public MachineResponse update(Long id, MachineRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Machine machine = findOwned(propertyId, id);
        String name = request.name().trim();
        if (machineRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(propertyId, name, id)) {
            throw new BusinessException("Já existe uma máquina chamada " + name + ".");
        }
        MachineType type = requireActiveType(propertyId, request.machineTypeId());
        machine.setName(name);
        machine.setMachineTypeId(type.getId());
        machine.setIdentification(blankToNull(request.identification()));
        machine.setMeasurementType(request.measurementType());
        machineRepository.save(machine);
        auditService.record("Machine", machine.getId(), AuditAction.UPDATE, describe(machine, type));
        return toResponse(machine, type);
    }

    /** Atualiza só a leitura atual (horas/km) — separado da edição do cadastro para deixar o evento claro na auditoria. */
    @Transactional
    public MachineResponse updateReading(Long id, MachineReadingRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Machine machine = findOwned(propertyId, id);
        if (request.reading().compareTo(machine.getCurrentReading()) < 0) {
            throw new BusinessException("A leitura não pode ser menor que a atual ("
                    + machine.getCurrentReading().stripTrailingZeros().toPlainString() + ").");
        }
        auditService.record("Machine", machine.getId(), AuditAction.UPDATE, "leitura de " + machine.getName() + ": "
                + machine.getCurrentReading().stripTrailingZeros().toPlainString() + " → "
                + request.reading().stripTrailingZeros().toPlainString());
        machine.setCurrentReading(request.reading());
        machineRepository.save(machine);
        return toResponse(machine, typeOf(machine));
    }

    @Transactional
    public MachineResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public MachineResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private MachineResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Machine machine = findOwned(propertyId, id);
        machine.setArchivedAt(archived ? LocalDateTime.now() : null);
        machineRepository.save(machine);
        auditService.record("Machine", machine.getId(), AuditAction.UPDATE,
                (archived ? "arquivada: " : "reativada: ") + machine.getName());
        return toResponse(machine, typeOf(machine));
    }

    /** Exclusão física: nada ainda referencia máquina (passa a arquivar só quando operações agrícolas usarem). */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Machine machine = findOwned(propertyId, id);
        machineRepository.delete(machine);
        auditService.record("Machine", id, AuditAction.DELETE, machine.getName());
    }

    private Machine findOwned(Long propertyId, Long id) {
        return machineRepository.findById(id)
                .filter(machine -> machine.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Máquina não encontrada"));
    }

    /** Tipo ativo do catálogo da propriedade — para cadastrar ou editar uma máquina. */
    private MachineType requireActiveType(Long propertyId, Long machineTypeId) {
        MachineType type = machineTypeRepository.findById(machineTypeId)
                .filter(t -> t.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Tipo de máquina inválido para esta propriedade"));
        if (type.isArchived()) {
            throw new BusinessException("O tipo " + type.getName() + " está arquivado.");
        }
        return type;
    }

    private MachineType typeOf(Machine machine) {
        return machineTypeRepository.findById(machine.getMachineTypeId()).orElseThrow();
    }

    private static String describe(Machine machine, MachineType type) {
        return machine.getName() + " (" + type.getName() + ")";
    }

    private static MachineResponse toResponse(Machine machine, MachineType type) {
        return new MachineResponse(machine.getId(), machine.getName(), machine.getMachineTypeId(),
                type != null ? type.getName() : null, machine.getIdentification(), machine.getMeasurementType(),
                machine.getCurrentReading(), machine.isArchived());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
