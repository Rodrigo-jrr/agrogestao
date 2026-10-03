import { useEffect, useId, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { cropHarvestsApi, inventoryApi, salesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Customer, CropCycle, CropHarvest, CropHarvestDestination, Field, InstallmentInput, PaymentType, Product } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { installmentsSum } from '../../components/InstallmentsEditor'
import { Pagination } from '../../components/Pagination'
import { fieldLabelClass, inputClass, linkDangerClass, primaryButtonClass } from '../../components/ui'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../../lib/format'
import { PartySelect, PaymentFields } from '../livestock/shared'
import { usePaged } from '../../lib/usePaged'
import { cardClass, cropHarvestDestinationLabels } from './shared'

function cycleLabel(cycle: CropCycle) {
  return `${cycle.cropName} · ${cycle.fieldName}`
}

function HarvestForm({ cycles, onSaved }: { cycles: CropCycle[]; onSaved: () => void }) {
  const ids = useId()
  const activeCycles = cycles.filter((c) => !c.archived)
  const [cropCycleId, setCropCycleId] = useState('')
  const [harvestDate, setHarvestDate] = useState(todayIso)
  const [quantity, setQuantity] = useState('')
  const [destination, setDestination] = useState<CropHarvestDestination>('ESTOQUE')
  const [products, setProducts] = useState<Product[]>([])
  const [productId, setProductId] = useState('')
  const [unitCost, setUnitCost] = useState('')
  const [customers, setCustomers] = useState<Customer[]>([])
  const [customerId, setCustomerId] = useState('')
  const [quantityUnit, setQuantityUnit] = useState('')
  const [totalAmount, setTotalAmount] = useState('')
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [installments, setInstallments] = useState<InstallmentInput[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    inventoryApi.listProducts().then(setProducts).catch(() => setProducts([]))
    salesApi.listCustomers().then(setCustomers).catch(() => setCustomers([]))
  }, [])

  const product = products.find((p) => String(p.id) === productId)
  const totalValue = Number(totalAmount) || 0

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (destination === 'VENDA' && paymentType === 'INSTALLMENTS' && installmentsSum(installments) !== totalValue) {
      setError('A soma das parcelas precisa ser igual ao total.')
      return
    }
    setSaving(true)
    try {
      await cropHarvestsApi.create({
        cropCycleId: Number(cropCycleId),
        harvestDate,
        quantity: Number(quantity),
        destination,
        ...(destination === 'ESTOQUE'
          ? { quantityUnit: product?.unit ?? '', productId: Number(productId), unitCost: Number(unitCost) }
          : {
              quantityUnit,
              customerId: Number(customerId),
              totalAmount: totalValue,
              paymentType,
              installments: paymentType === 'INSTALLMENTS' ? installments : undefined,
            }),
      })
      setQuantity('')
      setUnitCost('')
      setQuantityUnit('')
      setTotalAmount('')
      setPaymentType('CASH')
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  if (activeCycles.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-terra/20 bg-terreiro/40 p-4 text-sm text-terra/70">
        <p>Cadastre uma safra ativa para lançar colheitas.</p>
        <Link to="/agricultura?aba=safras" className="mt-2 inline-block font-medium text-rust hover:underline">
          Ir para Safras →
        </Link>
      </div>
    )
  }

  const radio = (checked: boolean) =>
    `flex cursor-pointer items-center justify-center rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
      checked ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
    }`

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage error={error} />
      <div>
        <label htmlFor={`${ids}-cycle`} className={fieldLabelClass}>
          Safra
        </label>
        <select id={`${ids}-cycle`} required value={cropCycleId} onChange={(e) => setCropCycleId(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {activeCycles.map((c) => (
            <option key={c.id} value={c.id}>
              {cycleLabel(c)}
            </option>
          ))}
        </select>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-date`} className={fieldLabelClass}>
            Data da colheita
          </label>
          <input
            id={`${ids}-date`}
            required
            type="date"
            max={todayIso()}
            value={harvestDate}
            onChange={(e) => setHarvestDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-quantity`} className={fieldLabelClass}>
            Quantidade colhida{destination === 'ESTOQUE' && product ? ` (${product.unit})` : ''}
          </label>
          <input
            id={`${ids}-quantity`}
            required
            type="number"
            min="0.001"
            step="0.001"
            inputMode="decimal"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            className={inputClass}
          />
        </div>
      </div>
      <fieldset>
        <legend className={fieldLabelClass}>Destino</legend>
        <div className="grid grid-cols-2 gap-1">
          {(
            [
              ['ESTOQUE', 'Para o estoque'],
              ['VENDA', 'Vendida'],
            ] as [CropHarvestDestination, string][]
          ).map(([value, text]) => (
            <label key={value} className={radio(destination === value)}>
              <input type="radio" name={`${ids}-destination`} checked={destination === value} onChange={() => setDestination(value)} className="sr-only" />
              {text}
            </label>
          ))}
        </div>
      </fieldset>

      {destination === 'ESTOQUE' ? (
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor={`${ids}-product`} className={fieldLabelClass}>
              Produto do estoque
            </label>
            <select id={`${ids}-product`} required value={productId} onChange={(e) => setProductId(e.target.value)} className={inputClass}>
              <option value="">Selecione</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({formatNumber(p.currentQuantity)} {p.unit})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-unit-cost`} className={fieldLabelClass}>
              Custo unitário (R$){product ? ` / ${product.unit}` : ''}
            </label>
            <input
              id={`${ids}-unit-cost`}
              required
              type="number"
              min="0"
              step="0.01"
              inputMode="decimal"
              value={unitCost}
              onChange={(e) => setUnitCost(e.target.value)}
              className={inputClass}
            />
          </div>
          {product && Number(quantity) > 0 && Number(unitCost) > 0 && (
            <p className="text-xs text-terra/60 sm:col-span-2">
              Entra no estoque: {formatNumber(Number(quantity))} {product.unit} (≈ {formatCurrency(Number(quantity) * Number(unitCost))})
            </p>
          )}
        </div>
      ) : (
        <>
          <div className="grid gap-3 sm:grid-cols-2">
            <PartySelect
              label="Cliente"
              parties={customers}
              value={customerId}
              onChange={setCustomerId}
              onCreate={async (name) => {
                const created = await salesApi.createCustomer({ name })
                setCustomers((list) => [...list, created])
                return created
              }}
            />
            <div>
              <label htmlFor={`${ids}-unit`} className={fieldLabelClass}>
                Unidade da quantidade
              </label>
              <input id={`${ids}-unit`} required maxLength={20} placeholder="sc, kg, ton" value={quantityUnit} onChange={(e) => setQuantityUnit(e.target.value)} className={inputClass} />
            </div>
          </div>
          <div>
            <label htmlFor={`${ids}-total`} className={fieldLabelClass}>
              Valor total (R$)
            </label>
            <input
              id={`${ids}-total`}
              required
              type="number"
              min="0.01"
              step="0.01"
              inputMode="decimal"
              value={totalAmount}
              onChange={(e) => setTotalAmount(e.target.value)}
              className={inputClass}
            />
          </div>
          <PaymentFields
            kind="sale"
            total={totalValue}
            date={harvestDate}
            paymentType={paymentType}
            onPaymentType={setPaymentType}
            installments={installments}
            onInstallments={setInstallments}
          />
        </>
      )}

      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Salvando…' : 'Registrar colheita'}
      </button>
    </form>
  )
}

export function CropHarvestsTab({
  cycles,
  fields,
  canWrite,
  canManage,
  onChanged,
}: {
  cycles: CropCycle[]
  fields: Field[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)
  const [cropCycleId, setCropCycleId] = useState('')
  const [fieldId, setFieldId] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const paged = usePaged(
    (page) =>
      cropHarvestsApi.list({
        page,
        cropCycleId: cropCycleId ? Number(cropCycleId) : undefined,
        fieldId: fieldId ? Number(fieldId) : undefined,
        from: from || undefined,
        to: to || undefined,
      }),
    [cropCycleId, fieldId, from, to],
  )

  const cycleOptions = useMemo(() => cycles.map((c) => [c.id, cycleLabel(c)] as const), [cycles])

  function refresh() {
    paged.reload()
    onChanged()
  }

  async function handleDelete(harvest: CropHarvest) {
    const message =
      harvest.destination === 'ESTOQUE'
        ? `${harvest.productName} sai do estoque pela mesma quantidade (${formatNumber(harvest.quantity)} ${harvest.quantityUnit}) e o custo médio é recalculado.`
        : harvest.paymentType === 'INSTALLMENTS'
          ? 'As parcelas são canceladas do financeiro. Só é possível se nenhuma ainda foi recebida.'
          : 'O recebimento sai do financeiro (caixa).'
    const ok = await confirm({
      title: `Excluir colheita de ${formatDate(harvest.harvestDate)}?`,
      message,
      confirmLabel: 'Excluir colheita',
    })
    if (!ok) return
    setError(null)
    try {
      await cropHarvestsApi.remove(harvest.id)
      refresh()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova colheita</h2>
          <HarvestForm cycles={cycles} onSaved={refresh} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Colheitas</h2>
          <div className="flex flex-wrap items-center gap-2">
            <select
              aria-label="Filtrar por safra"
              value={cropCycleId}
              onChange={(e) => setCropCycleId(e.target.value)}
              className={`${inputClass} w-auto`}
            >
              <option value="">Todas as safras</option>
              {cycleOptions.map(([id, label]) => (
                <option key={id} value={id}>
                  {label}
                </option>
              ))}
            </select>
            <select aria-label="Filtrar por talhão" value={fieldId} onChange={(e) => setFieldId(e.target.value)} className={`${inputClass} w-auto`}>
              <option value="">Todos os talhões</option>
              {fields.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </select>
            <label className="flex items-center gap-1 text-xs text-terra/60">
              De
              <input aria-label="Data inicial" type="date" value={from} onChange={(e) => setFrom(e.target.value)} className={`${inputClass} w-auto`} />
            </label>
            <label className="flex items-center gap-1 text-xs text-terra/60">
              até
              <input aria-label="Data final" type="date" value={to} onChange={(e) => setTo(e.target.value)} className={`${inputClass} w-auto`} />
            </label>
          </div>
        </div>
        <FormMessage error={error ?? paged.error} />
        <ul className="divide-y divide-terra/5">
          {paged.items.map((h) => (
            <li key={h.id} className="py-3 text-sm">
              <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <p className="font-medium text-terra">
                  {cropHarvestDestinationLabels[h.destination]} · {h.cropName} · {h.fieldName}
                </p>
                <p className="text-xs text-terra/55 tabular-nums">{formatDate(h.harvestDate)}</p>
              </div>
              <p className="text-xs text-terra/60">
                {formatNumber(h.quantity)} {h.quantityUnit}
                {h.destination === 'ESTOQUE'
                  ? h.productName && ` · ${h.productName} · ${formatCurrency(h.unitCost ?? 0)}/${h.quantityUnit}`
                  : h.customerName &&
                    ` · ${h.customerName} · ${formatCurrency(h.totalAmount ?? 0)} · ${h.paymentType === 'INSTALLMENTS' ? 'a prazo' : 'à vista'}`}
              </p>
              {canManage && (
                <button onClick={() => handleDelete(h)} className={`mt-1 cursor-pointer text-xs ${linkDangerClass}`}>
                  Excluir
                </button>
              )}
            </li>
          ))}
        </ul>
        {paged.loading && paged.items.length === 0 && <p className="py-6 text-center text-sm text-terra/50">Carregando…</p>}
        {!paged.loading && !paged.error && paged.items.length === 0 && (
          <p className="py-6 text-center text-sm text-terra/50">Nenhuma colheita registrada ainda.</p>
        )}
        <Pagination page={paged.page} totalPages={paged.totalPages} totalElements={paged.totalElements} onChange={paged.setPage} />
      </section>
    </div>
  )
}
