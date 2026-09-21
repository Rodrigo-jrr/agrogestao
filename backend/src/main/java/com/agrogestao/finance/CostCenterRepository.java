package com.agrogestao.finance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {

    List<CostCenter> findByPropertyId(Long propertyId);
}
