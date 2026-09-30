package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.token.TokenPurpose;
import com.agrogestao.auth.token.UserTokenService;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.TooManyRequestsException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados da própria conta. Trocar senha ou e-mail exige a senha atual (um token roubado sozinho não basta),
 * e as tentativas erradas contam no mesmo limite do login.
 */
@Service
public class ProfileService {

    private static final int MAX_EMAIL_CHANGES_PER_HOUR = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserTokenService userTokenService;
    private final EmailService emailService;
    private final LoginProtectionService loginProtectionService;

    public ProfileService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserTokenService userTokenService,
            EmailService emailService,
            LoginProtectionService loginProtectionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.loginProtectionService = loginProtectionService;
    }

    @Transactional
    public User updateName(Long userId, String name) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setName(name.trim());
        return userRepository.save(user);
    }

    /**
     * Troca a senha e encerra as outras sessões. Devolve um token novo para quem fez a troca continuar logado.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, String currentPassword, String newPassword, String clientIp) {
        User user = userRepository.findById(userId).orElseThrow();
        requireCurrentPassword(user, currentPassword, clientIp);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("A nova senha precisa ser diferente da atual.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Tokens emitidos antes deste instante deixam de valer (JwtAuthenticationFilter).
        user.setPasswordChangedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);
        emailService.sendPasswordChangedNotice(user);
        return new AuthResponse(jwtService.generateToken(user.getId()), user.getId(), user.getName(), user.getEmail());
    }

    /** O e-mail só muda quando o endereço novo for confirmado pelo link. */
    @Transactional
    public void requestEmailChange(Long userId, String newEmail, String currentPassword, String clientIp) {
        User user = userRepository.findById(userId).orElseThrow();
        requireCurrentPassword(user, currentPassword, clientIp);
        String normalized = newEmail.trim();
        if (normalized.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException("Este já é o e-mail da sua conta.");
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new BusinessException("Já existe uma conta com este e-mail.");
        }
        if (userTokenService.countIssuedSince(userId, TokenPurpose.CHANGE_EMAIL, LocalDateTime.now().minusHours(1))
                >= MAX_EMAIL_CHANGES_PER_HOUR) {
            throw new TooManyRequestsException("Muitos pedidos de troca de e-mail na última hora. Tente mais tarde.");
        }
        user.setPendingEmail(normalized);
        userRepository.save(user);
        emailService.sendEmailChangeConfirmation(user, normalized, userTokenService.issue(userId, TokenPurpose.CHANGE_EMAIL));
        emailService.sendEmailChangeNotice(user, normalized);
    }

    @Transactional
    public void cancelEmailChange(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setPendingEmail(null);
        userRepository.save(user);
        // Invalida o link já enviado emitindo um novo e descartando-o.
        userTokenService.issue(userId, TokenPurpose.CHANGE_EMAIL);
    }

    @Transactional
    public void confirmEmailChange(String token) {
        Long userId = userTokenService.consume(token, TokenPurpose.CHANGE_EMAIL)
                .orElseThrow(() -> new BusinessException("Link inválido ou expirado. Peça a troca de e-mail de novo."));
        User user = userRepository.findById(userId).orElseThrow();
        String newEmail = user.getPendingEmail();
        if (newEmail == null) {
            throw new BusinessException("Link inválido ou expirado. Peça a troca de e-mail de novo.");
        }
        boolean taken = userRepository.findByEmail(newEmail).filter(other -> !other.getId().equals(userId)).isPresent();
        if (taken) {
            throw new BusinessException("Este e-mail passou a ser usado por outra conta. Escolha outro endereço.");
        }
        user.setPendingEmail(null);
        user.setEmail(newEmail);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /** Confirmação de senha para ações sensíveis de outros módulos (ex.: excluir propriedade). */
    public void confirmPassword(Long userId, String password, String clientIp) {
        requireCurrentPassword(userRepository.findById(userId).orElseThrow(), password, clientIp);
    }

    private void requireCurrentPassword(User user, String password, String clientIp) {
        String key = user.getEmail().toLowerCase(Locale.ROOT);
        loginProtectionService.checkLoginAllowed(key, clientIp);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            loginProtectionService.loginFailed(key, clientIp);
            // 422, não 401: senha errada aqui não pode derrubar a sessão no frontend.
            throw new BusinessException("Senha incorreta.");
        }
        loginProtectionService.loginSucceeded(key);
    }
}
