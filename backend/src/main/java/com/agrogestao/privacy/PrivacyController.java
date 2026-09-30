package com.agrogestao.privacy;

import com.agrogestao.auth.SecurityUtils;
import com.agrogestao.privacy.dto.DeleteAccountRequest;
import com.agrogestao.privacy.dto.DeletionPreview;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Direitos do titular (LGPD, art. 18): exportar e excluir os próprios dados. */
@RestController
public class PrivacyController {

    private final DataExportService dataExportService;
    private final AccountDeletionService accountDeletionService;

    public PrivacyController(DataExportService dataExportService, AccountDeletionService accountDeletionService) {
        this.dataExportService = dataExportService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping("/api/me/export")
    public ResponseEntity<Map<String, Object>> export() {
        String filename = "agrogestao-meus-dados-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(dataExportService.export(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/api/me/deletion-preview")
    public DeletionPreview deletionPreview() {
        return accountDeletionService.preview(SecurityUtils.getCurrentUserId());
    }

    @DeleteMapping("/api/me")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request, HttpServletRequest http) {
        accountDeletionService.deleteAccount(SecurityUtils.getCurrentUserId(), request.password(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
