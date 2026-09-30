package com.agrogestao.auth.terms;

import com.agrogestao.common.exception.BusinessException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Termos de uso + política de privacidade, tratados como um documento com uma versão só.
 * Ao mudar o texto das páginas /termos e /privacidade, troque CURRENT_VERSION aqui e TERMS_VERSION em
 * frontend/src/lib/legal.ts: quem aceitou a versão anterior verá o aviso para aceitar a nova.
 */
@Service
public class TermsService {

    public static final String CURRENT_VERSION = "2026-09-30";

    private final TermsAcceptanceRepository termsAcceptanceRepository;

    public TermsService(TermsAcceptanceRepository termsAcceptanceRepository) {
        this.termsAcceptanceRepository = termsAcceptanceRepository;
    }

    public boolean hasAcceptedCurrent(Long userId) {
        return termsAcceptanceRepository.existsByUserIdAndTermsVersion(userId, CURRENT_VERSION);
    }

    /** O frontend manda a versão que a pessoa leu; se não for a atual, ela precisa ler de novo. */
    public void requireCurrent(String version) {
        if (!CURRENT_VERSION.equals(version)) {
            throw new BusinessException(
                    "Os termos de uso foram atualizados. Recarregue a página e leia a versão nova para continuar.");
        }
    }

    @Transactional
    public void recordAcceptance(Long userId, String version, String clientIp) {
        requireCurrent(version);
        if (hasAcceptedCurrent(userId)) {
            return;
        }
        TermsAcceptance acceptance = new TermsAcceptance();
        acceptance.setUserId(userId);
        acceptance.setTermsVersion(CURRENT_VERSION);
        acceptance.setAcceptedAt(LocalDateTime.now());
        acceptance.setIpAddress(clientIp);
        termsAcceptanceRepository.save(acceptance);
    }
}
