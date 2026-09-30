package com.agrogestao.auth.token;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, TokenPurpose purpose, LocalDateTime after);

    /** Invalida os links ainda válidos de um usuário para uma finalidade. */
    @Modifying
    @Query("UPDATE UserToken t SET t.usedAt = :now WHERE t.userId = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidateAll(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose, @Param("now") LocalDateTime now);
}
