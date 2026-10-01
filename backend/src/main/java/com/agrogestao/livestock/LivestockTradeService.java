package com.agrogestao.livestock;

import com.agrogestao.audit.AuditAction;
import com.agrogestao.audit.AuditService;
import com.agrogestao.common.Money;
import com.agrogestao.common.PageResponse;
import com.agrogestao.common.exception.BusinessException;
import com.agrogestao.common.exception.ResourceNotFoundException;
import com.agrogestao.finance.AccountPayableService;
import com.agrogestao.finance.AccountReceivableService;
import com.agrogestao.finance.FinancialTransaction;
import com.agrogestao.finance.FinancialTransactionRepository;
import com.agrogestao.finance.FinancialTransactionService;
import com.agrogestao.finance.InstallmentPlan;
import com.agrogestao.finance.PaymentType;
import com.agrogestao.finance.TransactionType;
import com.agrogestao.finance.dto.InstallmentInput;
import com.agrogestao.livestock.dto.AnimalResponse;
import com.agrogestao.livestock.dto.LivestockPurchaseRequest;
import com.agrogestao.livestock.dto.LivestockSaleRequest;
import com.agrogestao.livestock.dto.NewAnimalInput;
import com.agrogestao.livestock.dto.TradeDetailResponse;
import com.agrogestao.livestock.dto.TradeResponse;
import com.agrogestao.properties.TenantContext;
import com.agrogestao.purchases.Supplier;
import com.agrogestao.purchases.SupplierRepository;
import com.agrogestao.sales.Customer;
import com.agrogestao.sales.CustomerRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compra e venda de animais — com brinco ou por contagem de um lote — integradas ao financeiro como as
 * compras e vendas de produtos: à vista gera o lançamento no caixa; a prazo, uma conta por parcela.
 * Cancelar desfaz o rebanho e o financeiro, e mantém a operação no histórico como cancelada.
 */
@Service
public class LivestockTradeService {

