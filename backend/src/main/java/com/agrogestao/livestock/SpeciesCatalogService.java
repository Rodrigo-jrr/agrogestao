package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.livestock.dto.SpeciesRequest;
import com.agrogestao.livestock.dto.SpeciesResponse;
import com.agrogestao.properties.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Espécies que cada propriedade cadastra livremente (nome + se admite brinco/cadastro individual). */
@Service
public class SpeciesCatalogService {

    private final SpeciesCatalogRepository speciesRepository;
    private final AnimalRepository animalRepository;
    private final AnimalBatchRepository batchRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public SpeciesCatalogService(
            SpeciesCatalogRepository speciesRepository,
            AnimalRepository animalRepository,
            AnimalBatchRepository batchRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.speciesRepository = speciesRepository;
        this.animalRepository = animalRepository;
        this.batchRepository = batchRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public List<SpeciesResponse> list(boolean includeArchived) {
        Long propertyId = tenantContext.requirePropertyId();
        return speciesRepository.findByPropertyIdOrderByNameAsc(propertyId).stream()
                .filter(species -> includeArchived || !species.isArchived())
                .map(SpeciesCatalogService::toResponse)
                .toList();
    }

    @Transactional
    public SpeciesResponse create(SpeciesRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        String name = request.name().trim();
        if (speciesRepository.existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(propertyId, name)) {
            throw new BusinessException("Já existe uma espécie chamada " + name + ".");
        }
        SpeciesCatalog species = new SpeciesCatalog();
        species.setPropertyId(propertyId);
        species.setName(name);
        species.setAllowsIndividual(Boolean.TRUE.equals(request.allowsIndividual()));
        speciesRepository.save(species);
        auditService.record("Species", species.getId(), AuditAction.CREATE, species.getName());
        return toResponse(species);
    }

    @Transactional
    public SpeciesResponse archive(Long id) {
        return setArchived(id, true);
    }

    @Transactional
    public SpeciesResponse unarchive(Long id) {
        return setArchived(id, false);
    }

    private SpeciesResponse setArchived(Long id, boolean archived) {
        tenantContext.requireManageAccess();
        SpeciesCatalog species = findOwned(id);
        species.setArchivedAt(archived ? LocalDateTime.now() : null);
        speciesRepository.save(species);
        auditService.record("Species", species.getId(), AuditAction.UPDATE,
                (archived ? "arquivada: " : "reativada: ") + species.getName());
        return toResponse(species);
    }

    /** Só exclui espécie sem nenhum animal ou lote. Já em uso, o caminho é arquivar. */
    @Transactional
    public void delete(Long id) {
        tenantContext.requireManageAccess();
        SpeciesCatalog species = findOwned(id);
        if (animalRepository.existsBySpeciesId(id) || batchRepository.existsBySpeciesId(id)) {
            throw new BusinessException("A espécie " + species.getName()
                    + " já tem animais ou lotes cadastrados e não pode ser excluída. Arquive-a para escondê-la das listas.");
        }
        speciesRepository.delete(species);
        auditService.record("Species", species.getId(), AuditAction.DELETE, species.getName());
    }

    private SpeciesCatalog findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        return speciesRepository.findById(id)
                .filter(species -> species.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Espécie não encontrada"));
    }

    private static SpeciesResponse toResponse(SpeciesCatalog species) {
        return new SpeciesResponse(species.getId(), species.getName(), species.isAllowsIndividual(), species.isArchived());
    }
}
