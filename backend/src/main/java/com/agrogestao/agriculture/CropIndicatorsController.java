package com.agrogestao.agriculture;

import com.agrogestao.agriculture.dto.CropIndicators;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores da Agricultura (custo/ha, produção/ha, receita/ha, margem/ha) da propriedade atual. */
@RestController
@RequestMapping("/api/crop-indicators")
public class CropIndicatorsController {

    private final CropIndicatorsService cropIndicatorsService;

    public CropIndicatorsController(CropIndicatorsService cropIndicatorsService) {
        this.cropIndicatorsService = cropIndicatorsService;
    }

    @GetMapping
    public CropIndicators get(
            @RequestParam(required = false) Long cropCycleId,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return cropIndicatorsService.get(cropCycleId, fieldId, from, to);
    }
}
