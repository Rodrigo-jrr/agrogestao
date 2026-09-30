package com.agrogestao.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tokens de uso único enviados por e-mail. O valor em claro só existe no link; no banco fica o SHA-256,
 * então um vazamento da tabela não permite usar os links.
 */
@Service
public class UserTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final UserTokenRepository userTokenRepository;

    public UserTokenService(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    /** Cria um token novo e invalida os anteriores da mesma finalidade. Retorna o valor em claro. */
    @Transactional
    public String issue(Long userId, TokenPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        userTokenRepository.invalidateAll(userId, purpose, now);

        String rawToken = randomToken();
        UserToken token = new UserToken();
        token.setUserId(userId);
        token.setPurpose(purpose);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(purpose.validity()));
        userTokenRepository.save(token);
        return rawToken;
    }

    /** Valida e consome o token: só funciona uma vez e dentro da validade. Retorna o id do usuário. */
    @Transactional
    public Optional<Long> consume(String rawToken, TokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 100) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return userTokenRepository.findByTokenHashAndPurpose(hash(rawToken), purpose)
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(now))
                .map(token -> {
                    token.setUsedAt(now);
                    userTokenRepository.save(token);
                    return token.getUserId();
                });
    }

    public long countIssuedSince(Long userId, TokenPurpose purpose, LocalDateTime since) {
        return userTokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(userId, purpose, since);
    }

    /** Valor aleatório de 256 bits, seguro para ir num link. */
    public static String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
