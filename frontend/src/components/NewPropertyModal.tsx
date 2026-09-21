import { useState, type FormEvent } from 'react'
import { propertiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'

interface NewPropertyModalProps {
  organizationId: number
  onClose: () => void
  onCreated: (propertyId: number) => void
}

export function NewPropertyModal({ organizationId, onClose, onCreated }: NewPropertyModalProps) {
  const [name, setName] = useState('')
  const [location, setLocation] = useState('')
  const [totalAreaHa, setTotalAreaHa] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const created = await propertiesApi.create({
        organizationId,
        name,
        location: location || undefined,
        totalAreaHa: totalAreaHa ? Number(totalAreaHa) : undefined,
      })
      onCreated(created.id)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center bg-terra/40 px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm rounded-2xl border border-terra/10 bg-white p-6 shadow-sm"
      >
        <h2 className="mb-1 font-serif text-2xl font-semibold text-terra">Nova propriedade</h2>
        <p className="mb-4 text-sm text-terra/60">Adicione outra propriedade à sua conta.</p>

        {error && <p className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

        <label className="mb-1 block text-sm font-medium text-terra">Nome da propriedade</label>
        <input
          required
          autoFocus
          placeholder="Ex: Fazenda Vista Alegre"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <label className="mb-1 block text-sm font-medium text-terra">Localização</label>
        <input
          placeholder="Cidade, Estado"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <label className="mb-1 block text-sm font-medium text-terra">Área total (ha)</label>
        <input
          type="number"
          step="0.01"
          min="0"
          placeholder="0"
          value={totalAreaHa}
          onChange={(e) => setTotalAreaHa(e.target.value)}
          className="mb-6 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-md border border-terra/15 px-3 py-2 text-sm text-terra hover:bg-terra/5"
          >
            Cancelar
          </button>
          <button
            disabled={loading}
            className="flex-1 rounded-md bg-rust px-3 py-2 text-sm font-semibold text-white hover:bg-rust-dark disabled:opacity-60"
          >
            {loading ? 'Criando...' : 'Criar'}
          </button>
        </div>
      </form>
    </div>
  )
}
