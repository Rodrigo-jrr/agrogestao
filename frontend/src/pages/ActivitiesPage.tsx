import { useEffect, useState, type FormEvent } from 'react'
import { activitiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Activity } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'

export function ActivitiesPage() {
  const { currentProperty } = useProperty()
  const [activities, setActivities] = useState<Activity[]>([])
  const [error, setError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [type, setType] = useState('')
  const [areaHa, setAreaHa] = useState('')

  function loadActivities() {
    setListError(null)
    activitiesApi.list().then(setActivities).catch((err) => setListError(apiErrorMessage(err)))
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

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir esta atividade?')) return
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
          <input
            required
            aria-label="Nome"
            placeholder="Nome (ex: Produção de leite)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
          <input
            required
            aria-label="Tipo"
            placeholder="Tipo (ex: PECUARIA, AGRICULTURA)"
            value={type}
            onChange={(e) => setType(e.target.value)}
            className={inputClass}
          />
          <input
            type="number"
            step="0.01"
            aria-label="Área (ha)"
            placeholder="Área (ha)"
            value={areaHa}
            onChange={(e) => setAreaHa(e.target.value)}
            className={inputClass}
          />
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
          <div className="overflow-x-auto">
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
                    <td className="py-2 text-terra/50">{a.type}</td>
                    <td className="py-2 text-right">{a.areaHa ?? '-'}</td>
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
                      Nenhuma atividade cadastrada.
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
