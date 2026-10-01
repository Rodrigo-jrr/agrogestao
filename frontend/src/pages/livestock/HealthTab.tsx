import { useEffect, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { inventoryApi, livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Animal, AnimalBatch, HealthApplication, HealthKind, LivestockAlerts, Product } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { Pagination } from '../../components/Pagination'
import { inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../../lib/format'
import { healthKindLabels, healthTitle, relativeDay } from '../../lib/livestock'
import { usePaged } from '../../lib/usePaged'
import { AnimalPicker, cardClass, labelClass } from './shared'
import { useSelection } from './useSelection'

type Target = 'batch' | 'animals'
type Source = 'stock' | 'other'

/** Alertas: próximos passos sanitários (até 7 dias e atrasados) e quem está em carência. */
export function HealthAgenda({
  alerts,
  canWrite,
  onFollowUp,
  onDismiss,
}: {
  alerts: LivestockAlerts | null
  canWrite: boolean
  onFollowUp?: (application: HealthApplication) => void
  onDismiss?: (application: HealthApplication) => void
}) {
  const today = todayIso()
  if (!alerts) return <p className="text-sm text-terra/50">Carregando…</p>
  if (
    alerts.followUps.length === 0 &&
    alerts.withdrawals.length === 0 &&
    alerts.upcomingCalvings.length === 0 &&
    alerts.pendingDiagnoses.length === 0
  ) {
    return (
      <p className="text-sm text-terra/50">
        Nada pendente: nenhum reforço nos próximos 7 dias, nenhum animal em carência, nenhum parto previsto e nenhum diagnóstico de prenhez pendente.
      </p>
    )
  }
  return (
    <div className="space-y-4">
      {alerts.followUps.length > 0 && (
        <ul className="space-y-2">
          {alerts.followUps.map((a) => {
            const overdue = a.nextDate! < today
            return (
              <li key={a.id} className={`rounded-lg px-3 py-2 ${overdue ? 'bg-rust/10' : 'bg-terreiro'}`}>
                <p className="text-sm font-medium text-terra">
                  {a.nextDescription ?? `Próxima dose de ${a.productName}`} — {a.target}
                </p>
                <p className={`text-xs ${overdue ? 'font-medium text-rust' : 'text-terra/60'}`}>
                  {formatDate(a.nextDate!)} ({overdue ? `atrasado, ${relativeDay(a.nextDate!, today)}` : relativeDay(a.nextDate!, today)}) ·{' '}
                  {healthTitle(a.kind, a.productName)} em {formatDate(a.date)}
                </p>
                {canWrite && (onFollowUp || onDismiss) && (
                  <div className="mt-1 flex gap-4 text-sm">
                    {onFollowUp && (
                      <button onClick={() => onFollowUp(a)} className={`cursor-pointer ${linkNeutralClass}`}>
                        Registrar agora
                      </button>
                    )}
                    {onDismiss && (
                      <button onClick={() => onDismiss(a)} className={`cursor-pointer ${linkNeutralClass}`}>
                        Dispensar
                      </button>
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
      {alerts.withdrawals.length > 0 && (
        <div>
          <p className="mb-1 text-xs font-medium text-terra/70">Em carência (não vão para abate nem ordenha comercial)</p>
          <ul className="flex flex-wrap gap-2">
            {alerts.withdrawals.map((w) => (
              <li key={`${w.animalId ?? 'b'}-${w.batchId ?? 'a'}`} className="rounded-full bg-trigo/20 px-3 py-1 text-xs text-trigo-dark">
                {w.animalId ? (
                  <Link to={`/pecuaria/animais/${w.animalId}`} className="font-medium hover:underline">
                    {w.label}
                  </Link>
                ) : (
                  <span className="font-medium">{w.label}</span>
                )}{' '}
                até {formatDate(w.until)}
              </li>
            ))}
          </ul>
        </div>
      )}
      {alerts.upcomingCalvings.length > 0 && (
        <div>
          <p className="mb-1 text-xs font-medium text-terra/70">Partos previstos</p>
          <ul className="flex flex-wrap gap-2">
            {alerts.upcomingCalvings.map((c) => {
              const overdue = c.expectedCalvingDate < today
              return (
                <li
                  key={c.animalId}
                  className={`rounded-full px-3 py-1 text-xs ${overdue ? 'bg-rust/10 text-rust' : 'bg-mata/15 text-mata'}`}
                >
                  <Link to={`/pecuaria/animais/${c.animalId}`} className="font-medium hover:underline">
                    {c.animalTag}
                  </Link>{' '}
                  em {formatDate(c.expectedCalvingDate)} ({overdue ? 'atrasado' : relativeDay(c.expectedCalvingDate, today)})
                </li>
              )
            })}
          </ul>
        </div>
      )}
      {alerts.pendingDiagnoses.length > 0 && (
        <div>
          <p className="mb-1 text-xs font-medium text-terra/70">Diagnóstico de prenhez pendente</p>
          <ul className="flex flex-wrap gap-2">
            {alerts.pendingDiagnoses.map((d) => (
              <li key={d.animalId} className="rounded-full bg-trigo/20 px-3 py-1 text-xs text-trigo-dark">
                <Link to={`/pecuaria/animais/${d.animalId}`} className="font-medium hover:underline">
                  {d.animalTag}
                </Link>{' '}
                desde {formatDate(d.since)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function HealthForm({
  batches,
  followUp,
  onSaved,
  onCancelFollowUp,
}: {
  batches: AnimalBatch[]
  followUp: HealthApplication | null
  onSaved: () => void
  onCancelFollowUp: () => void
}) {
  const ids = useId()
  const [kind, setKind] = useState<HealthKind>('VACINA')
  const [date, setDate] = useState(todayIso)
  const [target, setTarget] = useState<Target>('batch')
  const [batchId, setBatchId] = useState('')
  const [active, setActive] = useState<Animal[]>([])
  const selection = useSelection()
  const [products, setProducts] = useState<Product[]>([])
  const [source, setSource] = useState<Source>('stock')
  const [productId, setProductId] = useState('')
  const [productName, setProductName] = useState('')
  const [dose, setDose] = useState('')
  const [doseUnit, setDoseUnit] = useState('')
  const [cost, setCost] = useState('')
  const [withdrawalDays, setWithdrawalDays] = useState('')
  const [nextDate, setNextDate] = useState('')
  const [nextDescription, setNextDescription] = useState('')
  const [responsible, setResponsible] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})
  const [version, setVersion] = useState(0)

  useEffect(() => {
    inventoryApi.listProducts().then(setProducts).catch(() => setProducts([]))
    livestockApi
      .listAnimals({ status: 'ATIVO', size: 200 })
      .then((page) => setActive(page.content))
      .catch(() => setActive([]))
  }, [version])

  // "Registrar agora" de um alerta: repete produto, alvo e tipo da aplicação anterior.
  useEffect(() => {
    if (!followUp) return
    setKind(followUp.kind)
    setResult({})
    if (followUp.batchId) {
      setTarget('batch')
      setBatchId(String(followUp.batchId))
    } else {
      setTarget('animals')
      selection.clear()
      followUp.animals.forEach((a) => selection.toggle(a.id))
    }
    if (followUp.productId) {
      setSource('stock')
      setProductId(String(followUp.productId))
    } else {
      setSource('other')
      setProductName(followUp.productName)
      setDoseUnit(followUp.doseUnit ?? '')
    }
    setDose(followUp.dose != null ? String(followUp.dose) : '')
    setWithdrawalDays(followUp.withdrawalDays != null ? String(followUp.withdrawalDays) : '')
    setNextDate('')
    setNextDescription('')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [followUp])

  const product = products.find((p) => String(p.id) === productId)
  const batch = batches.find((b) => String(b.id) === batchId)
  const heads = target === 'batch' ? (batch?.headCount ?? 0) : selection.chosen.size
  const estimatedQty = Number(dose) > 0 ? Math.round(Number(dose) * heads * 1000) / 1000 : 0

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    if (target === 'animals' && selection.chosen.size === 0) {
      setResult({ error: 'Marque os animais que receberam a aplicação.' })
      return
    }
    setSaving(true)
    try {
      const applied = await livestockApi.applyHealth({
        kind,
        date,
        ...(target === 'batch' ? { batchId: Number(batchId) } : { animalIds: [...selection.chosen] }),
        ...(source === 'stock'
          ? { productId: Number(productId), dose: Number(dose) }
          : {
              productName,
              dose: dose ? Number(dose) : null,
              doseUnit: doseUnit || null,
              cost: cost ? Number(cost) : null,
            }),
        withdrawalDays: withdrawalDays ? Number(withdrawalDays) : null,
        nextDate: nextDate || null,
        nextDescription: nextDescription || null,
        responsible: responsible || null,
        notes: notes || null,
        followUpOfId: followUp?.id ?? null,
      })
      setResult({
        success: `${healthKindLabels[applied.kind]} registrada para ${applied.target}${
          applied.totalCost != null ? ` (${formatCurrency(applied.totalCost)})` : ''
        }.`,
      })
      selection.clear()
      setDose('')
      setCost('')
      setNextDate('')
      setNextDescription('')
      setNotes('')
      setVersion((v) => v + 1)
      onSaved()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  const radio = (checked: boolean) =>
    `flex cursor-pointer items-center justify-center rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
      checked ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
    }`

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {followUp && (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-terreiro px-3 py-2 text-sm text-terra">
          <span>
            Continuação de {healthTitle(followUp.kind, followUp.productName)} ({formatDate(followUp.date)}): o
            alerta sai da agenda ao salvar.
          </span>
          <button type="button" onClick={onCancelFollowUp} className={`cursor-pointer text-xs ${linkNeutralClass}`}>
            Não é continuação
          </button>
        </div>
      )}
      <FormMessage {...result} />
      <fieldset>
        <legend className={labelClass}>Tipo</legend>
        <div className="grid grid-cols-3 gap-1">
          {(Object.keys(healthKindLabels) as HealthKind[]).map((k) => (
            <label key={k} className={radio(kind === k)}>
              <input type="radio" name={`${ids}-kind`} checked={kind === k} onChange={() => setKind(k)} className="sr-only" />
              {healthKindLabels[k]}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            Data
          </label>
          <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </div>
        <fieldset>
          <legend className={labelClass}>Em quem</legend>
          <div className="grid grid-cols-2 gap-1">
            {(
              [
                ['batch', 'Lote inteiro'],
                ['animals', 'Animais'],
              ] as [Target, string][]
            ).map(([value, text]) => (
              <label key={value} className={radio(target === value)}>
                <input type="radio" name={`${ids}-target`} checked={target === value} onChange={() => setTarget(value)} className="sr-only" />
                {text}
              </label>
            ))}
          </div>
        </fieldset>
      </div>

      {target === 'batch' ? (
        <div>
          <label htmlFor={`${ids}-batch`} className={labelClass}>
            Lote
          </label>
          <select id={`${ids}-batch`} required value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
            <option value="">Selecione</option>
            {batches
              .filter((b) => !b.archived && b.headCount > 0)
              .map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.speciesName.toLowerCase()}, {formatNumber(b.headCount)} cab.)
                </option>
              ))}
          </select>
        </div>
      ) : (
        <AnimalPicker legend="Animais" animals={active} chosen={selection.chosen} onToggle={selection.toggle} />
      )}

      <fieldset>
        <legend className={labelClass}>Produto</legend>
        <div className="grid grid-cols-2 gap-1">
          {(
            [
              ['stock', 'Do estoque'],
              ['other', 'Outro (sem estoque)'],
            ] as [Source, string][]
          ).map(([value, text]) => (
            <label key={value} className={radio(source === value)}>
              <input type="radio" name={`${ids}-source`} checked={source === value} onChange={() => setSource(value)} className="sr-only" />
              {text}
            </label>
          ))}
        </div>
      </fieldset>

      {source === 'stock' ? (
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor={`${ids}-product`} className={labelClass}>
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
            <label htmlFor={`${ids}-dose`} className={labelClass}>
              Dose por cabeça{product ? ` (${product.unit})` : ''}
            </label>
            <input id={`${ids}-dose`} required type="number" min="0.001" step="0.001" inputMode="decimal" value={dose} onChange={(e) => setDose(e.target.value)} className={inputClass} />
          </div>
          {product && estimatedQty > 0 && (
            <p className={`text-xs sm:col-span-2 ${estimatedQty > product.currentQuantity ? 'text-rust' : 'text-terra/60'}`}>
              Sai do estoque: {formatNumber(estimatedQty)} {product.unit} (≈ {formatCurrency(estimatedQty * product.averageCost)})
              {estimatedQty > product.currentQuantity && ` — só há ${formatNumber(product.currentQuantity)} ${product.unit}`}
            </p>
          )}
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="sm:col-span-3">
            <label htmlFor={`${ids}-name`} className={labelClass}>
              O que foi aplicado / feito
            </label>
            <input
              id={`${ids}-name`}
              required
              maxLength={150}
              placeholder="Ex.: Vacina contra raiva, consulta veterinária"
              value={productName}
              onChange={(e) => setProductName(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor={`${ids}-dose2`} className={labelClass}>
              Dose por cabeça <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-dose2`} type="number" min="0.001" step="0.001" inputMode="decimal" value={dose} onChange={(e) => setDose(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor={`${ids}-unit`} className={labelClass}>
              Unidade
            </label>
            <input id={`${ids}-unit`} maxLength={20} placeholder="mL, dose" value={doseUnit} onChange={(e) => setDoseUnit(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor={`${ids}-cost`} className={labelClass}>
              Custo total (R$) <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-cost`} type="number" min="0" step="0.01" inputMode="decimal" value={cost} onChange={(e) => setCost(e.target.value)} className={inputClass} />
          </div>
          <p className="text-xs text-terra/55 sm:col-span-3">
            O custo daqui entra no custo dos animais; o pagamento em si é lançado no Financeiro.
          </p>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <label htmlFor={`${ids}-withdrawal`} className={labelClass}>
            Carência (dias)
          </label>
          <input
            id={`${ids}-withdrawal`}
            type="number"
            min="0"
            max="365"
            step="1"
            placeholder="0"
            value={withdrawalDays}
            onChange={(e) => setWithdrawalDays(e.target.value)}
            aria-describedby={`${ids}-withdrawal-help`}
            className={inputClass}
          />
          <p id={`${ids}-withdrawal-help`} className="mt-1 text-xs text-terra/55">
            Dias sem abate nem ordenha comercial.
          </p>
        </div>
        <div>
          <label htmlFor={`${ids}-next`} className={labelClass}>
            Próxima data <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-next`} type="date" min={date} value={nextDate} onChange={(e) => setNextDate(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor={`${ids}-next-desc`} className={labelClass}>
            O que fazer nela
          </label>
          <input
            id={`${ids}-next-desc`}
            maxLength={255}
            placeholder="Ex.: reforço, fim do tratamento"
            value={nextDescription}
            onChange={(e) => setNextDescription(e.target.value)}
            disabled={!nextDate}
            className={`${inputClass} disabled:bg-terra/5`}
          />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-resp`} className={labelClass}>
            Responsável <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-resp`} maxLength={100} placeholder="Quem aplicou ou o veterinário" value={responsible} onChange={(e) => setResponsible(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor={`${ids}-notes`} className={labelClass}>
            Observações <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-notes`} maxLength={500} placeholder="Diagnóstico, lote da vacina…" value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
        </div>
      </div>
      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Registrando…' : `Registrar ${healthKindLabels[kind].toLowerCase()}`}
      </button>
    </form>
  )
}

export function HealthTab({
  batches,
  canWrite,
  canManage,
  onChanged,
}: {
  batches: AnimalBatch[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [alerts, setAlerts] = useState<LivestockAlerts | null>(null)
  const [followUp, setFollowUp] = useState<HealthApplication | null>(null)
  const [error, setError] = useState<string | null>(null)
  const history = usePaged((page) => livestockApi.listHealth({ page }), [])

  function loadAlerts() {
    livestockApi.alerts().then(setAlerts).catch((err) => setError(apiErrorMessage(err)))
  }
  useEffect(loadAlerts, [])

  function refresh() {
    loadAlerts()
    history.reload()
    onChanged()
  }

  async function dismiss(application: HealthApplication) {
    setError(null)
    try {
      await livestockApi.dismissFollowUp(application.id)
      loadAlerts()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function undo(application: HealthApplication) {
    const ok = await confirm({
      title: `Desfazer ${healthKindLabels[application.kind].toLowerCase()} de ${formatDate(application.date)}?`,
      message: `${application.productName} em ${application.target} sai da linha do tempo${
        application.productId ? ' e o produto volta ao estoque' : ''
      }. Use para lançamentos errados.`,
      confirmLabel: 'Desfazer aplicação',
    })
    if (!ok) return
    setError(null)
    try {
      await livestockApi.undoHealth(application.id)
      refresh()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-5">
      <div className="min-w-0 space-y-4 lg:col-span-2">
        <section className={cardClass}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Agenda</h2>
          <FormMessage error={error} />
          <HealthAgenda
            alerts={alerts}
            canWrite={canWrite}
            onFollowUp={(application) => {
              setFollowUp(application)
              document.getElementById('registrar-sanidade')?.scrollIntoView({ behavior: 'smooth' })
            }}
            onDismiss={dismiss}
          />
        </section>
        {canWrite && (
          <section id="registrar-sanidade" className={cardClass}>
            <h2 className="mb-3 text-sm font-semibold text-terra">Registrar vacina, medicamento ou procedimento</h2>
            <HealthForm
              batches={batches}
              followUp={followUp}
              onCancelFollowUp={() => setFollowUp(null)}
              onSaved={() => {
                setFollowUp(null)
                refresh()
              }}
            />
          </section>
        )}
      </div>

      <section className={`${cardClass} min-w-0 lg:col-span-3`}>
        <h2 className="mb-3 text-sm font-semibold text-terra">Histórico sanitário</h2>
        <FormMessage error={history.error} />
        <ul className="divide-y divide-terra/5">
          {history.items.map((a) => (
            <li key={a.id} className="py-3 text-sm">
              <div className="flex flex-wrap items-baseline justify-between gap-x-3">
                <p className="text-terra">
                  <span className="font-medium">{healthTitle(a.kind, a.productName, true)}</span>
                  {a.dose != null && (
                    <span className="text-terra/60">
                      {' '}
                      · {formatNumber(a.dose)} {a.doseUnit ?? ''}/cab.
                    </span>
                  )}
                </p>
                <p className="text-xs text-terra/55 tabular-nums">{formatDate(a.date)}</p>
              </div>
              <p className="text-xs text-terra/60">
                {a.batchId ? a.target : a.animals.length <= 5 ? a.animals.map((x) => x.tag).join(', ') : a.target}
                {a.totalCost != null && ` · ${formatCurrency(a.totalCost)}`}
                {a.withdrawalUntil && ` · carência até ${formatDate(a.withdrawalUntil)}`}
                {a.nextDate && ` · ${a.nextDescription ?? 'próxima'} em ${formatDate(a.nextDate)}${a.followUpDone ? ' (feito)' : ''}`}
                {a.responsible && ` · ${a.responsible}`}
              </p>
              {canManage && (
                <button
                  onClick={() => undo(a)}
                  aria-label={`Desfazer ${healthKindLabels[a.kind].toLowerCase()} ${a.productName} de ${formatDate(a.date)}`}
                  className={`mt-1 cursor-pointer text-xs ${linkDangerClass}`}
                >
                  Desfazer
                </button>
              )}
            </li>
          ))}
        </ul>
        {history.loading && history.items.length === 0 && <p className="py-6 text-center text-sm text-terra/50">Carregando…</p>}
        {!history.loading && !history.error && history.items.length === 0 && (
          <p className="py-6 text-center text-sm text-terra/50">Nenhuma vacina, medicamento ou procedimento registrado ainda.</p>
        )}
        <Pagination page={history.page} totalPages={history.totalPages} totalElements={history.totalElements} onChange={history.setPage} />

      </section>
    </div>
  )
}
