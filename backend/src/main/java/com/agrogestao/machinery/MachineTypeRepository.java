package com.agrogestao.machinery;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineTypeRepository extends JpaRepository<MachineType, Long> {

    List<MachineType> findByPropertyIdOrderByNameAsc(Long propertyId);

    /** Nome único por propriedade entre os tipos ativos — mesmo padrão de cultura/espécie. */
    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(Long propertyId, String name, Long id);
}
