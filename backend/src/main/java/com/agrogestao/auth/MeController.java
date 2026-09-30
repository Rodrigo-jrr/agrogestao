package com.agrogestao.auth;

import com.agrogestao.admin.PlatformAdminService;
import com.agrogestao.common.exception.ForbiddenException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeResponse(Long userId, String name, String email, boolean emailVerified, boolean platformAdmin) {
    }

    private final UserRepository userRepository;
    private final PlatformAdminService platformAdminService;
    private final AccountService accountService;

    public MeController(
            UserRepository userRepository,
            PlatformAdminService platformAdminService,
            AccountService accountService) {
        this.userRepository = userRepository;
        this.platformAdminService = platformAdminService;
        this.accountService = accountService;
    }

    @GetMapping("/api/me")
    public MeResponse me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado"));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified(),
                platformAdminService.isAdmin(userId));
    }

    @PostMapping("/api/me/resend-verification")
    public ResponseEntity<Void> resendVerification() {
        accountService.resendVerification(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
