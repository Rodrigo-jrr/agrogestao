package com.agrogestao.machinery;

import com.agrogestao.machinery.dto.MachineTypeRequest;
import com.agrogestao.machinery.dto.MachineTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de tipos de máquina da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/machine-types")
public class MachineTypeController {

    private final MachineTypeService machineTypeService;

    public MachineTypeController(MachineTypeService machineTypeService) {
        this.machineTypeService = machineTypeService;
    }

    @GetMapping
    public List<MachineTypeResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return machineTypeService.list(includeArchived);
    }

    @PostMapping
    public MachineTypeResponse create(@Valid @RequestBody MachineTypeRequest request) {
        return machineTypeService.create(request);
    }

    @PostMapping("/{id}/archive")
    public MachineTypeResponse archive(@PathVariable Long id) {
        return machineTypeService.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public MachineTypeResponse unarchive(@PathVariable Long id) {
        return machineTypeService.unarchive(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        machineTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
