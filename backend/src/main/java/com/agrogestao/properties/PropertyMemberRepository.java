package com.agrogestao.properties;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PropertyMemberRepository extends JpaRepository<PropertyMember, Long> {

    List<PropertyMember> findByUserId(Long userId);

    List<PropertyMember> findByPropertyId(Long propertyId);

    long countByPropertyIdAndRole(Long propertyId, PropertyRole role);

    /** [propertyId, quantidade de membros] */
    @Query("SELECT m.propertyId, COUNT(m) FROM PropertyMember m GROUP BY m.propertyId")
    List<Object[]> countMembersByProperty();

    /** [userId, quantidade de propriedades] */
    @Query("SELECT m.userId, COUNT(m) FROM PropertyMember m GROUP BY m.userId")
    List<Object[]> countPropertiesByUser();

    Optional<PropertyMember> findByPropertyIdAndUserId(Long propertyId, Long userId);

    boolean existsByPropertyIdAndUserId(Long propertyId, Long userId);
}
