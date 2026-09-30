package com.agrogestao.auth.terms;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, Long> {

    boolean existsByUserIdAndTermsVersion(Long userId, String termsVersion);

    List<TermsAcceptance> findByUserIdOrderByAcceptedAtAsc(Long userId);
}
