import { useId, useState, type FormEvent } from 'react'
import { livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { AnimalEvent } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, primaryButtonClass } from '../../components/ui'
import { formatDate, formatNumber, todayIso } from '../../lib/format'
import { overallDailyGain, weightHistory, type WeightPoint } from '../../lib/livestock'
import { cardClass, labelClass } from './shared'

/** Linha simples do peso ao longo do tempo (decorativa: os números estão na tabela ao lado). */
function WeightChart({ points }: { points: WeightPoint[] }) {
  if (points.length < 2) return null
  const width = 280
  const height = 72
  const pad = 6
  const times = points.map((p) => new Date(`${p.date}T00:00:00Z`).getTime())
  const minT = Math.min(...times)
  const spanT = Math.max(1, Math.max(...times) - minT)
  const weights = points.map((p) => p.weight)
  const minW = Math.min(...weights)
  const spanW = Math.max(1, Math.max(...weights) - minW)
  const coords = points.map((p, i) => [
    pad + ((times[i] - minT) / spanT) * (width - pad * 2),
    height - pad - ((p.weight - minW) / spanW) * (height - pad * 2),
  ])
  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="h-20 w-full max-w-xs" aria-hidden="true">
      <polyline
        points={coords.map(([x, y]) => `${x},${y}`).join(' ')}
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
        className="text-mata"
      />
      {coords.map(([x, y], i) => (
        <circle key={i} cx={x} cy={y} r="3" className="fill-rust" />
      ))}
    </svg>
  )
}

export function WeightSection({
  animalId,
  events,
  canWrite,
  onSaved,
}: {
  animalId: number
  events: AnimalEvent[]
  canWrite: boolean
  onSaved: () => void
}) {
  const ids = useId()
  const [date, setDate] = useState(todayIso)
  const [weight, setWeight] = useState('')
  const [result, setResult] = useState<{ error?: string; success?: string }>({})
  const points = weightHistory(events)
  const gain = overallDailyGain(points)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    try {
      await livestockApi.weigh({ date, entries: [{ animalId, weight: Number(weight) }] })
      setWeight('')
      setResult({ success: 'Pesagem registrada.' })
      onSaved()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    }
  }

  return (
    <section className={`${cardClass} min-w-0`}>
      <h2 className="mb-3 text-sm font-semibold text-terra">Peso</h2>
      {points.length === 0 ? (
        <p className="text-sm text-terra/50">Nenhuma pesagem ainda.</p>
      ) : (
        <>
          <div className="flex flex-wrap items-end gap-x-6 gap-y-1">
            <p className="font-serif text-2xl font-semibold text-terra tabular-nums">
              {formatNumber(points[points.length - 1].weight)} <span className="text-sm font-normal text-terra/55">kg</span>
            </p>
            {gain != null && (
              <p className="text-sm text-terra/70">
                Ganho médio:{' '}
                <strong className={`font-medium ${gain < 0 ? 'text-rust' : 'text-mata'}`}>
                  {gain > 0 ? '+' : ''}
                  {formatNumber(gain)} kg/dia
                </strong>{' '}
                <span className="text-terra/50">desde {formatDate(points[0].date)}</span>
              </p>
            )}
          </div>
          <WeightChart points={points} />
          <table className="mt-2 w-full text-sm">
            <caption className="sr-only">Histórico de peso</caption>
            <thead>
              <tr className="text-left text-xs text-terra/55">
                <th className="pb-1 font-medium">Data</th>
                <th className="pb-1 text-right font-medium">Peso</th>
                <th className="pb-1 text-right font-medium">GMD</th>
              </tr>
            </thead>
            <tbody>
              {[...points].reverse().map((p, i) => (
                <tr key={`${p.date}-${i}`} className="border-t border-terra/5">
                  <td className="py-1 tabular-nums text-terra/70">{formatDate(p.date)}</td>
                  <td className="py-1 text-right tabular-nums">{formatNumber(p.weight)} kg</td>
                  <td className={`py-1 text-right tabular-nums ${p.dailyGain == null ? 'text-terra/40' : p.dailyGain < 0 ? 'text-rust' : 'text-mata'}`}>
                    {p.dailyGain == null ? '—' : `${p.dailyGain > 0 ? '+' : ''}${formatNumber(p.dailyGain)} kg/dia`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
      {canWrite && (
        <form onSubmit={handleSubmit} className="mt-4 space-y-2 border-t border-terra/10 pt-3">
          <FormMessage {...result} />
          <div className="flex flex-wrap items-end gap-2">
            <div>
              <label htmlFor={`${ids}-weight`} className={labelClass}>
                Novo peso (kg)
              </label>
              <input id={`${ids}-weight`} required type="number" min="0.01" step="0.01" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} className={`${inputClass} w-32`} />
            </div>
            <div>
              <label htmlFor={`${ids}-date`} className={labelClass}>
                Data
              </label>
              <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={`${inputClass} w-40`} />
            </div>
            <button className={primaryButtonClass}>Registrar pesagem</button>
          </div>
        </form>
      )}
    </section>
  )
}
