import { useEffect, useState, type FormEvent } from 'react'
import { activitiesApi, productionApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Activity, ProductionRecord } from '../api/types'
import { useProperty } from '../property/PropertyContext'

const inputClass =
  'w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust'

function today() {
  return new Date().toISOString().slice(0, 10)
}

export function ProductionPage() {
  const { currentProperty } = useProperty()
  const [records, setRecords] = useState<ProductionRecord[]>([])
  const [activities, setActivities] = useState<Activity[]>([])
  const [error, setError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [productName, setProductName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState('kg')
  const [date, setDate] = useState(today)
  const [activityId, setActivityId] = useState('')

  function loadRecords() {
    setListError(null)
    productionApi.list().then(setRecords).catch((err) => setListError(apiErrorMessage(err)))
    activitiesApi.list().then(setActivities).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadRecords()
  }, [currentProperty])

  function startEdit(record: ProductionRecord) {
    setEditingId(record.id)
    setProductName(record.productName)
    setQuantity(String(record.quantity))
    setUnit(record.unit)
    setDate(record.productionDate)
    setActivityId(record.activityId != null ? String(record.activityId) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setProductName('')
    setQuantity('')
    setUnit('kg')
    setDate(today())
    setActivityId('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = {
        productName,
        quantity: Number(quantity),
        unit,
        productionDate: date,
        activityId: activityId ? Number(activityId) : undefined,
      }
      if (editingId) {
        await productionApi.update(editingId, payload)
      } else {
        await productionApi.create(payload)
      }
      cancelEdit()
      loadRecords()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir este registro de produção?')) return
    try {
      await productionApi.remove(id)
      if (editingId === id) cancelEdit()
      loadRecords()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  const activityName = (id: number | null) => activities.find((a) => a.id === id)?.name ?? '-'

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Produção</h1>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-terra">{editingId ? 'Editar registro' : 'Novo registro'}</h2>
          {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
          <input
            required
            placeholder="Produto (ex: Leite, Ovos, Milho)"
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
            className={inputClass}
          />
          <div className="flex gap-2">
            <input
              required
              type="number"
              step="0.001"
              min="0.001"
              placeholder="Quantidade"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className={inputClass}
            />
            <input
              required
              placeholder="Unidade"
              value={unit}
              onChange={(e) => setUnit(e.target.value)}
              className={inputClass}
            />
          </div>
          <input required type="date" value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
          <select value={activityId} onChange={(e) => setActivityId(e.target.value)} className={inputClass}>
            <option value="">Atividade (opcional)</option>
            {activities.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
          <div className="flex gap-2">
            <button className="flex-1 rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
              {editingId ? 'Salvar' : 'Registrar'}
            </button>
            {editingId && (
              <button
                type="button"
                onClick={cancelEdit}
                className="rounded-md border border-terra/15 px-3 py-2 text-sm text-terra hover:bg-terra/5"
              >
                Cancelar
              </button>
            )}
          </div>
        </form>

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Histórico</h2>
          {listError && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-terra/50">
                  <th className="pb-2">Data</th>
                  <th className="pb-2">Produto</th>
                  <th className="pb-2">Atividade</th>
                  <th className="pb-2 text-right">Quantidade</th>
                  <th className="pb-2" />
                </tr>
              </thead>
              <tbody>
                {records.map((r) => (
                  <tr key={r.id} className="border-t border-terra/5">
                    <td className="py-2 text-terra/50">{r.productionDate}</td>
                    <td className="py-2">{r.productName}</td>
                    <td className="py-2 text-terra/50">{activityName(r.activityId)}</td>
                    <td className="py-2 text-right">
                      {r.quantity} {r.unit}
                    </td>
                    <td className="py-2 text-right whitespace-nowrap">
                      <button onClick={() => startEdit(r)} className="text-rust hover:underline">
                        Editar
                      </button>
                      <button onClick={() => handleDelete(r.id)} className="ml-3 text-rust hover:underline">
                        Excluir
                      </button>
                    </td>
                  </tr>
                ))}
                {records.length === 0 && !listError && (
                  <tr>
                    <td colSpan={5} className="py-4 text-center text-terra/40">
                      Nenhum registro de produção.
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
