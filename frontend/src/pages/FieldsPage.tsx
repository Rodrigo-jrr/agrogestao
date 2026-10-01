import { useEffect, useId, useState, type FormEvent } from 'react'
import { fieldsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Field } from '../api/types'
import { FormMessage } from '../components/FormMessage'
import { fieldLabelClass, inputClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { formatNumber } from '../lib/format'
import { useProperty } from '../property/PropertyContext'

const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'

function FieldForm({ field, onSaved, onCancel }: { field?: Field; onSaved: () => void; onCancel?: () => void }) {
  const ids = useId()
  const [name, setName] = useState(field?.name ?? '')
  const [areaHa, setAreaHa] = useState(field ? String(field.areaHa) : '')
  const [location, setLocation] = useState(field?.location ?? '')
  const [soilType, setSoilType] = useState(field?.soilType ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    const data = { name, areaHa: Number(areaHa), location: location || null, soilType: soilType || null }
    try {
      if (field) await fieldsApi.update(field.id, data)
      else await fieldsApi.create(data)
      if (!field) {
        setName('')
        setAreaHa('')
        setLocation('')
        setSoilType('')
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
        <label htmlFor={`${ids}-name`} className={fieldLabelClass}>
          Nome do talhão
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={150}
          placeholder="Ex.: Talhão 1, Fundo da sede"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-area`} className={fieldLabelClass}>
          Área (ha)
        </label>
        <input
          id={`${ids}-area`}
          required
          type="number"
          min="0.01"
          step="0.01"
          inputMode="decimal"
          value={areaHa}
          onChange={(e) => setAreaHa(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-location`} className={fieldLabelClass}>
          Localização <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-location`}
          maxLength={255}
          placeholder="Ex.: Fundo da propriedade, perto da sede"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-soil`} className={fieldLabelClass}>
          Tipo de solo <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-soil`}
          maxLength={100}
          placeholder="Ex.: Argiloso, Arenoso"
          value={soilType}
          onChange={(e) => setSoilType(e.target.value)}
          className={inputClass}
        />
      </div>
      <div className="flex gap-2">
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Salvando…' : field ? 'Salvar' : 'Criar talhão'}
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

export function FieldsPage() {
  const { currentProperty } = useProperty()
  const [fields, setFields] = useState<Field[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  const role = currentProperty?.role
  const canWrite = role === 'OWNER' || role === 'MANAGER' || role === 'WORKER'
  const canManage = role === 'OWNER' || role === 'MANAGER'

  function load() {
    fieldsApi.list(true).then(setFields).catch((err) => setError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) load()
  }, [currentProperty])

  const visible = fields.filter((f) => showArchived || !f.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      load()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Agricultura</h1>

      <div className="grid gap-4 lg:grid-cols-3">
        {canWrite && (
          <section className={`${cardClass} min-w-0 self-start`}>
            <h2 className="mb-3 text-sm font-semibold text-terra">Novo talhão</h2>
            <FieldForm onSaved={load} />
          </section>
        )}
        <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
          <div className="mb-3 flex items-center justify-between gap-2">
            <h2 className="text-sm font-semibold text-terra">Talhões</h2>
            <label className="flex items-center gap-2 text-xs text-terra/60">
              <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
              Mostrar arquivados
            </label>
          </div>
          <FormMessage error={error} />
          {visible.length === 0 ? (
            <p className="py-6 text-center text-sm text-terra/50">
              Nenhum talhão cadastrado ainda. Talhões são as áreas da propriedade onde depois você vai registrar cultura e safra.
            </p>
          ) : (
            <ul className="divide-y divide-terra/5">
              {visible.map((f) => (
                <li key={f.id} className={`py-3 ${f.archived ? 'opacity-60' : ''}`}>
                  {editingId === f.id ? (
                    <FieldForm
                      field={f}
                      onCancel={() => setEditingId(null)}
                      onSaved={() => {
                        setEditingId(null)
                        load()
                      }}
                    />
                  ) : (
                    <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                      <div>
                        <p className="font-medium text-terra">
                          {f.name}
                          {f.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivado</span>}
                        </p>
                        {(f.location || f.soilType) && (
                          <p className="text-xs text-terra/55">{[f.location, f.soilType].filter(Boolean).join(' · ')}</p>
                        )}
                        <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm">
                          {canWrite && (
                            <button onClick={() => setEditingId(f.id)} className={`cursor-pointer ${linkNeutralClass}`}>
                              Editar
                            </button>
                          )}
                          {canManage &&
                            (f.archived ? (
                              <button onClick={() => run(() => fieldsApi.unarchive(f.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                                Reativar
                              </button>
                            ) : (
                              <button onClick={() => run(() => fieldsApi.archive(f.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                                Arquivar
                              </button>
                            ))}
                        </div>
                      </div>
                      <p className="font-serif text-lg font-semibold text-terra tabular-nums">
                        {formatNumber(f.areaHa)} <span className="text-sm font-normal text-terra/55">ha</span>
                      </p>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
