import { useEffect, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { cropOperationsApi, inventoryApi, machinesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { CropCycle, CropOperation, CropOperationKind, Field, Machine, Product } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { Pagination } from '../../components/Pagination'
import { fieldLabelClass, inputClass, linkDangerClass, primaryButtonClass } from '../../components/ui'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../../lib/format'
import { usePaged } from '../../lib/usePaged'
import { cardClass, cropOperationKindLabels } from './shared'

type Source = 'stock' | 'manual' | 'none'
type MachineMode = 'texto' | 'cadastrada'

const kinds = Object.keys(cropOperationKindLabels) as CropOperationKind[]

function cycleLabel(cycle: CropCycle) {
  return `${cycle.cropName} · ${cycle.fieldName}`
}

function OperationForm({ cycles, onSaved }: { cycles: CropCycle[]; onSaved: () => void }) {
  const ids = useId()
  const activeCycles = cycles.filter((c) => !c.archived)
  const [cropCycleId, setCropCycleId] = useState('')
  const [kind, setKind] = useState<CropOperationKind>('PREPARO')
  const [operationDate, setOperationDate] = useState(todayIso)
  const [source, setSource] = useState<Source>('none')
  const [products, setProducts] = useState<Product[]>([])
  const [productId, setProductId] = useState('')
  const [productName, setProductName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [quantityUnit, setQuantityUnit] = useState('')
  const [cost, setCost] = useState('')
  const [responsible, setResponsible] = useState('')
  const [machineMode, setMachineMode] = useState<MachineMode>('texto')
  const [machine, setMachine] = useState('')
  const [machines, setMachines] = useState<Machine[]>([])
  const [machineId, setMachineId] = useState('')
  const [machineReading, setMachineReading] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    inventoryApi.listProducts().then(setProducts).catch(() => setProducts([]))
    machinesApi.list().then(setMachines).catch(() => setMachines([]))
  }, [])

  const product = products.find((p) => String(p.id) === productId)
  const stockQuantity = Number(quantity) > 0 ? Number(quantity) : 0
  const selectedMachine = machines.find((m) => String(m.id) === machineId)

  function resetFields() {
    setQuantity('')
    setProductName('')
    setQuantityUnit('')
    setCost('')
    setResponsible('')
    setMachine('')
    setMachineReading('')
    setNotes('')
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (machineMode === 'cadastrada' && selectedMachine && machineReading && Number(machineReading) < selectedMachine.currentReading) {
      setError(`A leitura não pode ser menor que a atual (${formatNumber(selectedMachine.currentReading)}).`)
      return
    }
    setSaving(true)
    try {
      await cropOperationsApi.create({
        cropCycleId: Number(cropCycleId),
        kind,
        operationDate,
        ...(source === 'stock'
          ? { productId: Number(productId), quantity: Number(quantity) }
          : source === 'manual'
            ? {
                productName: productName || null,
                quantity: quantity ? Number(quantity) : null,
                quantityUnit: quantityUnit || null,
                cost: cost ? Number(cost) : null,
              }
            : {}),
        responsible: responsible || null,
        ...(machineMode === 'cadastrada'
          ? machineId
            ? { machineId: Number(machineId), machineReading: machineReading ? Number(machineReading) : null }
            : {}
          : { machine: machine || null }),
        notes: notes || null,
      })
      resetFields()
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
        <p>Cadastre uma safra ativa para lançar operações.</p>
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
      <fieldset>
        <legend className={fieldLabelClass}>Tipo de operação</legend>
        <div className="grid grid-cols-2 gap-1">
          {kinds.map((k) => (
            <label key={k} className={radio(kind === k)}>
              <input type="radio" name={`${ids}-kind`} checked={kind === k} onChange={() => setKind(k)} className="sr-only" />
              {cropOperationKindLabels[k]}
            </label>
          ))}
        </div>
      </fieldset>
      <div>
        <label htmlFor={`${ids}-date`} className={fieldLabelClass}>
          Data
        </label>
        <input
          id={`${ids}-date`}
          required
          type="date"
          max={todayIso()}
          value={operationDate}
          onChange={(e) => setOperationDate(e.target.value)}
          className={inputClass}
        />
      </div>
      <fieldset>
        <legend className={fieldLabelClass}>Insumo</legend>
        <div className="grid grid-cols-3 gap-1">
          {(
            [
              ['stock', 'Do estoque'],
              ['manual', 'Outro'],
              ['none', 'Sem insumo'],
            ] as [Source, string][]
          ).map(([value, text]) => (
            <label key={value} className={radio(source === value)}>
              <input type="radio" name={`${ids}-source`} checked={source === value} onChange={() => setSource(value)} className="sr-only" />
              {text}
            </label>
          ))}
        </div>
      </fieldset>

      {source === 'stock' && (
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
            <label htmlFor={`${ids}-quantity`} className={fieldLabelClass}>
              Quantidade usada{product ? ` (${product.unit})` : ''}
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
          {product && stockQuantity > 0 && (
            <p className={`text-xs sm:col-span-2 ${stockQuantity > product.currentQuantity ? 'text-rust' : 'text-terra/60'}`}>
              Sai do estoque: {formatNumber(stockQuantity)} {product.unit} (≈ {formatCurrency(stockQuantity * product.averageCost)})
              {stockQuantity > product.currentQuantity && ` — só há ${formatNumber(product.currentQuantity)} ${product.unit}`}
            </p>
          )}
        </div>
      )}

      {source === 'manual' && (
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="sm:col-span-3">
            <label htmlFor={`${ids}-name`} className={fieldLabelClass}>
              Insumo usado <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input
              id={`${ids}-name`}
              maxLength={150}
              placeholder="Ex.: Calcário, Ureia"
              value={productName}
              onChange={(e) => setProductName(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor={`${ids}-manual-qty`} className={fieldLabelClass}>
              Quantidade <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input
              id={`${ids}-manual-qty`}
              type="number"
              min="0.001"
              step="0.001"
              inputMode="decimal"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor={`${ids}-unit`} className={fieldLabelClass}>
              Unidade
            </label>
            <input id={`${ids}-unit`} maxLength={20} placeholder="kg, L, sc" value={quantityUnit} onChange={(e) => setQuantityUnit(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor={`${ids}-cost`} className={fieldLabelClass}>
              Custo total (R$) <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-cost`} type="number" min="0" step="0.01" inputMode="decimal" value={cost} onChange={(e) => setCost(e.target.value)} className={inputClass} />
          </div>
        </div>
      )}

      <div>
        <label htmlFor={`${ids}-resp`} className={fieldLabelClass}>
          Responsável <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input id={`${ids}-resp`} maxLength={100} placeholder="Quem executou" value={responsible} onChange={(e) => setResponsible(e.target.value)} className={inputClass} />
      </div>
      <fieldset>
        <legend className={fieldLabelClass}>
          Máquina <span className="font-normal text-terra/45">(opcional)</span>
        </legend>
        <div className="grid grid-cols-2 gap-1">
          {(
            [
              ['texto', 'Texto livre'],
              ['cadastrada', 'Máquina cadastrada'],
            ] as [MachineMode, string][]
          ).map(([value, text]) => (
            <label key={value} className={radio(machineMode === value)}>
              <input type="radio" name={`${ids}-machine-mode`} checked={machineMode === value} onChange={() => setMachineMode(value)} className="sr-only" />
              {text}
            </label>
          ))}
        </div>
      </fieldset>
      {machineMode === 'texto' ? (
        <div>
          <label htmlFor={`${ids}-machine`} className={fieldLabelClass}>
            Qual máquina <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-machine`} maxLength={150} placeholder="Ex.: Trator John Deere" value={machine} onChange={(e) => setMachine(e.target.value)} className={inputClass} />
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor={`${ids}-machine-select`} className={fieldLabelClass}>
              Máquina
            </label>
            <select id={`${ids}-machine-select`} value={machineId} onChange={(e) => setMachineId(e.target.value)} className={inputClass}>
              <option value="">Selecione</option>
              {machines
                .filter((m) => !m.archived)
                .map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-machine-reading`} className={fieldLabelClass}>
              Leitura atual <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input
              id={`${ids}-machine-reading`}
              type="number"
              step="0.01"
              inputMode="decimal"
              disabled={!machineId}
              value={machineReading}
              onChange={(e) => setMachineReading(e.target.value)}
              className={`${inputClass} disabled:bg-terra/5`}
            />
            {selectedMachine && (
              <p className="mt-1 text-xs text-terra/55">Leitura hoje: {formatNumber(selectedMachine.currentReading)}.</p>
            )}
          </div>
        </div>
      )}
      <div>
        <label htmlFor={`${ids}-notes`} className={fieldLabelClass}>
          Observações <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input id={`${ids}-notes`} maxLength={500} placeholder="Dose, condição do solo, clima…" value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
      </div>
      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Salvando…' : 'Registrar operação'}
      </button>
    </form>
  )
}

export function CropOperationsTab({
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
      cropOperationsApi.list({
        page,
        cropCycleId: cropCycleId ? Number(cropCycleId) : undefined,
        fieldId: fieldId ? Number(fieldId) : undefined,
        from: from || undefined,
        to: to || undefined,
      }),
    [cropCycleId, fieldId, from, to],
  )

  function refresh() {
    paged.reload()
    onChanged()
  }

  async function handleDelete(op: CropOperation) {
    const ok = await confirm({
      title: `Excluir operação de ${cropOperationKindLabels[op.kind]} de ${formatDate(op.operationDate)}?`,
      message: op.productId
        ? `${op.productName} volta ao estoque (${formatNumber(op.quantity ?? 0)} ${op.quantityUnit ?? ''}).`
        : 'Essa operação não usou insumo do estoque.',
      confirmLabel: 'Excluir operação',
    })
    if (!ok) return
    setError(null)
    try {
      await cropOperationsApi.remove(op.id)
      refresh()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova operação</h2>
          <OperationForm cycles={cycles} onSaved={refresh} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Operações</h2>
          <div className="flex flex-wrap items-center gap-2">
            <select
              aria-label="Filtrar por safra"
              value={cropCycleId}
              onChange={(e) => setCropCycleId(e.target.value)}
              className={`${inputClass} w-auto`}
            >
              <option value="">Todas as safras</option>
              {cycles.map((c) => (
                <option key={c.id} value={c.id}>
                  {cycleLabel(c)}
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
          {paged.items.map((op) => (
            <li key={op.id} className="py-3 text-sm">
              <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <p className="font-medium text-terra">
                  {cropOperationKindLabels[op.kind]} · {op.cropName} · {op.fieldName}
                </p>
                <p className="text-xs text-terra/55 tabular-nums">{formatDate(op.operationDate)}</p>
              </div>
              <p className="text-xs text-terra/60">
                {op.productName && `${op.productName}${op.quantity != null ? ` · ${formatNumber(op.quantity)} ${op.quantityUnit ?? ''}` : ''}`}
                {op.totalCost != null && ` · ${formatCurrency(op.totalCost)}`}
                {(op.machineName ?? op.machine) && ` · ${op.machineName ?? op.machine}`}
                {op.responsible && ` · ${op.responsible}`}
              </p>
              {op.notes && <p className="text-xs text-terra/55">{op.notes}</p>}
              {canManage && (
                <button onClick={() => handleDelete(op)} className={`mt-1 cursor-pointer text-xs ${linkDangerClass}`}>
                  Excluir
                </button>
              )}
            </li>
          ))}
        </ul>
        {paged.loading && paged.items.length === 0 && <p className="py-6 text-center text-sm text-terra/50">Carregando…</p>}
        {!paged.loading && !paged.error && paged.items.length === 0 && (
          <p className="py-6 text-center text-sm text-terra/50">Nenhuma operação registrada ainda.</p>
        )}
        <Pagination page={paged.page} totalPages={paged.totalPages} totalElements={paged.totalElements} onChange={paged.setPage} />
      </section>
    </div>
  )
}
