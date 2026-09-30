package com.agrogestao.auth.token;

import java.time.Duration;

public enum TokenPurpose {
    VERIFY_EMAIL(Duration.ofHours(24)),
    RESET_PASSWORD(Duration.ofHours(1));

    private final Duration validity;

    TokenPurpose(Duration validity) {
        this.validity = validity;
    }

    public Duration validity() {
        return validity;
    }
}
