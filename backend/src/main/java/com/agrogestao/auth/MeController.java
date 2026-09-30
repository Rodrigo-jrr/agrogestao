package com.agrogestao.auth;

import com.agrogestao.admin.PlatformAdminService;
import com.agrogestao.common.exception.ForbiddenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeResponse(Long userId, String name, String email, boolean platformAdmin) {
    }

    private final UserRepository userRepository;
    private final PlatformAdminService platformAdminService;

    public MeController(UserRepository userRepository, PlatformAdminService platformAdminService) {
        this.userRepository = userRepository;
        this.platformAdminService = platformAdminService;
    }

    @GetMapping("/api/me")
    public MeResponse me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado"));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), platformAdminService.isAdmin(userId));
    }
}
