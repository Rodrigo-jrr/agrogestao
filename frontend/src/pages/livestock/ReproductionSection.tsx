import { useEffect, useId, useState, type FormEvent } from 'react'
import { reproductionApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Animal, AnimalBatch, OffspringInput, PregnancyResult, ReproductiveEvent, ReproductiveEventKind, Sex } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, primaryButtonClass } from '../../components/ui'
import { formatDate, todayIso } from '../../lib/format'
import { pregnancyResultLabels, reproductiveKindLabels, reproductiveStatus, reproductiveStatusLabels, sexLabels } from '../../lib/livestock'
import { cardClass, labelClass } from './shared'

const kinds: ReproductiveEventKind[] = ['CIO', 'COBERTURA', 'INSEMINACAO', 'DIAGNOSTICO_PRENHEZ', 'PARTO', 'ABORTO']

/** Cobertura e inseminação são substantivos femininos ("a cobertura foi registrada"); os demais, masculinos. */
const feminineKinds = new Set<ReproductiveEventKind>(['COBERTURA', 'INSEMINACAO'])

const statusBadgeClass: Record<'VAZIA' | 'COBERTA' | 'PRENHE', string> = {
  VAZIA: 'bg-terra/5 text-terra/60',
  COBERTA: 'bg-trigo/20 text-trigo-dark',
  PRENHE: 'bg-mata/15 text-mata',
}

interface OffspringRow {
  tag: string
  name: string
  sex: Sex
  breed: string
  weight: string
}

function emptyOffspringRow(): OffspringRow {
  return { tag: '', name: '', sex: 'FEMEA', breed: '', weight: '' }
}

