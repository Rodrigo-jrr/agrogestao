package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.ForgotPasswordRequest;
import com.agrogestao.auth.dto.LoginRequest;
import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.auth.dto.ResetPasswordRequest;
import com.agrogestao.auth.dto.TokenRequest;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final ProfileService profileService;

    public AuthController(AuthService authService, AccountService accountService, ProfileService profileService) {
        this.authService = authService;
        this.accountService = accountService;
        this.profileService = profileService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.register(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        accountService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    /** Público: o link pode ser aberto em outro aparelho, sem estar logado. */
    @PostMapping("/confirm-email-change")
    public ResponseEntity<Void> confirmEmailChange(@Valid @RequestBody TokenRequest request) {
        profileService.confirmEmailChange(request.token());
        return ResponseEntity.noContent().build();
    }

    /** Resposta sempre igual, exista ou não a conta. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        accountService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(Map.of("message",
                "Se houver uma conta com este e-mail, enviamos um link para criar uma nova senha."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
