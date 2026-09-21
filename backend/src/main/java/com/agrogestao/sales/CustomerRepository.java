package com.agrogestao.sales;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByPropertyId(Long propertyId);
}
