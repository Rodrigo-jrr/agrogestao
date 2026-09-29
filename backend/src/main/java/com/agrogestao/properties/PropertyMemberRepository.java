package com.agrogestao.properties;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyMemberRepository extends JpaRepository<PropertyMember, Long> {

    List<PropertyMember> findByUserId(Long userId);

    List<PropertyMember> findByPropertyId(Long propertyId);

    long countByPropertyIdAndRole(Long propertyId, PropertyRole role);

    Optional<PropertyMember> findByPropertyIdAndUserId(Long propertyId, Long userId);

    boolean existsByPropertyIdAndUserId(Long propertyId, Long userId);
}
