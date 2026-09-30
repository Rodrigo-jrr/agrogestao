import { useId } from 'react'
import { toIsoDate } from '../lib/format'
import { inputClass } from './ui'

export interface Period {
  from: string
  to: string
}

type PresetKey = 'thisMonth' | 'lastMonth' | 'last3Months' | 'thisYear'

const presets: { key: PresetKey; label: string }[] = [
  { key: 'thisMonth', label: 'Este mês' },
  { key: 'lastMonth', label: 'Mês passado' },
  { key: 'last3Months', label: 'Últimos 3 meses' },
  { key: 'thisYear', label: 'Este ano' },
]

export function presetPeriod(key: PresetKey, today = new Date()): Period {
  const y = today.getFullYear()
  const m = today.getMonth()
  switch (key) {
    case 'thisMonth':
      return { from: toIsoDate(new Date(y, m, 1)), to: toIsoDate(new Date(y, m + 1, 0)) }
    case 'lastMonth':
      return { from: toIsoDate(new Date(y, m - 1, 1)), to: toIsoDate(new Date(y, m, 0)) }
    case 'last3Months':
      return { from: toIsoDate(new Date(y, m - 2, 1)), to: toIsoDate(new Date(y, m + 1, 0)) }
    case 'thisYear':
      return { from: toIsoDate(new Date(y, 0, 1)), to: toIsoDate(new Date(y, 11, 31)) }
  }
}

/** Atalhos de período + datas personalizadas. */
export function PeriodFilter({ value, onChange }: { value: Period; onChange: (period: Period) => void }) {
  const id = useId()
  const active = presets.find((p) => {
    const period = presetPeriod(p.key)
    return period.from === value.from && period.to === value.to
  })?.key

  return (
    <div className="flex flex-wrap items-end gap-x-3 gap-y-2" role="group" aria-label="Período">
      <div className="flex flex-wrap gap-1">
        {presets.map((p) => (
          <button
            key={p.key}
            type="button"
            aria-pressed={active === p.key}
            onClick={() => onChange(presetPeriod(p.key))}
            className={`cursor-pointer rounded-full border px-3 py-1 text-xs font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
              active === p.key ? 'border-rust bg-rust text-terreiro' : 'border-terra/15 text-terra/70 hover:bg-terra/5'
            }`}
          >
            {p.label}
          </button>
        ))}
      </div>
      <div className="flex items-end gap-2 text-xs text-terra/60">
        <label htmlFor={`${id}-from`}>
          De
          <input
            id={`${id}-from`}
            type="date"
            value={value.from}
            max={value.to}
            onChange={(e) => e.target.value && onChange({ ...value, from: e.target.value })}
            className={`${inputClass} mt-0.5 py-1`}
          />
        </label>
        <label htmlFor={`${id}-to`}>
          Até
          <input
            id={`${id}-to`}
            type="date"
            value={value.to}
            min={value.from}
            onChange={(e) => e.target.value && onChange({ ...value, to: e.target.value })}
            className={`${inputClass} mt-0.5 py-1`}
          />
        </label>
      </div>
    </div>
  )
}
