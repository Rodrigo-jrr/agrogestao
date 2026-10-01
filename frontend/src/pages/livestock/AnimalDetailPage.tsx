import { useCallback, useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { livestockApi, reproductionApi, speciesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { AnimalBatch, AnimalDetail, AnimalEvent, ExitType, SpeciesCatalogItem } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../../components/ui'
import { useProperty } from '../../property/PropertyContext'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../../lib/format'
import { eventLabels, exitLabels, formatAge, originLabels, sexLabels, statusLabels } from '../../lib/livestock'
import { AnimalForm } from './AnimalForm'
import { ReproductionSection } from './ReproductionSection'
import { WithdrawalBadge, cardClass, labelClass } from './shared'
import { WeightSection } from './WeightSection'

function describeEvent(e: AnimalEvent) {
  if (e.type === 'MUDANCA_LOTE') return `${e.fromBatchName ?? 'Sem lote'} → ${e.toBatchName ?? 'sem lote'}`
  const parts = [
    e.description,
    e.weight != null ? `${formatNumber(e.weight)} kg` : null,
    e.amount != null ? formatCurrency(e.amount) : null,
    e.withdrawalUntil ? `carência até ${formatDate(e.withdrawalUntil)}` : null,
  ]
  return parts.filter(Boolean).join(' · ')
}

const dotColor: Record<AnimalEvent['type'], string> = {
  ENTRADA: 'bg-mata',
  SAIDA: 'bg-rust',
  MUDANCA_LOTE: 'bg-trigo-dark',
  PESAGEM: 'bg-terra/40',
  SANIDADE: 'bg-trigo',
  REPRODUCAO: 'bg-mata-dark',
}

/** Saída sem venda (morte, descarte...): diálogo com motivo e data. */
function ExitDialog({ open, onClose, onDone, animalId, tag }: { open: boolean; onClose: () => void; onDone: () => void; animalId: number; tag: string }) {
  const ids = useId()
  const ref = useRef<HTMLDialogElement>(null)
  const [type, setType] = useState<ExitType>('MORTE')
  const [date, setDate] = useState(todayIso)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await livestockApi.exitAnimal(animalId, { type, date, reason: reason || undefined })
      onDone()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <dialog
      ref={ref}
      aria-labelledby={`${ids}-title`}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      className="m-auto w-[min(28rem,calc(100vw-2rem))] rounded-2xl border border-terra/10 bg-white p-0 text-terra shadow-xl backdrop:bg-terra/40"
    >
      {open && (
        <form onSubmit={handleSubmit} className="space-y-3 p-5">
          <h2 id={`${ids}-title`} className="font-serif text-xl font-semibold">
            Registrar saída de {tag}
          </h2>
          <p className="text-sm text-terra/70">Para venda, use Compras e vendas: ela também lança o recebimento.</p>
          <FormMessage error={error} />
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label htmlFor={`${ids}-type`} className={labelClass}>
                Motivo
              </label>
              <select id={`${ids}-type`} value={type} onChange={(e) => setType(e.target.value as ExitType)} className={inputClass}>
                {(Object.keys(exitLabels) as ExitType[]).map((t) => (
                  <option key={t} value={t}>
                    {exitLabels[t]}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor={`${ids}-date`} className={labelClass}>
                Data
              </label>
              <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
            </div>
          </div>
          <div>
            <label htmlFor={`${ids}-reason`} className={labelClass}>
              Detalhe <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-reason`} maxLength={255} placeholder="Ex.: causa da morte" value={reason} onChange={(e) => setReason(e.target.value)} className={inputClass} />
          </div>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button type="button" onClick={onClose} className={`cursor-pointer ${outlineButtonClass}`}>
              Voltar
            </button>
            <button className={primaryButtonClass}>Registrar saída</button>
          </div>
        </form>
      )}
    </dialog>
  )
}

export function AnimalDetailPage() {
  const { id } = useParams()
  const animalId = Number(id)
  const navigate = useNavigate()
  const confirm = useConfirm()
  const { currentProperty } = useProperty()
  const [detail, setDetail] = useState<AnimalDetail | null>(null)
  const [batches, setBatches] = useState<AnimalBatch[]>([])
  const [species, setSpecies] = useState<SpeciesCatalogItem[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [exiting, setExiting] = useState(false)

  const role = currentProperty?.role
  const canWrite = role === 'OWNER' || role === 'MANAGER' || role === 'WORKER'
  const canManage = role === 'OWNER' || role === 'MANAGER'

  const load = useCallback(() => {
    livestockApi
      .animal(animalId)
      .then((d) => {
        setDetail(d)
        setLoadError(null)
      })
      .catch((err) => setLoadError(apiErrorMessage(err)))
  }, [animalId])

  useEffect(() => {
    load()
    livestockApi.listBatches(true).then(setBatches).catch(() => setBatches([]))
    speciesApi.list(true).then(setSpecies).catch(() => setSpecies([]))
  }, [load, currentProperty?.id])

  if (loadError) {
    return (
      <div className="space-y-3">
        <Link to="/pecuaria" className={linkNeutralClass}>
          ← Pecuária
        </Link>
        <FormMessage error={loadError} />
      </div>
    )
  }
  if (!detail) return <p className="text-sm text-terra/50">Carregando…</p>

  const a = detail.animal

  async function removeWeighing(e: AnimalEvent) {
    const ok = await confirm({
      title: `Excluir a pesagem de ${formatDate(e.date)}?`,
      message: `O peso de ${formatNumber(e.weight ?? 0)} kg sai do histórico; o peso atual volta a ser o da pesagem anterior.`,
      confirmLabel: 'Excluir pesagem',
    })
    if (!ok) return
    setError(null)
    try {
      await livestockApi.deleteWeighing(e.id)
      load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function removeReproduction(e: AnimalEvent) {
    const ok = await confirm({
      title: `Excluir o evento de ${formatDate(e.date)}?`,
      message: 'Partos e abortos não podem ser excluídos — corrija o cadastro das crias ou registre a saída do animal, se for o caso.',
      confirmLabel: 'Excluir evento',
    })
    if (!ok) return
    setError(null)
    try {
      await reproductionApi.remove(e.reproductiveEventId!)
      load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function reactivate() {
    setError(null)
    try {
      await livestockApi.reactivateAnimal(a.id)
      load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function remove() {
    const ok = await confirm({
      title: `Excluir o animal ${a.tag}?`,
      message: 'Use só para cadastro feito por engano. Animais com movimentações, crias ou vindos de compra não podem ser excluídos: registre a saída.',
      confirmLabel: 'Excluir animal',
    })
    if (!ok) return
    try {
      await livestockApi.deleteAnimal(a.id)
      navigate('/pecuaria', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const facts: [string, string][] = [
    ['Espécie', `${a.speciesName}${a.breed ? ` · ${a.breed}` : ''}`],
    ['Sexo', sexLabels[a.sex]],
    ['Nascimento', a.birthDate ? `${formatDate(a.birthDate)}${a.birthDateEstimated ? ' (aprox.)' : ''} · ${formatAge(a.birthDate)}` : '—'],
    ['Origem', `${originLabels[a.origin]} em ${formatDate(a.entryDate)}`],
    ['Lote', a.batchName ?? '—'],
    ['Mãe', a.motherTag ?? '—'],
    ['Pai', a.sire ?? '—'],
  ]

  return (
    <div className="space-y-4">
      <Link to="/pecuaria" className={`text-sm ${linkNeutralClass}`}>
        ← Pecuária
      </Link>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-serif text-2xl font-semibold text-terra">
            {a.tag}
            {a.name && <span className="ml-2 text-terra/60">{a.name}</span>}
          </h1>
          <p className={`mt-1 text-sm ${a.status === 'ATIVO' ? 'text-mata' : 'text-terra/60'}`}>
            {statusLabels[a.status]}
            {a.exitDate && ` em ${formatDate(a.exitDate)}`}
            {a.exitReason && ` — ${a.exitReason}`}
            {a.status === 'ATIVO' && <WithdrawalBadge until={a.withdrawalUntil} />}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canWrite && !editing && (
            <button onClick={() => setEditing(true)} className={`cursor-pointer ${outlineButtonClass}`}>
              Editar dados
            </button>
          )}
          {canWrite && a.status === 'ATIVO' && (
            <button onClick={() => setExiting(true)} className={`cursor-pointer ${outlineButtonClass}`}>
              Registrar saída…
            </button>
          )}
          {canManage && (a.status === 'MORTO' || a.status === 'DESCARTADO') && (
            <button onClick={reactivate} className={`cursor-pointer ${outlineButtonClass}`}>
              Desfazer saída
            </button>
          )}
        </div>
      </div>
      <FormMessage error={error} />

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="min-w-0 space-y-4 lg:col-span-1">
        <section className={`${cardClass} min-w-0`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Dados</h2>
          {editing ? (
            <AnimalForm
              animal={a}
              batches={batches}
              species={species}
              onCancel={() => setEditing(false)}
              onSaved={() => {
                setEditing(false)
                load()
              }}
            />
          ) : (
            <>
              <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
                {facts.map(([label, value]) => (
                  <div key={label} className="contents">
                    <dt className="text-terra/55">{label}</dt>
                    <dd className="text-terra">{value}</dd>
                  </div>
                ))}
              </dl>
              {a.notes && <p className="mt-3 text-sm text-terra/70">{a.notes}</p>}
              {/* Só para cadastro feito por engano: o servidor recusa animais comprados, com movimentações ou crias. */}
              {canManage && a.origin !== 'COMPRA' && detail.events.length <= 1 && detail.offspring.length === 0 && (
                <button onClick={remove} className={`mt-4 cursor-pointer text-sm ${linkDangerClass}`}>
                  Excluir cadastro
                </button>
              )}
            </>
          )}
        </section>
        <WeightSection animalId={a.id} events={detail.events} canWrite={canWrite && a.status === 'ATIVO'} onSaved={load} />
        </div>

        <section className={`${cardClass} min-w-0 lg:col-span-2`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Linha do tempo</h2>
          <ol className="relative space-y-4 border-l border-terra/10 pl-5">
            {detail.events.map((e) => (
              <li key={e.id} className="relative">
                <span
                  aria-hidden="true"
                  className={`absolute -left-[1.6rem] top-1.5 h-2.5 w-2.5 rounded-full ring-4 ring-white ${dotColor[e.type]}`}
                />
                <p className="text-xs text-terra/55 tabular-nums">{formatDate(e.date)}</p>
                <p className="text-sm font-medium text-terra">{eventLabels[e.type]}</p>
                {describeEvent(e) && <p className="text-sm text-terra/70">{describeEvent(e)}</p>}
                {canManage && e.type === 'PESAGEM' && (
                  <button
                    onClick={() => removeWeighing(e)}
                    aria-label={`Excluir pesagem de ${formatDate(e.date)}`}
                    className={`cursor-pointer text-xs ${linkDangerClass}`}
                  >
                    Excluir pesagem
                  </button>
                )}
                {canManage &&
                  e.type === 'REPRODUCAO' &&
                  e.reproductiveEventId != null &&
                  !e.description?.startsWith('Parto') &&
                  !e.description?.startsWith('Aborto') && (
                    <button
                      onClick={() => removeReproduction(e)}
                      aria-label={`Excluir evento de ${formatDate(e.date)}`}
                      className={`cursor-pointer text-xs ${linkDangerClass}`}
                    >
                      Excluir evento
                    </button>
                  )}
              </li>
            ))}
          </ol>

          {detail.offspring.length > 0 && (
            <>
              <h2 className="mt-6 mb-2 text-sm font-semibold text-terra">Crias</h2>
              <ul className="flex flex-wrap gap-2">
                {detail.offspring.map((c) => (
                  <li key={c.id}>
                    <Link to={`/pecuaria/animais/${c.id}`} className="inline-block rounded-full border border-terra/15 px-3 py-1 text-sm text-terra hover:border-rust hover:text-rust">
                      {c.tag} · {sexLabels[c.sex].toLowerCase()} · {formatAge(c.birthDate)}
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>

      {a.sex === 'FEMEA' && (
        <ReproductionSection animal={a} batches={batches} canWrite={canWrite && a.status === 'ATIVO'} onSaved={load} />
      )}

      <ExitDialog
        open={exiting}
        animalId={a.id}
        tag={a.tag}
        onClose={() => setExiting(false)}
        onDone={() => {
          setExiting(false)
          load()
        }}
      />
    </div>
  )
}
