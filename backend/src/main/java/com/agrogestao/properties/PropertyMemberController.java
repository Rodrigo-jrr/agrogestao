package com.agrogestao.properties;

import com.agrogestao.properties.dto.MemberRequest;
import com.agrogestao.properties.dto.MemberResponse;
import com.agrogestao.properties.dto.MemberRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Membros da propriedade atual (definida pelo header X-Property-Id). */
@RestController
@RequestMapping("/api/properties/current/members")
public class PropertyMemberController {

    private final PropertyMemberService propertyMemberService;

    public PropertyMemberController(PropertyMemberService propertyMemberService) {
        this.propertyMemberService = propertyMemberService;
    }

    @GetMapping
    public List<MemberResponse> list() {
        return propertyMemberService.list();
    }

    @PostMapping
    public ResponseEntity<MemberResponse> add(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.ok(propertyMemberService.add(request));
    }

    @PutMapping("/{id}")
    public MemberResponse updateRole(@PathVariable Long id, @Valid @RequestBody MemberRoleRequest request) {
        return propertyMemberService.updateRole(id, request.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        propertyMemberService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
