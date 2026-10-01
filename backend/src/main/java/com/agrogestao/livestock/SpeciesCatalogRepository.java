package com.agrogestao.livestock;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesCatalogRepository extends JpaRepository<SpeciesCatalog, Long> {

    List<SpeciesCatalog> findByPropertyIdOrderByNameAsc(Long propertyId);

    Optional<SpeciesCatalog> findByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    /** Nome único por propriedade entre as espécies ativas — mesmo padrão do brinco único entre animais ativos. */
    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(Long propertyId, String name, Long id);
}
