package com.agrogestao.activities;

import com.agrogestao.activities.dto.ActivityRequest;
import com.agrogestao.activities.dto.ActivityResponse;
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

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public List<ActivityResponse> list() {
        return activityService.list();
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.create(request));
    }

    @PutMapping("/{id}")
    public ActivityResponse update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return activityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
