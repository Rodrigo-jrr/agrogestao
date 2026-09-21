package com.agrogestao.finance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, Long> {

    List<AccountReceivable> findByPropertyIdOrderByDueDateAsc(Long propertyId);
}
