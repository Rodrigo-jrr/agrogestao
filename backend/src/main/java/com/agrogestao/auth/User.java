package com.agrogestao.auth;

import com.agrogestao.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /** E-mail novo aguardando confirmação; o login continua pelo atual até lá. */
    @Column(name = "pending_email", length = 180)
    private String pendingEmail;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}
