package com.agrogestao.fields;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.fields.dto.FieldRequest;
import com.agrogestao.fields.dto.FieldResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Talhões da propriedade: cadastro básico. Cultura/safra e indicadores vêm nas próximas entregas da Agricultura. */
@Service
public class FieldService {

    private final FieldRepository fieldRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public FieldService(FieldRepository fieldRepository, TenantContext tenantContext, AuditService auditService) {
        this.fieldRepository = fieldRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<FieldResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        return fieldRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(field -> includeArchived || !field.isArchived())
                .map(FieldService::toResponse)
                .toList();
    }

    @Transactional
    public FieldResponse create(FieldRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (fieldRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(propertyId, name)) {
            throw new BusinessException("Já existe um talhão chamado " + name + ".");
        }
        Field field = new Field();
        field.setPropertyId(propertyId);
        field.setName(name);
        field.setAreaHa(request.areaHa());
        field.setLocation(blankToNull(request.location()));
        field.setSoilType(blankToNull(request.soilType()));
        fieldRepository.save(field);
        auditService.record("Field", field.getId(), AuditAction.CREATE, describe(field));
        return toResponse(field);
    }

    @Transactional
    public FieldResponse update(Long id, FieldRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Field field = findOwned(propertyId, id);
        String name = request.name().trim();
        if (fieldRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(propertyId, name, id)) {
            throw new BusinessException("Já existe um talhão chamado " + name + ".");
        }
        field.setName(name);
        field.setAreaHa(request.areaHa());
        field.setLocation(blankToNull(request.location()));
        field.setSoilType(blankToNull(request.soilType()));
        fieldRepository.save(field);
        auditService.record("Field", field.getId(), AuditAction.UPDATE, describe(field));
        return toResponse(field);
    }

    @Transactional
    public FieldResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public FieldResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private FieldResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Field field = findOwned(propertyId, id);
        field.setArchivedAt(archived ? LocalDateTime.now() : null);
        fieldRepository.save(field);
        auditService.record("Field", field.getId(), AuditAction.UPDATE,
                (archived ? "arquivado: " : "reativado: ") + field.getName());
        return toResponse(field);
    }

    private Field findOwned(Long propertyId, Long id) {
        return fieldRepository.findById(id)
                .filter(field -> field.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Talhão não encontrado"));
    }

    private static FieldResponse toResponse(Field field) {
        return new FieldResponse(field.getId(), field.getName(), field.getAreaHa(), field.getLocation(),
                field.getSoilType(), field.isArchived());
    }

    private static String describe(Field field) {
        return "%s (%s ha)".formatted(field.getName(), field.getAreaHa().stripTrailingZeros().toPlainString());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
