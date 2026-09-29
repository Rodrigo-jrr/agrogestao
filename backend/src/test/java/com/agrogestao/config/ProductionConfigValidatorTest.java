package com.agrogestao.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionConfigValidatorTest {

    private static final String STRONG_SECRET = "3f9c1a7e5b2d4f6a8c0e1b3d5f7a9c2e4b6d8f0a";

    @Test
    void acceptsProductionConfiguration() {
        assertThatCode(() -> new ProductionConfigValidator(STRONG_SECRET, "https://app.agrorust.com.br")
                .afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentSecret() {
        assertThatThrownBy(() -> new ProductionConfigValidator(ProductionConfigValidator.DEV_JWT_SECRET,
                "https://app.agrorust.com.br").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new ProductionConfigValidator("curto", "https://app.agrorust.com.br")
                .afterPropertiesSet())
                .hasMessageContaining("32 bytes");
    }

    @Test
    void rejectsLocalhostCors() {
        assertThatThrownBy(() -> new ProductionConfigValidator(STRONG_SECRET, "http://localhost:*")
                .afterPropertiesSet())
                .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }
}
