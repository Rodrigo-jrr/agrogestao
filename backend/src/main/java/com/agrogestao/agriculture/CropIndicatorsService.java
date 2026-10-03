package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropIndicators;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.fields.Field;
import com.agrogestao.fields.FieldRepository;
import com.agrogestao.properties.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Custo, produção, receita e margem por hectare — de uma safra, de um talhão (todas as suas safras) ou de
 * toda a propriedade. Calcula em memória sobre operações e colheitas já registradas, sem tabela própria,
 * mesmo padrão de {@code LivestockIndicatorsService}.
 */
@Service
public class CropIndicatorsService {

    private final CropCycleRepository cropCycleRepository;
    private final CropOperationRepository cropOperationRepository;
    private final CropHarvestRepository cropHarvestRepository;
    private final FieldRepository fieldRepository;
    private final CropRepository cropRepository;
    private final TenantContext tenantContext;

    public CropIndicatorsService(CropCycleRepository cropCycleRepository, CropOperationRepository cropOperationRepository,
            CropHarvestRepository cropHarvestRepository, FieldRepository fieldRepository, CropRepository cropRepository,
            TenantContext tenantContext) {
        this.cropCycleRepository = cropCycleRepository;
        this.cropOperationRepository = cropOperationRepository;
        this.cropHarvestRepository = cropHarvestRepository;
        this.fieldRepository = fieldRepository;
        this.cropRepository = cropRepository;
        this.tenantContext = tenantContext;
    }

    /** {@code cropCycleId} tem prioridade: se informado, {@code fieldId} é ignorado. */
    public CropIndicators get(Long cropCycleId, Long fieldId, LocalDate from, LocalDate to) {
        Long propertyId = tenantContext.requirePropertyId();

        Long scopeCropCycleId = null;
        String cropCycleLabel = null;
        Long scopeFieldId = null;
        String fieldName = null;
        List<CropCycle> cycles;
        BigDecimal totalAreaHa;

        if (cropCycleId != null) {
            CropCycle cycle = cropCycleRepository.findById(cropCycleId)
                    .filter(c -> c.getPropertyId().equals(propertyId))
                    .orElseThrow(() -> new BusinessException("Safra inválida para esta propriedade"));
            Field field = fieldRepository.findById(cycle.getFieldId()).orElseThrow();
            Crop crop = cropRepository.findById(cycle.getCropId()).orElseThrow();
            cycles = List.of(cycle);
            scopeCropCycleId = cycle.getId();
            cropCycleLabel = crop.getName() + " em " + field.getName();
            scopeFieldId = field.getId();
            fieldName = field.getName();
            totalAreaHa = field.getAreaHa();
        } else if (fieldId != null) {
            Field field = fieldRepository.findById(fieldId)
                    .filter(f -> f.getPropertyId().equals(propertyId))
                    .orElseThrow(() -> new BusinessException("Talhão inválido para esta propriedade"));
            cycles = cropCycleRepository.findByPropertyIdAndFieldIdOrderByPlannedPlantingDateDescIdDesc(propertyId, fieldId);
            scopeFieldId = field.getId();
            fieldName = field.getName();
            totalAreaHa = field.getAreaHa();
        } else {
            cycles = cropCycleRepository.findByPropertyIdOrderByPlannedPlantingDateDescIdDesc(propertyId);
            Set<Long> fieldIds = cycles.stream().map(CropCycle::getFieldId).collect(Collectors.toSet());
            totalAreaHa = fieldRepository.findAllById(fieldIds).stream().map(Field::getAreaHa)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<Long> cycleIds = cycles.stream().map(CropCycle::getId).toList();
        BigDecimal totalCost = cycleIds.isEmpty() ? BigDecimal.ZERO
                : cropOperationRepository.findByPropertyIdAndCropCycleIdIn(propertyId, cycleIds).stream()
                        .filter(o -> inPeriod(o.getOperationDate(), from, to))
                        .map(o -> o.getTotalCost() != null ? o.getTotalCost() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CropHarvest> harvests = cycleIds.isEmpty() ? List.of()
                : cropHarvestRepository.findByPropertyIdAndCropCycleIdIn(propertyId, cycleIds).stream()
                        .filter(h -> inPeriod(h.getHarvestDate(), from, to))
                        .toList();

        Map<String, BigDecimal> harvestedQuantityByUnit = new HashMap<>();
        for (CropHarvest harvest : harvests) {
            harvestedQuantityByUnit.merge(harvest.getQuantityUnit(), harvest.getQuantity(), BigDecimal::add);
        }
        BigDecimal revenue = harvests.stream().filter(h -> h.getDestination() == CropHarvestDestination.VENDA)
                .map(h -> h.getTotalAmount() != null ? h.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasArea = totalAreaHa.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal costPerHa = hasArea ? totalCost.divide(totalAreaHa, 2, RoundingMode.HALF_UP) : null;
        BigDecimal revenuePerHa = hasArea ? revenue.divide(totalAreaHa, 2, RoundingMode.HALF_UP) : null;
        BigDecimal marginPerHa = costPerHa != null && revenuePerHa != null ? revenuePerHa.subtract(costPerHa) : null;
        Map<String, BigDecimal> productionPerHaByUnit = hasArea
                ? harvestedQuantityByUnit.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().divide(totalAreaHa, 3, RoundingMode.HALF_UP)))
                : Map.of();

        return new CropIndicators(scopeCropCycleId, cropCycleLabel, scopeFieldId, fieldName, from, to, cycles.size(),
                totalAreaHa, totalCost, costPerHa, harvestedQuantityByUnit, productionPerHaByUnit, revenue,
                revenuePerHa, marginPerHa);
    }

    private static boolean inPeriod(LocalDate date, LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
}
