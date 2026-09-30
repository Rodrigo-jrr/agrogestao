package com.agrogestao.admin;

import com.agrogestao.auth.User;
import com.agrogestao.auth.UserRepository;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Decide quem é administrador da plataforma. A decisão é sempre tomada no servidor, a cada requisição,
 * a partir do e-mail cadastrado do usuário autenticado; nada disso vai no token, então remover um e-mail
 * da configuração revoga o acesso imediatamente (após reiniciar a aplicação).
 */
@Service("platformAdmin")
public class PlatformAdminService {

    private final Set<String> adminEmails;
    private final UserRepository userRepository;

    public PlatformAdminService(@Value("${agrogestao.admin.emails:}") String adminEmails, UserRepository userRepository) {
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Long userId) {
        if (userId == null || adminEmails.isEmpty()) {
            return false;
        }
        return userRepository.findById(userId)
                .map(User::getEmail)
                .map(email -> adminEmails.contains(email.trim().toLowerCase(Locale.ROOT)))
                .orElse(false);
    }

    /** Usado em {@code @PreAuthorize("@platformAdmin.check(authentication)")}. */
    public boolean check(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof Long userId && isAdmin(userId);
    }
}
