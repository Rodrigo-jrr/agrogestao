package com.agrogestao.properties.invitations;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyInvitationRepository extends JpaRepository<PropertyInvitation, Long> {

    List<PropertyInvitation> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

    Optional<PropertyInvitation> findByPropertyIdAndEmail(Long propertyId, String email);

    Optional<PropertyInvitation> findByTokenHash(String tokenHash);
}
