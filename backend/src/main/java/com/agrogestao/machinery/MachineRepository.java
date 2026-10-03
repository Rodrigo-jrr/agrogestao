package com.agrogestao.machinery;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    List<Machine> findByPropertyIdOrderByNameAsc(Long propertyId);

    /** Nome único por propriedade entre as máquinas ativas — mesmo padrão de talhão/cultura/espécie. */
    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNull(Long propertyId, String name);

    boolean existsByPropertyIdAndNameIgnoreCaseAndArchivedAtIsNullAndIdNot(Long propertyId, String name, Long id);

    boolean existsByMachineTypeId(Long machineTypeId);
}
