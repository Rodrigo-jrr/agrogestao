import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { onboardingApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useProperty } from '../property/PropertyContext'
import { Logo } from '../components/Logo'

interface ActivityRow {
  name: string
  type: string
  areaHa: string
}

const suggestedActivities = ['Pecuária', 'Agricultura', 'Leite', 'Horta', 'Avicultura', 'Piscicultura']

export function OnboardingPage() {
  const navigate = useNavigate()
  const { refresh, selectProperty } = useProperty()
  const [organizationName, setOrganizationName] = useState('')
  const [propertyName, setPropertyName] = useState('')
  const [location, setLocation] = useState('')
  const [totalAreaHa, setTotalAreaHa] = useState('')
  const [activities, setActivities] = useState<ActivityRow[]>([{ name: 'Pecuária', type: 'PECUARIA', areaHa: '' }])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function addActivity(name: string) {
    setActivities((current) => [...current, { name, type: name.toUpperCase(), areaHa: '' }])
  }

  function updateActivity(index: number, field: keyof ActivityRow, value: string) {
    setActivities((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  function removeActivity(index: number) {
    setActivities((current) => current.filter((_, i) => i !== index))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (activities.length === 0) {
      setError('Selecione ao menos uma atividade')
      return
    }
    setLoading(true)
    try {
      const created = await onboardingApi.create({
        organizationName,
        propertyName,
        location: location || undefined,
        totalAreaHa: totalAreaHa ? Number(totalAreaHa) : undefined,
        activities: activities.map((a) => ({
          name: a.name,
          type: a.type,
          areaHa: a.areaHa ? Number(a.areaHa) : undefined,
        })),
      })
      await refresh()
      selectProperty(created.id)
      navigate('/dashboard')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-terreiro px-4 py-10 font-sans">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg rounded-2xl border border-terra/10 bg-white p-8 shadow-sm"
      >
        <Logo size={24} className="mb-6" />

        <h1 className="mb-1 font-serif text-3xl font-semibold text-terra">Criar propriedade</h1>
        <p className="mb-6 text-sm text-terra/60">Comece cadastrando os dados básicos da sua propriedade.</p>

        {error && <p className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

        <label className="mb-1 block text-sm font-medium text-terra">Nome da conta/organização</label>
        <input
          required
          value={organizationName}
          onChange={(e) => setOrganizationName(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <label className="mb-1 block text-sm font-medium text-terra">Nome da propriedade</label>
        <input
          required
          placeholder="Ex: Fazenda Vista Alegre"
          value={propertyName}
          onChange={(e) => setPropertyName(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <div className="mb-4 grid grid-cols-2 gap-3">
          <div>
            <label className="mb-1 block text-sm font-medium text-terra">Localização</label>
            <input
              placeholder="Cidade, Estado"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-terra">Área total (ha)</label>
            <input
              type="number"
              step="0.01"
              placeholder="0"
              value={totalAreaHa}
              onChange={(e) => setTotalAreaHa(e.target.value)}
              className="w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
          </div>
        </div>

        <label className="mb-2 block text-sm font-medium text-terra">Atividades da propriedade</label>
        <div className="mb-3 flex flex-wrap gap-2">
          {suggestedActivities.map((label) => (
            <button
              key={label}
              type="button"
              onClick={() => addActivity(label)}
              className="rounded-full border border-trigo/40 px-3 py-1 text-xs text-trigo-dark hover:bg-trigo/10"
            >
              + {label}
            </button>
          ))}
        </div>

        <div className="mb-6 space-y-2">
          {activities.map((row, index) => (
            <div key={index} className="flex items-center gap-2">
              <input
                value={row.name}
                onChange={(e) => updateActivity(index, 'name', e.target.value)}
                placeholder="Nome da atividade"
                className="flex-1 rounded-md border border-terra/15 bg-white px-3 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
              />
              <input
                type="number"
                step="0.01"
                value={row.areaHa}
                onChange={(e) => updateActivity(index, 'areaHa', e.target.value)}
                placeholder="Área (ha)"
                className="w-28 rounded-md border border-terra/15 bg-white px-3 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
              />
              <button
                type="button"
                onClick={() => removeActivity(index)}
                className="text-sm text-rust hover:underline"
              >
                Remover
              </button>
            </div>
          ))}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-rust px-3 py-2.5 text-sm font-semibold text-terreiro hover:bg-rust-dark disabled:opacity-60"
        >
          {loading ? 'Criando...' : 'Continuar'}
        </button>
      </form>
    </div>
  )
}
