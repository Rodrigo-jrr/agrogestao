package com.agrogestao.properties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final PropertyMemberRepository propertyMemberRepository;
    private final TenantContext tenantContext;

    public TenantContextFilter(PropertyMemberRepository propertyMemberRepository, TenantContext tenantContext) {
        this.propertyMemberRepository = propertyMemberRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String propertyIdHeader = request.getHeader("X-Property-Id");
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (propertyIdHeader != null && authentication != null
                && authentication.getPrincipal() instanceof Long userId) {
            try {
                Long propertyId = Long.parseLong(propertyIdHeader);
                propertyMemberRepository.findByPropertyIdAndUserId(propertyId, userId)
                        .ifPresent(member -> {
                            tenantContext.setPropertyId(propertyId);
                            tenantContext.setRole(member.getRole());
                            tenantContext.setResolved(true);
                        });
            } catch (NumberFormatException ignored) {
                // header inválido, TenantContext permanece não resolvido
            }
        }
        filterChain.doFilter(request, response);
    }
}
