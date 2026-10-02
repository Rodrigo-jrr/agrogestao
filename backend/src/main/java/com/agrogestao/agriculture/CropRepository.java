package com.agrogestao.agriculture;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropRepository extends JpaRepository<Crop, Long> {

    List<Crop> findByPropertyIdOrderByNameAsc(Long propertyId);

    boolean existsByIdAndPropertyId(Long id, Long propertyId);

    /** Nome único por propriedade entre as culturas ativas — mesmo padrão do brinco único entre animais ativos. */
    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(Long propertyId, String name, Long id);
}
