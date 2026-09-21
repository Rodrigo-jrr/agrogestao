package com.agrogestao.activities;

import com.agrogestao.activities.dto.ActivityRequest;
import com.agrogestao.activities.dto.ActivityResponse;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.properties.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TenantContext tenantContext;

    public ActivityService(ActivityRepository activityRepository, TenantContext tenantContext) {
        this.activityRepository = activityRepository;
        this.tenantContext = tenantContext;
    }

    public List<ActivityResponse> list() {
        Long propertyId = tenantContext.requirePropertyId();
        return activityRepository.findByPropertyId(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ActivityResponse create(ActivityRequest request) {
        tenantContext.requireWriteAccess();
        Activity activity = new Activity();
        activity.setPropertyId(tenantContext.requirePropertyId());
        applyRequest(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    public ActivityResponse update(Long id, ActivityRequest request) {
        tenantContext.requireWriteAccess();
        Activity activity = findOwned(id);
        applyRequest(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    public void delete(Long id) {
        tenantContext.requireManageAccess();
        activityRepository.delete(findOwned(id));
    }

    private Activity findOwned(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada"));
        if (!activity.getPropertyId().equals(propertyId)) {
            throw new ResourceNotFoundException("Atividade não encontrada");
        }
        return activity;
    }

    private void applyRequest(Activity activity, ActivityRequest request) {
        activity.setName(request.getName());
        activity.setType(request.getType());
        activity.setAreaHa(request.getAreaHa());
        activity.setResponsible(request.getResponsible());
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(), activity.getName(), activity.getType(),
                activity.getAreaHa(), activity.getResponsible());
    }
}
