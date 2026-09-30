package com.agrogestao.auth;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.LoginRequest;
import com.agrogestao.auth.dto.RegisterRequest;
import com.agrogestao.common.exception.BusinessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final LoginProtectionService loginProtectionService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountService accountService,
            LoginProtectionService loginProtectionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.loginProtectionService = loginProtectionService;
    }

    public AuthResponse register(RegisterRequest request, String clientIp) {
        loginProtectionService.checkRegistrationAllowed(clientIp);
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        accountService.sendVerification(user);

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
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
