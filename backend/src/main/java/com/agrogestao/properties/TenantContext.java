package com.agrogestao.properties;

import com.agrogestao.common.exception.ForbiddenException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Setter
@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TenantContext {

    private Long propertyId;
    private boolean resolved;

    public Long requirePropertyId() {
        if (!resolved || propertyId == null) {
            throw new ForbiddenException(
                    "Nenhuma propriedade válida informada. Envie o header X-Property-Id.");
        }
        return propertyId;
    }
}