export function ReproductionSection({
  animal,
  batches,
  canWrite,
  onSaved,
}: {
  animal: Animal
  batches: AnimalBatch[]
  canWrite: boolean
  onSaved: () => void
}) {
  const ids = useId()
  const [events, setEvents] = useState<ReproductiveEvent[]>([])
  const [version, setVersion] = useState(0)
  const [kind, setKind] = useState<ReproductiveEventKind>('CIO')
  const [date, setDate] = useState(todayIso)
  const [sire, setSire] = useState('')
  const [pregnancyResult, setPregnancyResult] = useState<PregnancyResult | ''>('')
  const [expectedCalvingDate, setExpectedCalvingDate] = useState('')
  const [batchId, setBatchId] = useState('')
  const [offspring, setOffspring] = useState<OffspringRow[]>([emptyOffspringRow()])
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  useEffect(() => {
    reproductionApi
      .list({ animalId: animal.id, size: 200 })
      .then((page) => setEvents(page.content))
      .catch(() => setEvents([]))
  }, [animal.id, version])

  const status = reproductiveStatus(events)
  const batchOptions = batches.filter((b) => !b.archived && b.tracking === 'INDIVIDUAL' && b.speciesId === animal.speciesId)

  function updateOffspring(index: number, patch: Partial<OffspringRow>) {
    setOffspring((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function resetForm() {
    setSire('')
    setPregnancyResult('')
    setExpectedCalvingDate('')
    setBatchId('')
    setOffspring([emptyOffspringRow()])
    setNotes('')
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    if (kind === 'DIAGNOSTICO_PRENHEZ' && !pregnancyResult) {
      setResult({ error: 'Informe o resultado do diagnóstico.' })
      return
    }
    if (kind === 'DIAGNOSTICO_PRENHEZ' && pregnancyResult === 'POSITIVO' && !expectedCalvingDate) {
      setResult({ error: 'Informe a previsão do parto.' })
      return
    }
    const offspringRows = offspring.filter((row) => row.tag.trim())
    if (kind === 'PARTO' && offspringRows.length === 0) {
      setResult({ error: 'Informe ao menos uma cria nascida.' })
      return
    }
    setSaving(true)
    try {
      await reproductionApi.register({
        animalId: animal.id,
        kind,
        date,
        ...(kind === 'COBERTURA' || kind === 'INSEMINACAO' ? { sire: sire || null } : {}),
        ...(kind === 'DIAGNOSTICO_PRENHEZ'
          ? {
              pregnancyResult: pregnancyResult || null,
              ...(pregnancyResult === 'POSITIVO' ? { expectedCalvingDate } : {}),
            }
          : {}),
        ...(kind === 'PARTO'
          ? {
              batchId: batchId ? Number(batchId) : null,
              offspring: offspringRows.map(
                (row): OffspringInput => ({
                  tag: row.tag,
                  name: row.name || null,
                  sex: row.sex,
                  breed: row.breed || null,
                  weight: row.weight ? Number(row.weight) : null,
                }),
              ),
            }
          : {}),
        notes: notes || null,
      })
      setResult({ success: `${reproductiveKindLabels[kind]} registrad${feminineKinds.has(kind) ? 'a' : 'o'}.` })
      resetForm()
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
    <section id="reproducao" className={`${cardClass} min-w-0`}>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <h2 className="text-sm font-semibold text-terra">Reprodução</h2>
        <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusBadgeClass[status.status]}`}>
          {reproductiveStatusLabels[status.status]}
        </span>
        {status.status === 'PRENHE' && status.expectedCalvingDate && (
          <span className="text-xs text-terra/60">parto previsto para {formatDate(status.expectedCalvingDate)}</span>
        )}
        {status.status === 'COBERTA' && status.since && <span className="text-xs text-terra/60">desde {formatDate(status.since)}</span>}
      </div>

      {canWrite && (
        <form onSubmit={handleSubmit} className="space-y-3">
          <FormMessage {...result} />
          <fieldset>
            <legend className={labelClass}>Evento</legend>
            <div className="grid grid-cols-3 gap-1">
              {kinds.map((k) => (
                <label key={k} className={radio(kind === k)}>
                  <input type="radio" name={`${ids}-kind`} checked={kind === k} onChange={() => setKind(k)} className="sr-only" />
                  {reproductiveKindLabels[k]}
                </label>
              ))}
            </div>
          </fieldset>

          <div>
            <label htmlFor={`${ids}-date`} className={labelClass}>
              Data
            </label>
            <input
              id={`${ids}-date`}
              type="date"
              required
              max={todayIso()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className={`${inputClass} w-40`}
            />
          </div>

          {(kind === 'COBERTURA' || kind === 'INSEMINACAO') && (
            <div>
              <label htmlFor={`${ids}-sire`} className={labelClass}>
                Touro, reprodutor ou sêmen <span className="font-normal text-terra/45">(opcional)</span>
              </label>
              <input id={`${ids}-sire`} maxLength={100} value={sire} onChange={(e) => setSire(e.target.value)} className={inputClass} />
            </div>
          )}

          {kind === 'DIAGNOSTICO_PRENHEZ' && (
            <div className="grid gap-3 sm:grid-cols-2">
              <fieldset>
                <legend className={labelClass}>Resultado</legend>
                <div className="grid grid-cols-2 gap-1">
                  {(['POSITIVO', 'NEGATIVO'] as PregnancyResult[]).map((r) => (
                    <label key={r} className={radio(pregnancyResult === r)}>
                      <input
                        type="radio"
                        name={`${ids}-result`}
                        checked={pregnancyResult === r}
                        onChange={() => setPregnancyResult(r)}
                        className="sr-only"
                      />
                      {pregnancyResultLabels[r]}
                    </label>
                  ))}
                </div>
              </fieldset>
              {pregnancyResult === 'POSITIVO' && (
                <div>
                  <label htmlFor={`${ids}-calving`} className={labelClass}>
                    Previsão do parto
                  </label>
                  <input
                    id={`${ids}-calving`}
                    type="date"
                    required
                    min={date}
                    value={expectedCalvingDate}
                    onChange={(e) => setExpectedCalvingDate(e.target.value)}
                    className={inputClass}
                  />
                </div>
              )}
            </div>
          )}

          {kind === 'PARTO' && (
            <>
              <div>
                <label htmlFor={`${ids}-batch`} className={labelClass}>
                  Lote das crias <span className="font-normal text-terra/45">(opcional, padrão: lote da mãe)</span>
                </label>
                <select id={`${ids}-batch`} value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
                  <option value="">Lote da mãe</option>
                  {batchOptions.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </select>
              </div>
              <fieldset className="space-y-2">
                <legend className={labelClass}>Crias</legend>
                {offspring.map((row, index) => (
                  <div key={index} className="grid gap-2 rounded-lg border border-terra/10 p-2 sm:grid-cols-5">
                    <input
                      aria-label={`Brinco da cria ${index + 1}`}
                      required
                      maxLength={30}
                      placeholder="Brinco"
                      value={row.tag}
                      onChange={(e) => updateOffspring(index, { tag: e.target.value })}
                      className={inputClass}
                    />
                    <input
                      aria-label={`Nome da cria ${index + 1}`}
                      maxLength={100}
                      placeholder="Nome (opcional)"
                      value={row.name}
                      onChange={(e) => updateOffspring(index, { name: e.target.value })}
                      className={inputClass}
                    />
                    <select
                      aria-label={`Sexo da cria ${index + 1}`}
                      value={row.sex}
                      onChange={(e) => updateOffspring(index, { sex: e.target.value as Sex })}
                      className={inputClass}
                    >
                      {(['FEMEA', 'MACHO'] as Sex[]).map((s) => (
                        <option key={s} value={s}>
                          {sexLabels[s]}
                        </option>
                      ))}
                    </select>
                    <input
                      aria-label={`Raça da cria ${index + 1}`}
                      maxLength={60}
                      placeholder="Raça (opcional)"
                      value={row.breed}
                      onChange={(e) => updateOffspring(index, { breed: e.target.value })}
                      className={inputClass}
                    />
                    <div className="flex gap-1">
                      <input
                        aria-label={`Peso da cria ${index + 1} (kg)`}
                        type="number"
                        min="0.01"
                        step="0.01"
                        inputMode="decimal"
                        placeholder="Peso (kg)"
                        value={row.weight}
                        onChange={(e) => updateOffspring(index, { weight: e.target.value })}
                        className={inputClass}
                      />
                      {offspring.length > 1 && (
                        <button
                          type="button"
                          onClick={() => setOffspring((rows) => rows.filter((_, i) => i !== index))}
                          aria-label={`Remover cria ${index + 1}`}
                          className="cursor-pointer px-2 text-terra/50 hover:text-rust"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                {offspring.length < 20 && (
                  <button
                    type="button"
                    onClick={() => setOffspring((rows) => [...rows, emptyOffspringRow()])}
                    className="cursor-pointer text-xs font-medium text-rust hover:underline"
                  >
                    + Adicionar cria
                  </button>
                )}
              </fieldset>
            </>
          )}

          <div>
            <label htmlFor={`${ids}-notes`} className={labelClass}>
              Observações <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-notes`} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
          </div>

          <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
            {saving ? 'Registrando…' : `Registrar ${reproductiveKindLabels[kind].toLowerCase()}`}
          </button>
        </form>
      )}
    </section>
  )
}
