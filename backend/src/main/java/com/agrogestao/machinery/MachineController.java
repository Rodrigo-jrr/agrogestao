package com.agrogestao.machinery;

import com.agrogestao.machinery.dto.MachineReadingRequest;
import com.agrogestao.machinery.dto.MachineRequest;
import com.agrogestao.machinery.dto.MachineResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Máquinas e implementos da propriedade atual (header X-Property-Id). */
@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    public List<MachineResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return machineService.list(includeArchived);
    }

    @PostMapping
    public MachineResponse create(@Valid @RequestBody MachineRequest request) {
        return machineService.create(request);
    }

    @PutMapping("/{id}")
    public MachineResponse update(@PathVariable Long id, @Valid @RequestBody MachineRequest request) {
        return machineService.update(id, request);
    }

    @PostMapping("/{id}/reading")
    public MachineResponse updateReading(@PathVariable Long id, @Valid @RequestBody MachineReadingRequest request) {
        return machineService.updateReading(id, request);
    }

    @PostMapping("/{id}/archive")
    public MachineResponse archive(@PathVariable Long id) {
        return machineService.archive(id);
    }

    @PostMapping("/{id}/unarchive")
    public MachineResponse unarchive(@PathVariable Long id) {
        return machineService.unarchive(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        machineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
