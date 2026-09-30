package com.agrogestao.auth;

import com.agrogestao.auth.token.TokenPurpose;
import com.agrogestao.auth.token.UserTokenService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.TooManyRequestsException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verificação de e-mail e recuperação de senha por links de uso único. */
@Service
public class AccountService {

    private static final int MAX_EMAILS_PER_HOUR = 3;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final UserTokenService userTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserRepository userRepository,
            UserTokenService userTokenService,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendVerification(User user) {
        emailService.sendEmailVerification(user, userTokenService.issue(user.getId(), TokenPurpose.VERIFY_EMAIL));
    }

    /** Reenvio pedido pelo próprio usuário logado. */
    public void resendVerification(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.isEmailVerified()) {
            throw new BusinessException("Seu e-mail já está confirmado.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (userTokenService.countIssuedSince(userId, TokenPurpose.VERIFY_EMAIL, now.minusSeconds(RESEND_COOLDOWN_SECONDS)) > 0) {
            throw new TooManyRequestsException("Acabamos de enviar um e-mail. Aguarde um minuto para pedir outro.");
        }
        if (userTokenService.countIssuedSince(userId, TokenPurpose.VERIFY_EMAIL, now.minusHours(1)) >= MAX_EMAILS_PER_HOUR) {
            throw new TooManyRequestsException("Muitos envios na última hora. Tente novamente mais tarde.");
        }
        sendVerification(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        Long userId = userTokenService.consume(token, TokenPurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new BusinessException(
                        "Link inválido ou expirado. Entre na sua conta e peça um novo e-mail de confirmação."));
        User user = userRepository.findById(userId).orElseThrow();
        if (!user.isEmailVerified()) {
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Sempre termina sem erro, exista ou não a conta, para não revelar quais e-mails estão cadastrados.
     * Pedidos acima do limite por hora são ignorados em silêncio pelo mesmo motivo.
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            long recent = userTokenService.countIssuedSince(
                    user.getId(), TokenPurpose.RESET_PASSWORD, LocalDateTime.now().minusHours(1));
            if (recent < MAX_EMAILS_PER_HOUR) {
                emailService.sendPasswordReset(user, userTokenService.issue(user.getId(), TokenPurpose.RESET_PASSWORD));
            }
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Long userId = userTokenService.consume(token, TokenPurpose.RESET_PASSWORD)
                .orElseThrow(() -> new BusinessException("Link inválido ou expirado. Peça uma nova redefinição de senha."));
        User user = userRepository.findById(userId).orElseThrow();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Encerra sessões abertas: tokens emitidos antes deste instante deixam de valer.
        user.setPasswordChangedAt(now);
        // Quem recebeu o link no e-mail comprovou que o e-mail é seu.
        if (!user.isEmailVerified()) {
            user.setEmailVerifiedAt(now);
        }
        userRepository.save(user);
    }
}
