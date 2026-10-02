package com.agrogestao.agriculture;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropCycleRepository extends JpaRepository<CropCycle, Long> {

    List<CropCycle> findByPropertyIdOrderByPlannedPlantingDateDescIdDesc(Long propertyId);

    List<CropCycle> findByPropertyIdAndFieldIdOrderByPlannedPlantingDateDescIdDesc(Long propertyId, Long fieldId);

    boolean existsByCropId(Long cropId);
}
