package com.agrogestao.livestock;

import com.agrogestao.common.PageResponse;
import com.agrogestao.livestock.dto.AnimalDetailResponse;
import com.agrogestao.livestock.dto.AnimalExitRequest;
import com.agrogestao.livestock.dto.AnimalRequest;
import com.agrogestao.livestock.dto.AnimalResponse;
import com.agrogestao.livestock.dto.BatchCountRequest;
import com.agrogestao.livestock.dto.BatchRequest;
import com.agrogestao.livestock.dto.BatchResponse;
import com.agrogestao.livestock.dto.AnimalEventResponse;
import com.agrogestao.livestock.dto.HealthApplicationRequest;
import com.agrogestao.livestock.dto.HealthApplicationResponse;
import com.agrogestao.livestock.dto.HerdSummary;
import com.agrogestao.livestock.dto.LivestockAlerts;
import com.agrogestao.livestock.dto.LivestockIndicators;
import com.agrogestao.livestock.dto.WeighingRequest;
import com.agrogestao.livestock.dto.LivestockPurchaseRequest;
import com.agrogestao.livestock.dto.LivestockSaleRequest;
import com.agrogestao.livestock.dto.MoveAnimalsRequest;
import com.agrogestao.livestock.dto.ReproductiveEventRequest;
import com.agrogestao.livestock.dto.ReproductiveEventResponse;
import com.agrogestao.livestock.dto.SpeciesRequest;
import com.agrogestao.livestock.dto.SpeciesResponse;
import com.agrogestao.livestock.dto.TradeDetailResponse;
import com.agrogestao.livestock.dto.TradeResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
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

/** Pecuária da propriedade atual (header X-Property-Id): lotes, animais e compra/venda de animais. */
@RestController
@RequestMapping("/api/livestock")
public class LivestockController {

    private final AnimalBatchService batchService;
    private final AnimalService animalService;
    private final LivestockTradeService tradeService;
    private final WeighingService weighingService;
    private final HealthService healthService;
    private final SpeciesCatalogService speciesService;
    private final ReproductiveEventService reproductiveEventService;
    private final LivestockIndicatorsService indicatorsService;

    public LivestockController(AnimalBatchService batchService, AnimalService animalService,
            LivestockTradeService tradeService, WeighingService weighingService, HealthService healthService,
            SpeciesCatalogService speciesService, ReproductiveEventService reproductiveEventService,
            LivestockIndicatorsService indicatorsService) {
        this.batchService = batchService;
        this.animalService = animalService;
        this.tradeService = tradeService;
        this.weighingService = weighingService;
        this.healthService = healthService;
        this.speciesService = speciesService;
        this.reproductiveEventService = reproductiveEventService;
        this.indicatorsService = indicatorsService;
    }

    @GetMapping("/summary")
    public HerdSummary summary() {
        return batchService.summary();
    }

    // Espécies

    @GetMapping("/species")
    public List<SpeciesResponse> listSpecies(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return speciesService.list(includeArchived);
    }

    @PostMapping("/species")
    public SpeciesResponse createSpecies(@Valid @RequestBody SpeciesRequest request) {
        return speciesService.create(request);
    }

    @PostMapping("/species/{id}/archive")
    public SpeciesResponse archiveSpecies(@PathVariable Long id) {
        return speciesService.archive(id);
    }

    @PostMapping("/species/{id}/unarchive")
    public SpeciesResponse unarchiveSpecies(@PathVariable Long id) {
        return speciesService.unarchive(id);
    }

