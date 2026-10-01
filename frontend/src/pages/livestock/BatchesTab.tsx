import { useEffect, useId, useState, type FormEvent } from 'react'
import { activitiesApi, livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Activity, AnimalBatch, BatchTracking, ExitType, Species } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../../components/ui'
import { formatNumber, todayIso } from '../../lib/format'
import { allSpecies, exitLabels, speciesLabels } from '../../lib/livestock'
import { WithdrawalBadge, cardClass, labelClass } from './shared'

function BatchForm({
  batch,
  activities,
  onSaved,
  onCancel,
}: {
  batch?: AnimalBatch
  activities: Activity[]
  onSaved: () => void
  onCancel?: () => void
}) {
  const ids = useId()
  const [name, setName] = useState(batch?.name ?? '')
  const [species, setSpecies] = useState<Species>(batch?.species ?? 'BOVINO')
  const [tracking, setTracking] = useState<BatchTracking>(batch?.tracking ?? 'INDIVIDUAL')
  const [activityId, setActivityId] = useState(batch?.activityId ? String(batch.activityId) : '')
  const [notes, setNotes] = useState(batch?.notes ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const countOnly = species === 'AVE' || species === 'PEIXE'

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    const data = {
      name,
      species,
      tracking: countOnly ? 'AGGREGATE' : tracking,
      activityId: activityId ? Number(activityId) : null,
      notes: notes || null,
    } as const
    try {
      if (batch) await livestockApi.updateBatch(batch.id, data)
      else await livestockApi.createBatch(data)
      if (!batch) {
        setName('')
        setNotes('')
      }
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage error={error} />
      <div>
        <label htmlFor={`${ids}-name`} className={labelClass}>
          Nome do lote
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={100}
          placeholder="Ex.: Vacas em lactação, Bezerros 2026, Galpão 1"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-species`} className={labelClass}>
            Espécie
          </label>
          <select id={`${ids}-species`} value={species} onChange={(e) => setSpecies(e.target.value as Species)} className={inputClass}>
            {allSpecies.map((s) => (
              <option key={s} value={s}>
                {speciesLabels[s]}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-activity`} className={labelClass}>
            Atividade <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select id={`${ids}-activity`} value={activityId} onChange={(e) => setActivityId(e.target.value)} className={inputClass}>
            <option value="">Nenhuma</option>
            {activities.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
        </div>
      </div>
      <fieldset>
        <legend className={labelClass}>Controle</legend>
        <div className="grid gap-1 sm:grid-cols-2">
          {(
            [
              ['INDIVIDUAL', 'Animal por animal', 'Cada um com brinco, idade, peso e histórico.'],
              ['AGGREGATE', 'Só a quantidade', 'Conta cabeças: aves, peixes ou animais manejados em grupo.'],
            ] as [BatchTracking, string, string][]
          ).map(([value, title, help]) => {
            const disabled = countOnly && value === 'INDIVIDUAL'
            const checked = countOnly ? value === 'AGGREGATE' : tracking === value
            return (
              <label
                key={value}
                className={`rounded-md border px-3 py-2 text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                  disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
                } ${checked ? 'border-rust bg-rust/10' : 'border-terra/15'}`}
              >
                <input
                  type="radio"
                  name={`${ids}-tracking`}
                  checked={checked}
                  disabled={disabled}
                  onChange={() => setTracking(value)}
                  className="sr-only"
                />
                <span className={`block font-medium ${checked ? 'text-rust' : 'text-terra'}`}>{title}</span>
                <span className="block text-xs text-terra/60">{help}</span>
              </label>
            )
          })}
        </div>
        {countOnly && <p className="mt-1 text-xs text-terra/55">{speciesLabels[species]}s são controlados só pela quantidade.</p>}
      </fieldset>
      <div>
        <label htmlFor={`${ids}-notes`} className={labelClass}>
          Observações <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input id={`${ids}-notes`} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
      </div>
      <div className="flex gap-2">
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Salvando…' : batch ? 'Salvar' : 'Criar lote'}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className={`cursor-pointer ${outlineButtonClass}`}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}

/** Entrada ou saída de cabeças num lote por contagem, sem compra/venda. */
function CountForm({ batch, onSaved }: { batch: AnimalBatch; onSaved: () => void }) {
  const ids = useId()
  const [direction, setDirection] = useState<'in' | 'out'>('in')
  const [count, setCount] = useState('')
  const [date, setDate] = useState(todayIso)
  const [exitType, setExitType] = useState<ExitType>('MORTE')
  const [description, setDescription] = useState('')
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    try {
      const headCount = Number(count)
      if (direction === 'in') await livestockApi.addHeads(batch.id, { headCount, date, description: description || undefined })
      else await livestockApi.removeHeads(batch.id, { headCount, date, exitType, description: description || undefined })
      setResult({ success: direction === 'in' ? `Entrada de ${count} registrada.` : `Saída de ${count} registrada.` })
      setCount('')
      setDescription('')
      onSaved()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-3 space-y-2 rounded-xl bg-terreiro/60 p-3">
      <FormMessage {...result} />
      <div className="flex flex-wrap items-end gap-2">
        <div>
          <label htmlFor={`${ids}-dir`} className={labelClass}>
            Movimento
          </label>
          <select id={`${ids}-dir`} value={direction} onChange={(e) => setDirection(e.target.value as 'in' | 'out')} className={`${inputClass} w-32`}>
            <option value="in">Entrada</option>
            <option value="out">Saída</option>
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-count`} className={labelClass}>
            Cabeças
          </label>
          <input id={`${ids}-count`} required type="number" min="1" step="1" value={count} onChange={(e) => setCount(e.target.value)} className={`${inputClass} w-28`} />
        </div>
        {direction === 'out' && (
          <div>
            <label htmlFor={`${ids}-exit`} className={labelClass}>
              Motivo
            </label>
            <select id={`${ids}-exit`} value={exitType} onChange={(e) => setExitType(e.target.value as ExitType)} className={`${inputClass} w-32`}>
              {(Object.keys(exitLabels) as ExitType[]).map((t) => (
                <option key={t} value={t}>
                  {exitLabels[t]}
                </option>
              ))}
            </select>
          </div>
        )}
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            Data
          </label>
          <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={`${inputClass} w-40`} />
        </div>
        <div className="min-w-40 flex-1">
          <label htmlFor={`${ids}-desc`} className={labelClass}>
            Detalhe <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input
            id={`${ids}-desc`}
            maxLength={255}
            placeholder={direction === 'in' ? 'Ex.: eclosão, nascimentos' : 'Ex.: calor, predador'}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={inputClass}
          />
        </div>
        <button className={primaryButtonClass}>Registrar</button>
      </div>
      <p className="text-xs text-terra/55">Compras e vendas do lote ficam na aba Compras e vendas, que lança o financeiro.</p>
    </form>
  )
}

export function BatchesTab({
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
  const [activities, setActivities] = useState<Activity[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [countingId, setCountingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  useEffect(() => {
    activitiesApi.list().then(setActivities).catch(() => setActivities([]))
  }, [])

  const visible = batches.filter((b) => showArchived || !b.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(batch: AnimalBatch) {
    const ok = await confirm({
      title: `Excluir o lote ${batch.name}?`,
      message: 'Só lotes criados por engano, sem animais nem movimentações, podem ser excluídos. Com histórico, arquive.',
      confirmLabel: 'Excluir lote',
    })
    if (ok) run(() => livestockApi.deleteBatch(batch.id))
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Novo lote</h2>
          <BatchForm activities={activities} onSaved={onChanged} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Lotes</h2>
          <label className="flex items-center gap-2 text-xs text-terra/60">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
            Mostrar arquivados
          </label>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">
            Nenhum lote ainda. Lotes agrupam animais por fase, pasto ou finalidade — e guardam a contagem de aves e peixes.
          </p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((b) => (
              <li key={b.id} className={`py-3 ${b.archived ? 'opacity-60' : ''}`}>
                {editingId === b.id ? (
                  <BatchForm batch={b} activities={activities} onCancel={() => setEditingId(null)} onSaved={() => {
                    setEditingId(null)
                    onChanged()
                  }} />
                ) : (
                  <>
                    <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                      <div>
                        <p className="font-medium text-terra">
                          {b.name}
                          {b.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivado</span>}
                          <WithdrawalBadge until={b.withdrawalUntil} />
                        </p>
                        <p className="text-xs text-terra/55">
                          {speciesLabels[b.species]} · {b.tracking === 'INDIVIDUAL' ? 'animal por animal' : 'só a quantidade'}
                          {b.activityName && ` · ${b.activityName}`}
                        </p>
                      </div>
                      <p className="font-serif text-lg font-semibold text-terra tabular-nums">
                        {formatNumber(b.headCount)} <span className="text-sm font-normal text-terra/55">{b.headCount === 1 ? 'cabeça' : 'cabeças'}</span>
                      </p>
                    </div>
                    <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm">
                      {canWrite && b.tracking === 'AGGREGATE' && !b.archived && (
                        <button onClick={() => setCountingId(countingId === b.id ? null : b.id)} aria-expanded={countingId === b.id} className={`cursor-pointer ${linkNeutralClass}`}>
                          Entrada / saída
                        </button>
                      )}
                      {canWrite && (
                        <button onClick={() => setEditingId(b.id)} className={`cursor-pointer ${linkNeutralClass}`}>
                          Editar
                        </button>
                      )}
                      {canManage &&
                        (b.archived ? (
                          <button onClick={() => run(() => livestockApi.unarchiveBatch(b.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Reativar
                          </button>
                        ) : (
                          <button onClick={() => run(() => livestockApi.archiveBatch(b.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Arquivar
                          </button>
                        ))}
                      {canManage && (
                        <button onClick={() => handleDelete(b)} className={`cursor-pointer ${linkDangerClass}`}>
                          Excluir
                        </button>
                      )}
                    </div>
                    {countingId === b.id && <CountForm batch={b} onSaved={onChanged} />}
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
