package com.agrogestao.activities;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByPropertyId(Long propertyId);
}
