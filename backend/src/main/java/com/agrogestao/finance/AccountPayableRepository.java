package com.agrogestao.finance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPayableRepository extends JpaRepository<AccountPayable, Long> {

    List<AccountPayable> findByPropertyIdOrderByDueDateAsc(Long propertyId);
}
