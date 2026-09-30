package com.agrogestao.audit;

import com.agrogestao.audit.dto.AuditAuthor;
import com.agrogestao.audit.dto.AuditEntryResponse;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.Period;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de alterações da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/properties/current/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /** {@code entity} pode se repetir (ex.: entity=AccountPayable&entity=AccountReceivable). */
    @GetMapping
    public PageResponse<AuditEntryResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Set<String> entity,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return auditQueryService.search(Period.resolve(from, to), userId, entity, PageResponse.pageable(page, size));
    }

    @GetMapping("/authors")
    public List<AuditAuthor> authors() {
        return auditQueryService.authors();
    }
}
