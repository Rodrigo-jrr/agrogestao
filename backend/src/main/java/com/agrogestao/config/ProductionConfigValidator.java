package com.agrogestao.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No perfil "prod", impede a subida da aplicação com configuração de desenvolvimento
 * (segredo JWT padrão/curto ou CORS liberado para localhost).
 */
@Component
@Profile("prod")
public class ProductionConfigValidator implements InitializingBean {

    static final String DEV_JWT_SECRET = "dev-only-secret-change-me-please-32bytes-minimum";
    private static final int MIN_SECRET_BYTES = 32;

    private final String jwtSecret;
    private final String allowedOrigins;

    public ProductionConfigValidator(
            @Value("${agrogestao.jwt.secret}") String jwtSecret,
            @Value("${agrogestao.cors.allowed-origins}") String allowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> problems = new ArrayList<>();
        if (jwtSecret.isBlank() || DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("JWT_SECRET não definido ou usando o valor de desenvolvimento");
        } else if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            problems.add("JWT_SECRET deve ter ao menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (allowedOrigins.isBlank() || allowedOrigins.contains("localhost")) {
            problems.add("CORS_ALLOWED_ORIGINS deve listar o(s) domínio(s) reais do frontend");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Configuração de produção inválida: " + String.join("; ", problems));
        }
    }
}