    private final LivestockTradeRepository tradeRepository;
    private final AnimalRepository animalRepository;
    private final AnimalBatchRepository batchRepository;
    private final AnimalEventRepository eventRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final FinancialTransactionService financialTransactionService;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final AccountPayableService accountPayableService;
    private final AccountReceivableService accountReceivableService;
    private final LivestockSupport support;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public LivestockTradeService(
            LivestockTradeRepository tradeRepository,
            AnimalRepository animalRepository,
            AnimalBatchRepository batchRepository,
            AnimalEventRepository eventRepository,
            SupplierRepository supplierRepository,
            CustomerRepository customerRepository,
            FinancialTransactionService financialTransactionService,
            FinancialTransactionRepository financialTransactionRepository,
            AccountPayableService accountPayableService,
            AccountReceivableService accountReceivableService,
            LivestockSupport support,
            TenantContext tenantContext,
            AuditService auditService) {
        this.tradeRepository = tradeRepository;
        this.animalRepository = animalRepository;
        this.batchRepository = batchRepository;
        this.eventRepository = eventRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.financialTransactionService = financialTransactionService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.accountPayableService = accountPayableService;
        this.accountReceivableService = accountReceivableService;
        this.support = support;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    public PageResponse<TradeResponse> list(TradeKind kind, Pageable pageable) {
        Long propertyId = tenantContext.requirePropertyId();
        Page<LivestockTrade> page = tradeRepository.search(propertyId, kind, pageable);
        List<TradeResponse> content = toResponses(propertyId, page.getContent());
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public TradeDetailResponse detail(Long id) {
        Long propertyId = tenantContext.requirePropertyId();
        LivestockTrade trade = findOwned(propertyId, id);
        List<Long> animalIds = eventRepository.findByTradeId(id).stream()
                .map(AnimalEvent::getAnimalId).filter(Objects::nonNull).distinct().toList();
        List<AnimalResponse> animals = support.toResponses(propertyId, animalRepository.findAllById(animalIds));
        return new TradeDetailResponse(toResponses(propertyId, List.of(trade)).get(0), animals,
                trade.getKind() == TradeKind.PURCHASE
                        ? accountPayableService.livestockInstallmentsOf(id)
                        : accountReceivableService.livestockInstallmentsOf(id));
    }

    @Transactional
    public TradeResponse purchase(LivestockPurchaseRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .filter(s -> s.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Fornecedor inválido para esta propriedade"));
        LivestockSupport.requireNotFuture(request.tradeDate());
        PaymentType paymentType = InstallmentPlan.validate(request.paymentType(), request.installments(),
                request.totalAmount(), request.tradeDate());
        boolean individual = request.animals() != null && !request.animals().isEmpty();
        if (individual == (request.headCount() != null)) {
            throw new BusinessException("Informe os animais (com brinco) ou a quantidade de cabeças de um lote por contagem.");
        }

        LivestockTrade trade = newTrade(propertyId, TradeKind.PURCHASE, request.tradeDate(), request.totalAmount(),
                paymentType, request.installments(), request.notes());
        trade.setSupplierId(supplier.getId());

        if (individual) {
            AnimalBatch batch = null;
            Long speciesId = request.speciesId();
            if (request.batchId() != null) {
                batch = support.requireIndividualBatch(propertyId, request.batchId(), speciesId);
                speciesId = batch.getSpeciesId();
            }
            if (speciesId == null) {
                throw new BusinessException("Informe a espécie dos animais comprados.");
            }
            SpeciesCatalog species = support.requireActiveSpecies(propertyId, speciesId);
            LivestockSupport.requireIndividualSpecies(species);
            validateNewAnimals(propertyId, request.animals(), request.tradeDate());
            trade.setHeadCount(request.animals().size());
            trade.setBatchId(null);
            tradeRepository.save(trade);

            List<BigDecimal> shares = LivestockSupport.splitAmount(request.totalAmount(), request.animals().size());
            for (int i = 0; i < request.animals().size(); i++) {
                NewAnimalInput input = request.animals().get(i);
                Animal animal = new Animal();
                animal.setPropertyId(propertyId);
                animal.setTag(input.tag().trim());
                animal.setName(AnimalBatchService.blankToNull(input.name()));
                animal.setSpeciesId(species.getId());
                animal.setBreed(AnimalBatchService.blankToNull(input.breed()));
                animal.setSex(input.sex());
                animal.setBirthDate(input.birthDate());
                animal.setBirthDateEstimated(input.birthDate() != null && Boolean.TRUE.equals(input.birthDateEstimated()));
                animal.setOrigin(AnimalOrigin.COMPRA);
                animal.setEntryDate(request.tradeDate());
                animal.setBatchId(batch != null ? batch.getId() : null);
                animal.setCurrentWeight(input.weight());
                animalRepository.save(animal);
                support.saveEvent(propertyId, AnimalEventType.ENTRADA, request.tradeDate(), animal, batch, null,
                        input.weight(), trade.getId(), shares.get(i), "Compra de " + supplier.getName());
            }
        } else {
            if (request.batchId() == null) {
                throw new BusinessException("Escolha o lote por contagem que recebe as cabeças compradas.");
            }
            AnimalBatch batch = support.requireAggregateBatch(propertyId, request.batchId());
            batch.setHeadCount(batch.getHeadCount() + request.headCount());
            batchRepository.save(batch);
            trade.setHeadCount(request.headCount());
            trade.setBatchId(batch.getId());
            tradeRepository.save(trade);
            support.saveEvent(propertyId, AnimalEventType.ENTRADA, request.tradeDate(), null, batch,
                    request.headCount(), null, trade.getId(), request.totalAmount(), "Compra de " + supplier.getName());
        }

        if (paymentType == PaymentType.CASH) {
            FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                    TransactionType.EXPENSE, "Compra de animais #" + trade.getId(), request.totalAmount(),
                    request.tradeDate(), null);
            trade.setFinancialTransactionId(transaction.getId());
            tradeRepository.save(trade);
        } else {
            accountPayableService.createLivestockInstallments(propertyId, supplier.getId(), trade.getId(),
                    request.installments());
        }
        auditService.record("LivestockTrade", trade.getId(), AuditAction.CREATE, describe(trade, supplier.getName()));
        return toResponses(propertyId, List.of(trade)).get(0);
    }

    @Transactional
    public TradeResponse sale(LivestockSaleRequest request) {
        tenantContext.requireWriteAccess();
        Long propertyId = tenantContext.requirePropertyId();
        Customer customer = customerRepository.findById(request.customerId())
                .filter(c -> c.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new BusinessException("Cliente inválido para esta propriedade"));
        LivestockSupport.requireNotFuture(request.tradeDate());
        PaymentType paymentType = InstallmentPlan.validate(request.paymentType(), request.installments(),
                request.totalAmount(), request.tradeDate());
        boolean individual = request.animalIds() != null && !request.animalIds().isEmpty();
        if (individual == (request.headCount() != null)) {
            throw new BusinessException("Escolha os animais vendidos ou a quantidade de cabeças de um lote por contagem.");
        }

        LivestockTrade trade = newTrade(propertyId, TradeKind.SALE, request.tradeDate(), request.totalAmount(),
                paymentType, request.installments(), request.notes());
        trade.setCustomerId(customer.getId());

        if (individual) {
            List<Long> ids = List.copyOf(new LinkedHashSet<>(request.animalIds()));
            List<Animal> animals = animalRepository.findByPropertyIdAndIdIn(propertyId, ids);
            if (animals.size() != ids.size()) {
                throw new BusinessException("Algum animal selecionado não pertence a esta propriedade.");
            }
            for (Animal animal : animals) {
                if (!animal.isActive()) {
                    throw new BusinessException("O animal " + animal.getTag() + " não está ativo.");
                }
                if (request.tradeDate().isBefore(animal.getEntryDate())) {
                    throw new BusinessException("A venda é anterior à entrada do animal " + animal.getTag() + ".");
                }
            }
            trade.setHeadCount(animals.size());
            tradeRepository.save(trade);
            List<BigDecimal> shares = LivestockSupport.splitAmount(request.totalAmount(), animals.size());
            for (int i = 0; i < animals.size(); i++) {
                Animal animal = animals.get(i);
                animal.setStatus(AnimalStatus.VENDIDO);
                animal.setExitDate(request.tradeDate());
                animal.setExitReason("Venda para " + customer.getName());
                animalRepository.save(animal);
                support.saveEvent(propertyId, AnimalEventType.SAIDA, request.tradeDate(), animal, null, null, null,
                        trade.getId(), shares.get(i), "Venda para " + customer.getName());
                support.closeOpenPregnanciesWithoutBirth(animal.getId());
            }
        } else {
            if (request.batchId() == null) {
                throw new BusinessException("Escolha o lote por contagem de onde saem as cabeças vendidas.");
            }
            AnimalBatch batch = support.requireAggregateBatch(propertyId, request.batchId());
            if (request.headCount() > batch.getHeadCount()) {
                throw new BusinessException("O lote " + batch.getName() + " tem só "
                        + LivestockSupport.plural(batch.getHeadCount(), "cabeça", "cabeças") + ".");
            }
            batch.setHeadCount(batch.getHeadCount() - request.headCount());
            batchRepository.save(batch);
            trade.setHeadCount(request.headCount());
            trade.setBatchId(batch.getId());
            tradeRepository.save(trade);
            support.saveEvent(propertyId, AnimalEventType.SAIDA, request.tradeDate(), null, batch,
                    request.headCount(), null, trade.getId(), request.totalAmount(), "Venda para " + customer.getName());
        }

        if (paymentType == PaymentType.CASH) {
            FinancialTransaction transaction = financialTransactionService.registerFromOperation(propertyId,
                    TransactionType.INCOME, "Venda de animais #" + trade.getId(), request.totalAmount(),
                    request.tradeDate(), null);
            trade.setFinancialTransactionId(transaction.getId());
            tradeRepository.save(trade);
        } else {
            accountReceivableService.createLivestockInstallments(propertyId, customer.getId(), trade.getId(),
                    request.installments());
        }
        auditService.record("LivestockTrade", trade.getId(), AuditAction.CREATE, describe(trade, customer.getName()));
        return toResponses(propertyId, List.of(trade)).get(0);
    }

    /**
     * Cancela a compra/venda: desfaz a entrada ou saída dos animais (ou a contagem do lote) e o financeiro.
     * Compra só se cancela se os animais ainda não tiveram outras movimentações.
     */
    @Transactional
    public TradeResponse cancel(Long id) {
        tenantContext.requireManageAccess();
        Long propertyId = tenantContext.requirePropertyId();
        LivestockTrade trade = findOwned(propertyId, id);
        if (trade.isCanceled()) {
            throw new BusinessException("Esta operação já foi cancelada.");
        }
        boolean purchase = trade.getKind() == TradeKind.PURCHASE;
        if (trade.getPaymentType() == PaymentType.INSTALLMENTS) {
            if (purchase) {
                accountPayableService.removeLivestockInstallments(id);
            } else {
                accountReceivableService.removeLivestockInstallments(id);
            }
        }

        List<AnimalEvent> events = eventRepository.findByTradeId(id);
        if (trade.getBatchId() != null) {
            AnimalBatch batch = support.findBatch(propertyId, trade.getBatchId());
            int heads = purchase ? batch.getHeadCount() - trade.getHeadCount() : batch.getHeadCount() + trade.getHeadCount();
            if (heads < 0) {
                throw new BusinessException("O lote " + batch.getName() + " já não tem as "
                        + trade.getHeadCount() + " cabeças desta compra. Ajuste a contagem antes de cancelar.");
            }
            batch.setHeadCount(heads);
            batchRepository.save(batch);
            eventRepository.deleteAll(events);
        } else {
            List<Animal> animals = animalRepository.findAllById(
                    events.stream().map(AnimalEvent::getAnimalId).filter(Objects::nonNull).toList());
            if (purchase) {
                for (Animal animal : animals) {
                    if (eventRepository.existsByAnimalIdAndTypeNot(animal.getId(), AnimalEventType.ENTRADA)
                            || animalRepository.existsByMotherId(animal.getId())) {
                        throw new BusinessException("O animal " + animal.getTag()
                                + " já tem movimentações ou crias depois da compra. Desfaça-as antes de cancelar.");
                    }
                }
                eventRepository.deleteAll(events);
                eventRepository.flush();
                animalRepository.deleteAll(animals);
            } else {
                Set<String> tags = new HashSet<>();
                for (Animal animal : animals) {
                    support.requireFreeTag(propertyId, animal.getTag(), animal.getId());
                    if (!tags.add(animal.getTag().toLowerCase(Locale.ROOT))) {
                        throw new BusinessException("Brinco repetido entre os animais desta venda: " + animal.getTag());
                    }
                    animal.setStatus(AnimalStatus.ATIVO);
                    animal.setExitDate(null);
                    animal.setExitReason(null);
                    animalRepository.save(animal);
                }
                eventRepository.deleteAll(events);
            }
        }

        Long transactionId = trade.getFinancialTransactionId();
        trade.setFinancialTransactionId(null);
        trade.setCanceledAt(LocalDateTime.now());
        tradeRepository.saveAndFlush(trade);
        if (transactionId != null) {
            financialTransactionRepository.deleteById(transactionId);
        }
        auditService.record("LivestockTrade", trade.getId(), AuditAction.CANCEL, describe(trade, partyName(trade)));
        return toResponses(propertyId, List.of(trade)).get(0);
    }

    private void validateNewAnimals(Long propertyId, List<NewAnimalInput> animals, java.time.LocalDate tradeDate) {
        Set<String> tags = new HashSet<>();
        for (NewAnimalInput input : animals) {
            String tag = input.tag().trim();
            if (!tags.add(tag.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("O brinco " + tag + " aparece mais de uma vez na compra.");
            }
            support.requireFreeTag(propertyId, tag, null);
            if (input.birthDate() != null && input.birthDate().isAfter(tradeDate)) {
                throw new BusinessException("O nascimento do animal " + tag + " é depois da data da compra.");
            }
        }
    }

    private LivestockTrade newTrade(Long propertyId, TradeKind kind, java.time.LocalDate date, BigDecimal total,
            PaymentType paymentType, List<InstallmentInput> installments, String notes) {
        LivestockTrade trade = new LivestockTrade();
        trade.setPropertyId(propertyId);
        trade.setKind(kind);
        trade.setTradeDate(date);
        trade.setTotalAmount(total);
        trade.setPaymentType(paymentType);
        trade.setInstallmentCount(paymentType == PaymentType.INSTALLMENTS ? installments.size() : 0);
        trade.setNotes(AnimalBatchService.blankToNull(notes));
        return trade;
    }

    private LivestockTrade findOwned(Long propertyId, Long id) {
        return tradeRepository.findById(id)
                .filter(trade -> trade.getPropertyId().equals(propertyId))
                .orElseThrow(() -> new ResourceNotFoundException("Operação não encontrada"));
    }

    private String partyName(LivestockTrade trade) {
        return trade.getKind() == TradeKind.PURCHASE
                ? supplierRepository.findById(trade.getSupplierId()).map(Supplier::getName).orElse("")
                : customerRepository.findById(trade.getCustomerId()).map(Customer::getName).orElse("");
    }

    private List<TradeResponse> toResponses(Long propertyId, List<LivestockTrade> trades) {
        Map<Long, String> suppliers = supplierRepository.findAllById(trades.stream().map(LivestockTrade::getSupplierId)
                .filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        Map<Long, String> customers = customerRepository.findAllById(trades.stream().map(LivestockTrade::getCustomerId)
                .filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
        Map<Long, AnimalBatch> batches = support.batchesOf(propertyId);
        return trades.stream().map(t -> {
            boolean purchase = t.getKind() == TradeKind.PURCHASE;
            Long partyId = purchase ? t.getSupplierId() : t.getCustomerId();
            String party = purchase ? suppliers.get(partyId) : customers.get(partyId);
            AnimalBatch batch = t.getBatchId() != null ? batches.get(t.getBatchId()) : null;
            return new TradeResponse(t.getId(), t.getKind(), partyId, party, t.getTradeDate(), t.getHeadCount(),
                    t.getTotalAmount(), t.getPaymentType(), t.getInstallmentCount(), t.getBatchId(),
                    batch != null ? batch.getName() : null, t.getNotes(), t.isCanceled());
        }).toList();
    }

    private static String describe(LivestockTrade trade, String party) {
        boolean purchase = trade.getKind() == TradeKind.PURCHASE;
        return "%s de %s %s %s — %s%s".formatted(purchase ? "compra" : "venda",
                LivestockSupport.plural(trade.getHeadCount(), "cabeça", "cabeças"), purchase ? "de" : "para", party,
                Money.brl(trade.getTotalAmount()),
                trade.getPaymentType() == PaymentType.INSTALLMENTS
                        ? " em " + trade.getInstallmentCount() + " parcela(s)" : " à vista");
    }
}