    @DeleteMapping("/species/{id}")
    public ResponseEntity<Void> deleteSpecies(@PathVariable Long id) {
        speciesService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Lotes

    @GetMapping("/batches")
    public List<BatchResponse> listBatches(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return batchService.list(includeArchived);
    }

    @PostMapping("/batches")
    public BatchResponse createBatch(@Valid @RequestBody BatchRequest request) {
        return batchService.create(request);
    }

    @PutMapping("/batches/{id}")
    public BatchResponse updateBatch(@PathVariable Long id, @Valid @RequestBody BatchRequest request) {
        return batchService.update(id, request);
    }

    @PostMapping("/batches/{id}/archive")
    public BatchResponse archiveBatch(@PathVariable Long id) {
        return batchService.setArchived(id, true);
    }

    @PostMapping("/batches/{id}/unarchive")
    public BatchResponse unarchiveBatch(@PathVariable Long id) {
        return batchService.setArchived(id, false);
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Lote por contagem: entrada de cabeças sem compra (nascimento, eclosão...). */
    @PostMapping("/batches/{id}/entries")
    public BatchResponse addHeads(@PathVariable Long id, @Valid @RequestBody BatchCountRequest request) {
        return batchService.addHeads(id, request);
    }

    /** Lote por contagem: saída de cabeças sem venda (morte, descarte...). */
    @PostMapping("/batches/{id}/exits")
    public BatchResponse removeHeads(@PathVariable Long id, @Valid @RequestBody BatchCountRequest request) {
        return batchService.removeHeads(id, request);
    }

    // Animais

    @GetMapping("/animals")
    public PageResponse<AnimalResponse> listAnimals(
            @RequestParam(required = false) AnimalStatus status,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return animalService.search(status, batchId, speciesId, q, PageResponse.pageable(page, size));
    }

    @GetMapping("/animals/{id}")
    public AnimalDetailResponse animal(@PathVariable Long id) {
        return animalService.detail(id);
    }

    @PostMapping("/animals")
    public AnimalResponse createAnimal(@Valid @RequestBody AnimalRequest request) {
        return animalService.create(request);
    }

    @PutMapping("/animals/{id}")
    public AnimalResponse updateAnimal(@PathVariable Long id, @Valid @RequestBody AnimalRequest request) {
        return animalService.update(id, request);
    }

    @DeleteMapping("/animals/{id}")
    public ResponseEntity<Void> deleteAnimal(@PathVariable Long id) {
        animalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/animals/move")
    public List<AnimalResponse> moveAnimals(@Valid @RequestBody MoveAnimalsRequest request) {
        return animalService.move(request);
    }

    @PostMapping("/animals/{id}/exit")
    public AnimalResponse exit(@PathVariable Long id, @Valid @RequestBody AnimalExitRequest request) {
        return animalService.exit(id, request);
    }

    @PostMapping("/animals/{id}/reactivate")
    public AnimalResponse reactivate(@PathVariable Long id) {
        return animalService.reactivate(id);
    }

    // Compra e venda de animais

    @GetMapping("/trades")
    public PageResponse<TradeResponse> listTrades(
            @RequestParam(required = false) TradeKind kind,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return tradeService.list(kind, PageResponse.pageable(page, size));
    }

    @GetMapping("/trades/{id}")
    public TradeDetailResponse trade(@PathVariable Long id) {
        return tradeService.detail(id);
    }

    @PostMapping("/purchases")
    public TradeResponse purchase(@Valid @RequestBody LivestockPurchaseRequest request) {
        return tradeService.purchase(request);
    }

    @PostMapping("/sales")
    public TradeResponse sale(@Valid @RequestBody LivestockSaleRequest request) {
        return tradeService.sale(request);
    }

    @PostMapping("/trades/{id}/cancel")
    public TradeResponse cancelTrade(@PathVariable Long id) {
        return tradeService.cancel(id);
    }

    // Pesagens

    @PostMapping("/weighings")
    public List<AnimalEventResponse> weigh(@Valid @RequestBody WeighingRequest request) {
        return weighingService.register(request);
    }

    @DeleteMapping("/weighings/{eventId}")
    public ResponseEntity<Void> deleteWeighing(@PathVariable Long eventId) {
        weighingService.delete(eventId);
        return ResponseEntity.noContent().build();
    }

    // Sanidade e alertas

    @GetMapping("/health")
    public PageResponse<HealthApplicationResponse> listHealth(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return healthService.list(PageResponse.pageable(page, size));
    }

    @PostMapping("/health")
    public HealthApplicationResponse applyHealth(@Valid @RequestBody HealthApplicationRequest request) {
        return healthService.apply(request);
    }

    @DeleteMapping("/health/{id}")
    public ResponseEntity<Void> undoHealth(@PathVariable Long id) {
        healthService.undo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/health/{id}/follow-up-done")
    public HealthApplicationResponse dismissFollowUp(@PathVariable Long id) {
        return healthService.dismissFollowUp(id);
    }

    @GetMapping("/alerts")
    public LivestockAlerts alerts() {
        return new LivestockAlerts(healthService.pendingFollowUps(), healthService.withdrawalAlerts(),
                reproductiveEventService.upcomingCalvings(), reproductiveEventService.pendingDiagnoses());
    }

    // Reprodução

    @GetMapping("/reproduction")
    public PageResponse<ReproductiveEventResponse> listReproduction(
            @RequestParam(required = false) Long animalId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return reproductiveEventService.list(animalId, PageResponse.pageable(page, size));
    }

    @PostMapping("/reproduction")
    public ReproductiveEventResponse registerReproduction(@Valid @RequestBody ReproductiveEventRequest request) {
        return reproductiveEventService.register(request);
    }

    @DeleteMapping("/reproduction/{id}")
    public ResponseEntity<Void> deleteReproduction(@PathVariable Long id) {
        reproductiveEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Indicadores

    @GetMapping("/indicators")
    public LivestockIndicators indicators(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return indicatorsService.get(batchId, from, to);
    }
}
