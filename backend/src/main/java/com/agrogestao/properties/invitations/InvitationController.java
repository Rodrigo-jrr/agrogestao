package com.agrogestao.properties.invitations;

import com.agrogestao.auth.dto.AuthResponse;
import com.agrogestao.auth.dto.TokenRequest;
import com.agrogestao.properties.dto.MemberRequest;
import com.agrogestao.properties.dto.PropertySummaryResponse;
import com.agrogestao.properties.invitations.dto.InvitationPreview;
import com.agrogestao.properties.invitations.dto.InvitationRegisterRequest;
import com.agrogestao.properties.invitations.dto.InvitationResponse;
import com.agrogestao.properties.invitations.dto.InviteResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    // Gestão pela equipe da propriedade atual (header X-Property-Id).

    @PostMapping("/api/properties/current/invitations")
    public InviteResult invite(@Valid @RequestBody MemberRequest request) {
        return invitationService.invite(request);
    }

    @GetMapping("/api/properties/current/invitations")
    public List<InvitationResponse> list() {
        return invitationService.list();
    }

    @PostMapping("/api/properties/current/invitations/{id}/resend")
    public InvitationResponse resend(@PathVariable Long id) {
        return invitationService.resend(id);
    }

    @DeleteMapping("/api/properties/current/invitations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        invitationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    // Quem recebeu o convite. O token vai no corpo (POST), não na URL, para não parar em logs.

    @PostMapping("/api/auth/invitations/preview")
    public InvitationPreview preview(@Valid @RequestBody TokenRequest request) {
        return invitationService.preview(request.token());
    }

    @PostMapping("/api/auth/invitations/register")
    public AuthResponse register(@Valid @RequestBody InvitationRegisterRequest request, HttpServletRequest http) {
        return invitationService.registerAndAccept(request.token(), request.name(), request.password(),
                request.termsVersion(), http.getRemoteAddr());
    }

    @PostMapping("/api/invitations/accept")
    public PropertySummaryResponse accept(@Valid @RequestBody TokenRequest request) {
        return invitationService.accept(request.token());
    }
}
