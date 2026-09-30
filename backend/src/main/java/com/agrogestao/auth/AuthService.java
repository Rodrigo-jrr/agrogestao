package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.LoginRequest;
import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.auth.terms.TermsService;
import com.agrogestao.common.exception.BusinessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final LoginProtectionService loginProtectionService;
    private final TermsService termsService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountService accountService,
            LoginProtectionService loginProtectionService,
            TermsService termsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.loginProtectionService = loginProtectionService;
        this.termsService = termsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        termsService.requireCurrent(request.getTermsVersion());
        loginProtectionService.checkRegistrationAllowed(clientIp);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        termsService.recordAcceptance(user.getId(), request.getTermsVersion(), clientIp);
        accountService.sendVerification(user);

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }

    /**
     * Conta criada pelo link de um convite: quem abriu o link recebido no e-mail já comprovou que ele é seu,
     * então a conta nasce com o e-mail confirmado.
     */
    public User registerVerified(String name, String email, String password, String termsVersion, String clientIp) {
        termsService.requireCurrent(termsVersion);
        loginProtectionService.checkRegistrationAllowed(clientIp);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe uma conta com este e-mail. Entre com ela para aceitar o convite.");
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        termsService.recordAcceptance(user.getId(), termsVersion, clientIp);
        return user;
    }

    public AuthResponse tokenFor(User user) {
        return new AuthResponse(jwtService.generateToken(user.getId()), user.getId(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        loginProtectionService.checkLoginAllowed(request.getEmail(), clientIp);
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginProtectionService.loginFailed(request.getEmail(), clientIp);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        loginProtectionService.loginSucceeded(request.getEmail());
        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }
}
