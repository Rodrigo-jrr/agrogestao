import { useId, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { cropCyclesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Crop, CropCycle, Field } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { fieldLabelClass, inputClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { formatDate } from '../../lib/format'
import { cardClass } from './shared'

function CropCycleForm({
  fields,
  crops,
  onSaved,
}: {
  fields: Field[]
  crops: Crop[]
  onSaved: () => void
}) {
  const ids = useId()
  const activeFields = fields.filter((f) => !f.archived)
  const activeCrops = crops.filter((c) => !c.archived)
  const [fieldId, setFieldId] = useState('')
  const [cropId, setCropId] = useState('')
  const [variety, setVariety] = useState('')
  const [plannedPlantingDate, setPlannedPlantingDate] = useState('')
  const [plannedHarvestDate, setPlannedHarvestDate] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (plannedHarvestDate && plannedHarvestDate < plannedPlantingDate) {
      setError('A data de colheita prevista não pode ser antes da data de plantio.')
      return
    }
    setSaving(true)
    try {
      await cropCyclesApi.create({
        fieldId: Number(fieldId),
        cropId: Number(cropId),
        variety: variety || null,
        plannedPlantingDate,
        plannedHarvestDate: plannedHarvestDate || null,
      })
      setFieldId('')
      setCropId('')
      setVariety('')
      setPlannedPlantingDate('')
      setPlannedHarvestDate('')
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
        <label htmlFor={`${ids}-field`} className={fieldLabelClass}>
          Talhão
        </label>
        <select id={`${ids}-field`} required value={fieldId} onChange={(e) => setFieldId(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {activeFields.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor={`${ids}-crop`} className={fieldLabelClass}>
          Cultura
        </label>
        <select id={`${ids}-crop`} required value={cropId} onChange={(e) => setCropId(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {activeCrops.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor={`${ids}-variety`} className={fieldLabelClass}>
          Variedade <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-variety`}
          maxLength={100}
          placeholder="Ex.: BRS 1010"
          value={variety}
          onChange={(e) => setVariety(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-planting`} className={fieldLabelClass}>
          Plantio previsto
        </label>
        <input
          id={`${ids}-planting`}
          required
          type="date"
          value={plannedPlantingDate}
          onChange={(e) => setPlannedPlantingDate(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-harvest`} className={fieldLabelClass}>
          Colheita prevista <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-harvest`}
          type="date"
          value={plannedHarvestDate}
          onChange={(e) => setPlannedHarvestDate(e.target.value)}
          className={inputClass}
        />
      </div>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Criar safra'}
      </button>
    </form>
  )
}

export function CropCyclesTab({
  cycles,
  fields,
  crops,
  canWrite,
  canManage,
  onChanged,
}: {
  cycles: CropCycle[]
  fields: Field[]
  crops: Crop[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const [fieldFilter, setFieldFilter] = useState('')

  const fieldOptions = useMemo(() => {
    const seen = new Map<number, string>()
    for (const c of cycles) seen.set(c.fieldId, c.fieldName)
    return [...seen.entries()].sort((a, b) => a[1].localeCompare(b[1]))
  }, [cycles])

  const visible = cycles.filter(
    (c) => (showArchived || !c.archived) && (!fieldFilter || String(c.fieldId) === fieldFilter),
  )

  const hasActiveFields = fields.some((f) => !f.archived)
  const hasActiveCrops = crops.some((c) => !c.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova safra</h2>
          {hasActiveFields && hasActiveCrops ? (
            <CropCycleForm fields={fields} crops={crops} onSaved={onChanged} />
          ) : (
            <div className="rounded-xl border border-dashed border-terra/20 bg-terreiro/40 p-4 text-sm text-terra/70">
              <p>
                {!hasActiveFields && !hasActiveCrops
                  ? 'Cadastre um talhão e uma cultura para começar a registrar safras.'
                  : !hasActiveFields
                    ? 'Cadastre um talhão para começar a registrar safras.'
                    : 'Cadastre uma cultura para começar a registrar safras.'}
              </p>
              <div className="mt-2 flex flex-col gap-1">
                {!hasActiveFields && (
                  <Link to="/agricultura?aba=talhoes" className="font-medium text-rust hover:underline">
                    Ir para Talhões →
                  </Link>
                )}
                {!hasActiveCrops && (
                  <Link to="/agricultura?aba=culturas" className="font-medium text-rust hover:underline">
                    Ir para Culturas →
                  </Link>
                )}
              </div>
            </div>
          )}
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Safras</h2>
          <div className="flex flex-wrap items-center gap-3">
            {fieldOptions.length > 0 && (
              <select
                aria-label="Filtrar por talhão"
                value={fieldFilter}
                onChange={(e) => setFieldFilter(e.target.value)}
                className={`${inputClass} w-auto`}
              >
                <option value="">Todos os talhões</option>
                {fieldOptions.map(([id, name]) => (
                  <option key={id} value={id}>
                    {name}
                  </option>
                ))}
              </select>
            )}
            <label className="flex items-center gap-2 text-xs text-terra/60">
              <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
              Mostrar arquivadas
            </label>
          </div>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">Nenhuma safra cadastrada ainda.</p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((c) => (
              <li key={c.id} className={`py-3 ${c.archived ? 'opacity-60' : ''}`}>
                <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                  <div>
                    <p className="font-medium text-terra">
                      {c.cropName} · {c.fieldName}
                      {c.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivada</span>}
                    </p>
                    <p className="text-xs text-terra/55">
                      {c.variety && `${c.variety} · `}
                      Plantio {formatDate(c.plannedPlantingDate)}
                      {c.plannedHarvestDate && ` · Colheita prevista ${formatDate(c.plannedHarvestDate)}`}
                    </p>
                    {canManage && (
                      <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm">
                        {c.archived ? (
                          <button onClick={() => run(() => cropCyclesApi.unarchive(c.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Reativar
                          </button>
                        ) : (
                          <button onClick={() => run(() => cropCyclesApi.archive(c.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Arquivar
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
