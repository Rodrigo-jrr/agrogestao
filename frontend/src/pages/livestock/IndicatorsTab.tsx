import { useEffect, useId, useState } from 'react'
import { indicatorsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { AnimalBatch, LivestockIndicators } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { PeriodFilter, presetPeriod, type Period } from '../../components/PeriodFilter'
import { inputClass } from '../../components/ui'
import { formatCurrency, formatNumber } from '../../lib/format'
import { cardClass, labelClass } from './shared'

function formatRate(value: number | null) {
  return value == null ? 'sem dados' : `${formatNumber(value)}%`
}

function formatDays(value: number | null) {
  return value == null ? 'sem dados' : `${formatNumber(value)} dias`
}

function formatGain(value: number | null) {
  return value == null ? 'sem dados' : `${formatNumber(value)} kg/dia`
}

function formatMoney(value: number | null) {
  return value == null ? 'sem dados' : formatCurrency(value)
}

function Tile({ label, value, hint, dark }: { label: string; value: string; hint?: string; dark?: boolean }) {
  return (
    <div className={dark ? 'rounded-2xl bg-mata p-4 text-terreiro shadow-sm' : `${cardClass} p-4`}>
      <p className={`text-xs font-semibold tracking-wider uppercase ${dark ? 'text-terreiro/60' : 'text-terra/45'}`}>{label}</p>
      <p className="mt-1 font-serif text-2xl font-semibold">{value}</p>
      {hint && <p className={`mt-1 text-xs ${dark ? 'text-terreiro/60' : 'text-terra/55'}`}>{hint}</p>}
    </div>
  )
}

export function IndicatorsTab({ batches }: { batches: AnimalBatch[] }) {
  const ids = useId()
  const [batchId, setBatchId] = useState('')
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))
  const [indicators, setIndicators] = useState<LivestockIndicators | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    indicatorsApi
      .get({ batchId: batchId ? Number(batchId) : undefined, from: period.from, to: period.to })
      .then(setIndicators)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [batchId, period])

  return (
    <div className="space-y-4">
      <section className={cardClass}>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <label htmlFor={`${ids}-batch`} className={labelClass}>
              Lote
            </label>
            <select id={`${ids}-batch`} value={batchId} onChange={(e) => setBatchId(e.target.value)} className={`${inputClass} w-56`}>
              <option value="">Toda a propriedade</option>
              {batches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                  {b.archived ? ' (arquivado)' : ''}
                </option>
              ))}
            </select>
          </div>
          <PeriodFilter value={period} onChange={setPeriod} />
        </div>
      </section>

      <FormMessage error={error} />

      {indicators && (
        <div className={loading ? 'space-y-4 opacity-60' : 'space-y-4'} aria-busy={loading}>
          <Tile
            label="Cabeças no escopo"
            value={`${formatNumber(indicators.headCount)}`}
            hint={indicators.batchName ?? 'Toda a propriedade'}
          />

          <div>
            <h2 className="mb-2 text-sm font-semibold text-terra">Reprodução e produtividade</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Tile
                label="Taxa de prenhez"
                value={formatRate(indicators.pregnancyRate)}
                hint={`${formatNumber(indicators.positivePregnancyDiagnoses)} de ${formatNumber(indicators.pregnancyDiagnoses)} diagnósticos positivos`}
              />
              <Tile label="Taxa de natalidade" value={formatRate(indicators.birthRate)} hint={`${formatNumber(indicators.births)} partos`} />
              <Tile label="Taxa de mortalidade" value={formatRate(indicators.mortalityRate)} hint={`${formatNumber(indicators.deaths)} mortes`} />
              <Tile label="Intervalo entre partos" value={formatDays(indicators.averageCalvingIntervalDays)} />
              <Tile label="Ganho médio diário" value={formatGain(indicators.averageDailyGain)} />
            </div>
          </div>

          <div>
            <h2 className="mb-2 text-sm font-semibold text-terra">Custos</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Tile label="Compra" value={formatCurrency(indicators.purchaseCost)} />
              <Tile label="Sanidade" value={formatCurrency(indicators.healthCost)} />
              <Tile label="Despesas gerais" value={formatCurrency(indicators.generalCost)} />
              <Tile label="Custo total" value={formatCurrency(indicators.totalCost)} dark />
              <Tile label="Custo por animal" value={formatMoney(indicators.costPerAnimal)} />
              <Tile label="Custo por kg" value={formatMoney(indicators.costPerKg)} />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
