package com.agrogestao.purchases;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByPropertyId(Long propertyId);

    boolean existsByIdAndPropertyId(Long id, Long propertyId);
}
