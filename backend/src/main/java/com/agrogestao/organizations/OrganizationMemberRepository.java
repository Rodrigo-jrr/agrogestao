package com.agrogestao.organizations;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByUserId(Long userId);

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
