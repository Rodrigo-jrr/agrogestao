package com.agrogestao.fields;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldRepository extends JpaRepository<Field, Long> {

    List<Field> findByPropertyIdOrderByNameAsc(Long propertyId);

    boolean existsByIdAndPropertyId(Long id, Long propertyId);

    /** Nome único por propriedade entre os talhões ativos — mesmo padrão de lote/produto/espécie. */
    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(Long propertyId, String name, Long id);
}
