import { useId, useState, type FormEvent } from 'react'
import { machinesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Machine, MachineType, MeasurementType } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { fieldLabelClass, inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../../components/ui'
import { formatNumber } from '../../lib/format'
import { cardClass, measurementTypeLabels } from './shared'

const measurements: MeasurementType[] = ['HOROMETRO', 'ODOMETRO']

const radio = (checked: boolean) =>
  `flex cursor-pointer items-center justify-center rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
    checked ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
  }`

function MachineForm({
  machine,
  types,
  onSaved,
  onCancel,
}: {
  machine?: Machine
  types: MachineType[]
  onSaved: () => void
  onCancel?: () => void
}) {
  const ids = useId()
  const activeTypes = types.filter((t) => !t.archived)
  const [name, setName] = useState(machine?.name ?? '')
  const [machineTypeId, setMachineTypeId] = useState(machine ? String(machine.machineTypeId) : '')
  const [identification, setIdentification] = useState(machine?.identification ?? '')
  const [measurementType, setMeasurementType] = useState<MeasurementType>(machine?.measurementType ?? 'HOROMETRO')
  const [currentReading, setCurrentReading] = useState('0')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (machine) {
        await machinesApi.update(machine.id, {
          name,
          machineTypeId: Number(machineTypeId),
          identification: identification || null,
          measurementType,
        })
      } else {
        await machinesApi.create({
          name,
          machineTypeId: Number(machineTypeId),
          identification: identification || null,
          measurementType,
          currentReading: currentReading ? Number(currentReading) : 0,
        })
        setName('')
        setMachineTypeId('')
        setIdentification('')
        setMeasurementType('HOROMETRO')
        setCurrentReading('0')
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
          Nome
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={150}
          placeholder="Ex.: Trator 1, Pulverizador novo"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${ids}-type`} className={fieldLabelClass}>
          Tipo
        </label>
        <select id={`${ids}-type`} required value={machineTypeId} onChange={(e) => setMachineTypeId(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {activeTypes.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </div>
      <div>
        <label htmlFor={`${ids}-id`} className={fieldLabelClass}>
          Identificação <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-id`}
          maxLength={60}
          placeholder="Placa, nº de série…"
          value={identification}
          onChange={(e) => setIdentification(e.target.value)}
          className={inputClass}
        />
      </div>
      <fieldset>
        <legend className={fieldLabelClass}>Medição</legend>
        <div className="grid grid-cols-2 gap-1">
          {measurements.map((m) => (
            <label key={m} className={radio(measurementType === m)}>
              <input
                type="radio"
                name={`${ids}-measurement`}
                checked={measurementType === m}
                onChange={() => setMeasurementType(m)}
                className="sr-only"
              />
              {measurementTypeLabels[m]}
            </label>
          ))}
        </div>
      </fieldset>
      {!machine && (
        <div>
          <label htmlFor={`${ids}-reading`} className={fieldLabelClass}>
            Leitura inicial {measurementType === 'HOROMETRO' ? '(horas)' : '(km)'}{' '}
            <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input
            id={`${ids}-reading`}
            type="number"
            min="0"
            step="0.01"
            inputMode="decimal"
            value={currentReading}
            onChange={(e) => setCurrentReading(e.target.value)}
            className={inputClass}
          />
        </div>
      )}
      <div className="flex gap-2">
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Salvando…' : machine ? 'Salvar' : 'Cadastrar máquina'}
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

function ReadingForm({ machine, onSaved, onCancel }: { machine: Machine; onSaved: () => void; onCancel: () => void }) {
  const ids = useId()
  const [reading, setReading] = useState(String(machine.currentReading))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (Number(reading) < machine.currentReading) {
      setError(`A leitura não pode ser menor que a atual (${formatNumber(machine.currentReading)}).`)
      return
    }
    setSaving(true)
    try {
      await machinesApi.updateReading(machine.id, { reading: Number(reading) })
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mt-2 space-y-2 rounded-lg border border-terra/10 bg-terreiro/40 p-3">
      <FormMessage error={error} />
      <p className="text-xs text-terra/60">
        Registrar leitura atual de {measurementTypeLabels[machine.measurementType].toLowerCase()}: hoje está em{' '}
        {formatNumber(machine.currentReading)}.
      </p>
      <div className="flex flex-wrap items-end gap-2">
        <div className="min-w-0 flex-1">
          <label htmlFor={`${ids}-reading`} className={fieldLabelClass}>
            Nova leitura
          </label>
          <input
            id={`${ids}-reading`}
            required
            type="number"
            step="0.01"
            inputMode="decimal"
            value={reading}
            onChange={(e) => setReading(e.target.value)}
            className={inputClass}
          />
        </div>
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Salvando…' : 'Salvar leitura'}
        </button>
        <button type="button" onClick={onCancel} className={`cursor-pointer ${outlineButtonClass}`}>
          Cancelar
        </button>
      </div>
    </form>
  )
}

export function MachinesTab({
  machines,
  types,
  canWrite,
  canManage,
  onChanged,
}: {
  machines: Machine[]
  types: MachineType[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [readingId, setReadingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  const visible = machines.filter((m) => showArchived || !m.archived)
  const hasActiveTypes = types.some((t) => !t.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(m: Machine) {
    const ok = await confirm({
      title: `Excluir a máquina ${m.name}?`,
      message: 'A máquina é removida definitivamente, junto com a leitura registrada.',
      confirmLabel: 'Excluir máquina',
    })
    if (ok) run(() => machinesApi.remove(m.id))
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova máquina</h2>
          {hasActiveTypes ? (
            <MachineForm types={types} onSaved={onChanged} />
          ) : (
            <p className="text-sm text-terra/60">Cadastre um tipo de máquina antes de registrar a primeira máquina.</p>
          )}
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Máquinas</h2>
          <label className="flex items-center gap-2 text-xs text-terra/60">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
            Mostrar arquivadas
          </label>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">Nenhuma máquina cadastrada ainda.</p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((m) => (
              <li key={m.id} className={`py-3 ${m.archived ? 'opacity-60' : ''}`}>
                {editingId === m.id ? (
                  <MachineForm
                    machine={m}
                    types={types}
                    onCancel={() => setEditingId(null)}
                    onSaved={() => {
                      setEditingId(null)
                      onChanged()
                    }}
                  />
                ) : (
                  <>
                    <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                      <div>
                        <p className="font-medium text-terra">
                          {m.name}
                          {m.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivada</span>}
                        </p>
                        <p className="text-xs text-terra/55">
                          {m.machineTypeName}
                          {m.identification && ` · ${m.identification}`}
                        </p>
                      </div>
                      <p className="font-serif text-lg font-semibold text-terra tabular-nums">
                        {formatNumber(m.currentReading)}{' '}
                        <span className="text-sm font-normal text-terra/55">
                          {m.measurementType === 'HOROMETRO' ? 'h' : 'km'}
                        </span>
                      </p>
                    </div>
                    <div className="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-sm">
                      {canWrite && (
                        <button onClick={() => setEditingId(m.id)} className={`cursor-pointer ${linkNeutralClass}`}>
                          Editar
                        </button>
                      )}
                      {canWrite && !m.archived && (
                        <button onClick={() => setReadingId(readingId === m.id ? null : m.id)} className={`cursor-pointer ${linkNeutralClass}`}>
                          Registrar leitura
                        </button>
                      )}
                      {canManage &&
                        (m.archived ? (
                          <button onClick={() => run(() => machinesApi.unarchive(m.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Reativar
                          </button>
                        ) : (
                          <button onClick={() => run(() => machinesApi.archive(m.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                            Arquivar
                          </button>
                        ))}
                      {canManage && (
                        <button onClick={() => handleDelete(m)} className={`cursor-pointer ${linkDangerClass}`}>
                          Excluir
                        </button>
                      )}
                    </div>
                    {readingId === m.id && (
                      <ReadingForm
                        machine={m}
                        onCancel={() => setReadingId(null)}
                        onSaved={() => {
                          setReadingId(null)
                          onChanged()
                        }}
                      />
                    )}
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
