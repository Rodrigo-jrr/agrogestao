package com.agrogestao.properties;

import com.agrogestao.common.exception.ForbiddenException;
import java.util.EnumSet;
import java.util.Set;
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

    private static final Set<PropertyRole> MANAGE_ROLES = EnumSet.of(PropertyRole.OWNER, PropertyRole.MANAGER);
    private static final Set<PropertyRole> WRITE_ROLES =
            EnumSet.of(PropertyRole.OWNER, PropertyRole.MANAGER, PropertyRole.WORKER);

    private Long propertyId;
    private PropertyRole role;
    private boolean resolved;

    public Long requirePropertyId() {
        if (!resolved || propertyId == null) {
            throw new ForbiddenException(
                    "Nenhuma propriedade válida informada. Envie o header X-Property-Id.");
        }
        return propertyId;
    }

    /** Bloqueia VIEWER; permite criar/editar registros do dia a dia. */
    public void requireWriteAccess() {
        requirePropertyId();
        if (!WRITE_ROLES.contains(role)) {
            throw new ForbiddenException("Seu papel na propriedade não permite esta ação.");
        }
    }

    /** Exige OWNER ou MANAGER; usado para exclusões e alterações de dados mestres/financeiros. */
    public void requireManageAccess() {
        requirePropertyId();
        if (!MANAGE_ROLES.contains(role)) {
            throw new ForbiddenException("Apenas proprietários ou gerentes podem realizar esta ação.");
        }
    }
}
