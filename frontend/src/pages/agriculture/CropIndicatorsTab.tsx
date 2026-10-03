import { useEffect, useId, useState } from 'react'
import { cropIndicatorsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { CropCycle, CropIndicators, Field } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { PeriodFilter, presetPeriod, type Period } from '../../components/PeriodFilter'
import { inputClass } from '../../components/ui'
import { formatCurrency, formatNumber } from '../../lib/format'
import { cardClass, labelClass } from './shared'

function cycleLabel(cycle: CropCycle) {
  return `${cycle.cropName} · ${cycle.fieldName}`
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

export function CropIndicatorsTab({ cycles, fields }: { cycles: CropCycle[]; fields: Field[] }) {
  const ids = useId()
  // Um só seletor de escopo: a API dá prioridade à safra sobre o talhão, então oferecer os dois juntos
  // evita a combinação ambígua de escolher ambos ao mesmo tempo.
  const [scope, setScope] = useState('')
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))
  const [indicators, setIndicators] = useState<CropIndicators | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [kind, rawId] = scope ? scope.split(':') : [null, null]
  const cropCycleId = kind === 'c' ? Number(rawId) : undefined
  const fieldId = kind === 'f' ? Number(rawId) : undefined

  useEffect(() => {
    setLoading(true)
    setError(null)
    cropIndicatorsApi
      .get({ cropCycleId, fieldId, from: period.from, to: period.to })
      .then(setIndicators)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [cropCycleId, fieldId, period])

  const units = indicators ? Object.keys(indicators.harvestedQuantityByUnit).sort() : []

  return (
    <div className="space-y-4">
      <section className={cardClass}>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <label htmlFor={`${ids}-scope`} className={labelClass}>
              Escopo
            </label>
            <select id={`${ids}-scope`} value={scope} onChange={(e) => setScope(e.target.value)} className={`${inputClass} w-56`}>
              <option value="">Toda a propriedade</option>
              <optgroup label="Talhões">
                {fields.map((f) => (
                  <option key={`f:${f.id}`} value={`f:${f.id}`}>
                    {f.name}
                    {f.archived ? ' (arquivado)' : ''}
                  </option>
                ))}
              </optgroup>
              <optgroup label="Safras">
                {cycles.map((c) => (
                  <option key={`c:${c.id}`} value={`c:${c.id}`}>
                    {cycleLabel(c)}
                    {c.archived ? ' (arquivada)' : ''}
                  </option>
                ))}
              </optgroup>
            </select>
          </div>
          <PeriodFilter value={period} onChange={setPeriod} />
        </div>
      </section>

      <FormMessage error={error} />

      {indicators && (
        <div className={loading ? 'space-y-4 opacity-60' : 'space-y-4'} aria-busy={loading}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Tile
              label="Safras no escopo"
              value={`${formatNumber(indicators.cycleCount)}`}
              hint={indicators.cropCycleLabel ?? indicators.fieldName ?? 'Toda a propriedade'}
            />
            <Tile label="Área total" value={`${formatNumber(indicators.totalAreaHa)} ha`} />
          </div>

          <div>
            <h2 className="mb-2 text-sm font-semibold text-terra">Produção por ha</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {units.length === 0 ? (
                <Tile label="Produção por ha" value="sem dados" />
              ) : (
                units.map((unit) => (
                  <Tile
                    key={unit}
                    label={`Produção por ha (${unit})`}
                    value={`${formatNumber(indicators.productionPerHaByUnit[unit] ?? 0)} ${unit}/ha`}
                    hint={`${formatNumber(indicators.harvestedQuantityByUnit[unit])} ${unit} colhidos no total`}
                  />
                ))
              )}
            </div>
          </div>

          <div>
            <h2 className="mb-2 text-sm font-semibold text-terra">Custos e receita</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Tile label="Custo total" value={formatCurrency(indicators.totalCost)} />
              <Tile label="Custo por ha" value={formatMoney(indicators.costPerHa)} />
              <Tile label="Receita (vendida)" value={formatCurrency(indicators.revenue)} hint="Só colheitas vendidas; o que foi para o estoque não conta aqui." />
              <Tile label="Receita por ha" value={formatMoney(indicators.revenuePerHa)} />
              <Tile label="Margem por ha" value={formatMoney(indicators.marginPerHa)} dark />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
