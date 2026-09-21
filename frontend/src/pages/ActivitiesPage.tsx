import { useEffect, useState, type FormEvent } from 'react'
import { activitiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Activity } from '../api/types'
import { useProperty } from '../property/PropertyContext'

export function ActivitiesPage() {
  const { currentProperty } = useProperty()
  const [activities, setActivities] = useState<Activity[]>([])
  const [error, setError] = useState<string | null>(null)

  const [name, setName] = useState('')
  const [type, setType] = useState('')
  const [areaHa, setAreaHa] = useState('')

  function loadActivities() {
    activitiesApi.list().then(setActivities).catch(() => {})
  }

  useEffect(() => {
    if (currentProperty) loadActivities()
  }, [currentProperty])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await activitiesApi.create({ name, type, areaHa: areaHa ? Number(areaHa) : undefined })
      setName('')
      setType('')
      setAreaHa('')
      loadActivities()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Atividades</h1>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-terra">Nova atividade</h2>
          {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
          <input
            required
            placeholder="Nome (ex: Produção de leite)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <input
            required
            placeholder="Tipo (ex: PECUARIA, AGRICULTURA)"
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <input
            type="number"
            step="0.01"
            placeholder="Área (ha)"
            value={areaHa}
            onChange={(e) => setAreaHa(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <button className="w-full rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
            Adicionar
          </button>
        </form>

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Atividades cadastradas</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Nome</th>
                <th className="pb-2">Tipo</th>
                <th className="pb-2 text-right">Área (ha)</th>
              </tr>
            </thead>
            <tbody>
              {activities.map((a) => (
                <tr key={a.id} className="border-t border-terra/5">
                  <td className="py-2">{a.name}</td>
                  <td className="py-2 text-terra/50">{a.type}</td>
                  <td className="py-2 text-right">{a.areaHa ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
