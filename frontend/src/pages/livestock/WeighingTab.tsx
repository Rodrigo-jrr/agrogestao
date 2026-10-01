import { useEffect, useId, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Animal, AnimalBatch } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, primaryButtonClass } from '../../components/ui'
import { formatNumber, todayIso } from '../../lib/format'
import { cardClass, labelClass } from './shared'

const NO_BATCH = 'sem-lote'

/**
 * Pesagem no tronco: escolhe o lote, anota o peso de cada animal e salva tudo de uma vez.
 * Lote por contagem: peso médio de uma amostra.
 */
export function WeighingTab({ batches, onChanged }: { batches: AnimalBatch[]; onChanged: () => void }) {
  const ids = useId()
  const [batchKey, setBatchKey] = useState('')
  const [date, setDate] = useState(todayIso)
  const [animals, setAnimals] = useState<Animal[]>([])
  const [weights, setWeights] = useState<Record<number, string>>({})
  const [average, setAverage] = useState('')
  const [sample, setSample] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  const usable = batches.filter((b) => !b.archived)
  const batch = usable.find((b) => String(b.id) === batchKey)
  const aggregate = batch?.tracking === 'AGGREGATE'

  useEffect(() => {
    if (!batchKey || aggregate) return
    let active = true
    setLoading(true)
    livestockApi
      .listAnimals({ status: 'ATIVO', size: 200, batchId: batchKey === NO_BATCH ? undefined : Number(batchKey) })
      .then((page) => active && setAnimals(batchKey === NO_BATCH ? page.content.filter((a) => a.batchId == null) : page.content))
      .catch((err) => active && setResult({ error: apiErrorMessage(err) }))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [batchKey, aggregate])

  const filled = animals.filter((a) => Number(weights[a.id]) > 0)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    setSaving(true)
    try {
      if (aggregate && batch) {
        await livestockApi.weigh({ date, batchId: batch.id, averageWeight: Number(average), sampleSize: Number(sample) })
        setResult({ success: `Peso médio do lote ${batch.name} registrado.` })
        setAverage('')
        setSample('')
      } else {
        if (filled.length === 0) {
          setResult({ error: 'Anote o peso de ao menos um animal.' })
          return
        }
        await livestockApi.weigh({ date, entries: filled.map((a) => ({ animalId: a.id, weight: Number(weights[a.id]) })) })
        setResult({ success: `${filled.length === 1 ? '1 pesagem registrada' : `${filled.length} pesagens registradas`}.` })
        // Recarrega para mostrar os pesos novos como "último peso".
        const page = await livestockApi.listAnimals({
          status: 'ATIVO',
          size: 200,
          batchId: batchKey === NO_BATCH ? undefined : Number(batchKey),
        })
        setAnimals(batchKey === NO_BATCH ? page.content.filter((a) => a.batchId == null) : page.content)
        setWeights({})
      }
      onChanged()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className={`${cardClass} min-w-0`}>
      <h2 className="mb-1 text-sm font-semibold text-terra">Registrar pesagem</h2>
      <p className="mb-3 text-sm text-terra/60">
        Escolha o lote e anote o peso de cada animal. Quem ficar em branco não é pesado agora.
      </p>
      <form onSubmit={handleSubmit} className="space-y-3">
        <FormMessage {...result} />
        <div className="grid gap-3 sm:grid-cols-2 md:max-w-xl">
          <div>
            <label htmlFor={`${ids}-batch`} className={labelClass}>
              Lote
            </label>
            <select
              id={`${ids}-batch`}
              required
              value={batchKey}
              onChange={(e) => {
                setBatchKey(e.target.value)
                setWeights({})
                setAnimals([])
                setResult({})
              }}
              className={inputClass}
            >
              <option value="">Selecione</option>
              {usable.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.speciesName.toLowerCase()}, {formatNumber(b.headCount)} cab.)
                </option>
              ))}
              <option value={NO_BATCH}>Animais sem lote</option>
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-date`} className={labelClass}>
              Data
            </label>
            <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
          </div>
        </div>

        {aggregate && batch && (
          <div className="grid gap-3 sm:grid-cols-2 md:max-w-xl">
            <div>
              <label htmlFor={`${ids}-avg`} className={labelClass}>
                Peso médio (kg)
              </label>
              <input id={`${ids}-avg`} required type="number" min="0.001" step="0.001" inputMode="decimal" value={average} onChange={(e) => setAverage(e.target.value)} className={inputClass} />
            </div>
            <div>
              <label htmlFor={`${ids}-sample`} className={labelClass}>
                Animais pesados na amostra
              </label>
              <input id={`${ids}-sample`} required type="number" min="1" step="1" value={sample} onChange={(e) => setSample(e.target.value)} className={inputClass} />
            </div>
          </div>
        )}

        {batchKey && !aggregate && (
          <div className="relative overflow-x-auto">
            <table className="w-full min-w-[440px] text-sm">
              <thead>
                <tr className="text-left text-xs text-terra/55">
                  <th className="pb-2 font-medium">Brinco</th>
                  <th className="pb-2 text-right font-medium">Último peso</th>
                  <th className="pb-2 pl-4 font-medium">Peso hoje (kg)</th>
                  <th className="pb-2 text-right font-medium">Diferença</th>
                </tr>
              </thead>
              <tbody>
                {animals.map((a) => {
                  const typed = Number(weights[a.id])
                  const diff = typed > 0 && a.currentWeight != null ? Math.round((typed - a.currentWeight) * 100) / 100 : null
                  return (
                    <tr key={a.id} className="border-t border-terra/5">
                      <td className="py-1.5 pr-3">
                        <Link to={`/pecuaria/animais/${a.id}`} className="font-medium text-terra hover:text-rust hover:underline">
                          {a.tag}
                        </Link>
                        {a.name && <span className="ml-1 text-terra/55">{a.name}</span>}
                      </td>
                      <td className="py-1.5 text-right tabular-nums text-terra/60">
                        {a.currentWeight != null ? `${formatNumber(a.currentWeight)} kg` : '—'}
                      </td>
                      <td className="py-1.5 pl-4">
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          inputMode="decimal"
                          aria-label={`Peso de ${a.tag}`}
                          value={weights[a.id] ?? ''}
                          onChange={(e) => setWeights((w) => ({ ...w, [a.id]: e.target.value }))}
                          className={`${inputClass} w-28 py-1`}
                        />
                      </td>
                      <td className={`py-1.5 text-right tabular-nums ${diff == null ? '' : diff < 0 ? 'text-rust' : 'text-mata'}`}>
                        {diff == null ? '' : `${diff > 0 ? '+' : ''}${formatNumber(diff)} kg`}
                      </td>
                    </tr>
                  )
                })}
                {loading && (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-terra/50">
                      Carregando…
                    </td>
                  </tr>
                )}
                {!loading && animals.length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-terra/50">
                      Nenhum animal ativo neste lote.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {batchKey && (
          <button disabled={saving || (!aggregate && filled.length === 0)} className={primaryButtonClass}>
            {saving
              ? 'Salvando…'
              : aggregate
                ? 'Registrar peso médio'
                : filled.length > 1
                  ? `Salvar ${filled.length} pesagens`
                  : 'Salvar pesagem'}
          </button>
        )}
      </form>
    </section>
  )
}
