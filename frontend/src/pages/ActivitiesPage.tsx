import { useEffect, useId, useState, type FormEvent } from 'react'
import { activitiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Activity } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { fieldLabelClass, inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { activityTypeLabel, activityTypes } from '../lib/activityTypes'
import { formatNumber } from '../lib/format'
import { useConfirm } from '../components/ConfirmDialog'

export function ActivitiesPage() {
  const { currentProperty } = useProperty()
  const [activities, setActivities] = useState<Activity[]>([])
  const [error, setError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const formId = useId()

  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [type, setType] = useState('')
  const [areaHa, setAreaHa] = useState('')

  function loadActivities() {
    setListError(null)
    activitiesApi
      .list()
      .then(setActivities)
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (currentProperty) loadActivities()
  }, [currentProperty])

  function startEdit(activity: Activity) {
    setEditingId(activity.id)
    setName(activity.name)
    setType(activity.type)
    setAreaHa(activity.areaHa != null ? String(activity.areaHa) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setName('')
    setType('')
    setAreaHa('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = { name, type, areaHa: areaHa ? Number(areaHa) : undefined }
      if (editingId) {
        await activitiesApi.update(editingId, payload)
      } else {
        await activitiesApi.create(payload)
      }
      cancelEdit()
      loadActivities()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const confirm = useConfirm()

  async function handleDelete(id: number) {
    const activity = activities.find((a) => a.id === id)
    const ok = await confirm({
      title: `Excluir a atividade ${activity?.name ?? ''}?`,
      message: 'Esta ação não pode ser desfeita. Se houver produção ou centro de custo ligado a ela, a exclusão será recusada.',
      confirmLabel: 'Excluir atividade',
    })
    if (!ok) return
    try {
      await activitiesApi.remove(id)
      if (editingId === id) cancelEdit()
      loadActivities()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Atividades</h1>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-terra">{editingId ? 'Editar atividade' : 'Nova atividade'}</h2>
          {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
          <div>
            <label htmlFor={`${formId}-name`} className={fieldLabelClass}>
              Nome
            </label>
            <input
              id={`${formId}-name`}
              required
              maxLength={150}
              placeholder="Ex.: Gado de leite, Milho safra"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor={`${formId}-type`} className={fieldLabelClass}>
              Tipo
            </label>
            <select id={`${formId}-type`} required value={type} onChange={(e) => setType(e.target.value)} className={inputClass}>
              <option value="">Selecione</option>
              {activityTypes.map((t) => (
                <option key={t.code} value={t.code}>
                  {t.label}
                </option>
              ))}
              {type && !activityTypes.some((t) => t.code === type) && <option value={type}>{activityTypeLabel(type)}</option>}
            </select>
          </div>
          <div>
            <label htmlFor={`${formId}-area`} className={fieldLabelClass}>
              Área (ha) <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input
              id={`${formId}-area`}
              type="number"
              min="0"
              step="0.01"
              value={areaHa}
              onChange={(e) => setAreaHa(e.target.value)}
              className={inputClass}
            />
          </div>
          <div className="flex gap-2">
            <button className={`flex-1 ${primaryButtonClass}`}>
              {editingId ? 'Salvar' : 'Adicionar'}
            </button>
            {editingId && (
              <button type="button" onClick={cancelEdit} className={outlineButtonClass}>
                Cancelar
              </button>
            )}
          </div>
        </form>

        <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm md:col-span-2">
          <h2 className="mb-3 text-sm font-semibold text-terra">Atividades cadastradas</h2>
          {listError && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}
          <div className="relative overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-terra/50">
                  <th className="pb-2">Nome</th>
                  <th className="pb-2">Tipo</th>
                  <th className="pb-2 text-right">Área (ha)</th>
                  <th className="pb-2" />
                </tr>
              </thead>
              <tbody>
                {activities.map((a) => (
                  <tr key={a.id} className="border-t border-terra/5">
                    <td className="py-2">{a.name}</td>
                    <td className="py-2 text-terra/60">{activityTypeLabel(a.type)}</td>
                    <td className="py-2 text-right tabular-nums">{a.areaHa != null ? formatNumber(a.areaHa) : '—'}</td>
                    <td className="py-2 text-right whitespace-nowrap">
                      <button onClick={() => startEdit(a)} className={linkNeutralClass}>
                        Editar
                      </button>
                      <button onClick={() => handleDelete(a.id)} className={`ml-3 ${linkDangerClass}`}>
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
                {activities.length === 0 && !listError && (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-terra/40">
                      {loading ? 'Carregando…' : 'Nenhuma atividade cadastrada ainda.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}
