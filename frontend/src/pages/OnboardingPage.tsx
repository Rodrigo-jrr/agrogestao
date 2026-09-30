import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { onboardingApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useProperty } from '../property/PropertyContext'
import { Logo } from '../components/Logo'
import { inputClass, primaryButtonClass } from '../components/ui'
import { activityTypeCode } from '../lib/activityTypes'

interface ActivityRow {
  name: string
  type: string
  areaHa: string
}

const suggestedActivities = ['Pecuária', 'Agricultura', 'Leite', 'Horta', 'Avicultura', 'Piscicultura']

export function OnboardingPage() {
  const navigate = useNavigate()
  const { refresh } = useProperty()
  const [organizationName, setOrganizationName] = useState('')
  const [propertyName, setPropertyName] = useState('')
  const [location, setLocation] = useState('')
  const [totalAreaHa, setTotalAreaHa] = useState('')
  const [activities, setActivities] = useState<ActivityRow[]>([{ name: 'Pecuária', type: 'PECUARIA', areaHa: '' }])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  function addActivity(name: string) {
    setActivities((current) => [...current, { name, type: activityTypeCode(name), areaHa: '' }])
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
      localStorage.setItem('agro.propertyId', String(created.id))
      await refresh()
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

        <label htmlFor="onboarding-org-name" className="mb-1 block text-sm font-medium text-terra">Nome da conta/organização</label>
        <input
          id="onboarding-org-name"
          required
          value={organizationName}
          onChange={(e) => setOrganizationName(e.target.value)}
          className={`mb-4 ${inputClass}`}
        />

        <label htmlFor="onboarding-property-name" className="mb-1 block text-sm font-medium text-terra">Nome da propriedade</label>
        <input
          id="onboarding-property-name"
          required
          placeholder="Ex: Fazenda Vista Alegre"
          value={propertyName}
          onChange={(e) => setPropertyName(e.target.value)}
          className={`mb-4 ${inputClass}`}
        />

        <div className="mb-4 grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="onboarding-location" className="mb-1 block text-sm font-medium text-terra">Localização</label>
            <input
              id="onboarding-location"
              placeholder="Cidade, Estado"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor="onboarding-area" className="mb-1 block text-sm font-medium text-terra">Área total (ha)</label>
            <input
              id="onboarding-area"
              type="number"
              step="0.01"
              placeholder="0"
              value={totalAreaHa}
              onChange={(e) => setTotalAreaHa(e.target.value)}
              className={inputClass}
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
                aria-label={`Nome da atividade ${index + 1}`}
                required
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
                aria-label={`Área em hectares da atividade ${index + 1}`}
                min="0"
                placeholder="Área (ha)"
                className="w-28 rounded-md border border-terra/15 bg-white px-3 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
              />
              <button
                type="button"
                onClick={() => removeActivity(index)}
                aria-label={`Remover atividade ${row.name || index + 1}`}
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
          className={`w-full ${primaryButtonClass}`}
        >
          {loading ? 'Criando...' : 'Continuar'}
        </button>
      </form>
    </div>
  )
}
