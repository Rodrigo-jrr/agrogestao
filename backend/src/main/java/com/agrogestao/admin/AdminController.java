package com.agrogestao.admin;

import com.agrogestao.admin.dto.AdminOverviewResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Painel da plataforma (somente leitura). Protegido em duas camadas: regra de rota no SecurityConfig
 * e {@code @PreAuthorize} aqui, ambas consultando {@link PlatformAdminService}.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("@platformAdmin.check(authentication)")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return adminService.overview();
    }
}
