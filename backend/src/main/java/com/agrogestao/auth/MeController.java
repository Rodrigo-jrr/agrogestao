package com.agrogestao.auth;

import com.agrogestao.admin.PlatformAdminService;
import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.ChangeEmailRequest;
import com.agrogestao.auth.dto.ChangePasswordRequest;
import com.agrogestao.auth.dto.UpdateProfileRequest;
import com.agrogestao.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** A conta do usuário logado. */
@RestController
public class MeController {

    public record MeResponse(Long userId, String name, String email, boolean emailVerified, boolean platformAdmin,
            String pendingEmail) {
    }

    private final UserRepository userRepository;
    private final PlatformAdminService platformAdminService;
    private final AccountService accountService;
    private final ProfileService profileService;

    public MeController(
            UserRepository userRepository,
            PlatformAdminService platformAdminService,
            AccountService accountService,
            ProfileService profileService) {
        this.userRepository = userRepository;
        this.platformAdminService = platformAdminService;
        this.accountService = accountService;
        this.profileService = profileService;
    }

    @GetMapping("/api/me")
    public MeResponse me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não encontrado"));
        return toResponse(user);
    }

    @PutMapping("/api/me")
    public MeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return toResponse(profileService.updateName(SecurityUtils.getCurrentUserId(), request.name()));
    }

    /** Devolve um token novo: as sessões abertas com a senha antiga são encerradas. */
    @PostMapping("/api/me/password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        return profileService.changePassword(SecurityUtils.getCurrentUserId(), request.currentPassword(),
                request.newPassword(), http.getRemoteAddr());
    }

    @PostMapping("/api/me/email")
    public ResponseEntity<Void> requestEmailChange(@Valid @RequestBody ChangeEmailRequest request, HttpServletRequest http) {
        profileService.requestEmailChange(SecurityUtils.getCurrentUserId(), request.newEmail(),
                request.currentPassword(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/api/me/email")
    public ResponseEntity<Void> cancelEmailChange() {
        profileService.cancelEmailChange(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/me/resend-verification")
    public ResponseEntity<Void> resendVerification() {
        accountService.resendVerification(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private MeResponse toResponse(User user) {
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.isEmailVerified(),
                platformAdminService.isAdmin(user.getId()), user.getPendingEmail());
    }
}
