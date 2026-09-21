import { useEffect, useState, type FormEvent } from 'react'
import { productionApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { ProductionRecord } from '../api/types'
import { useProperty } from '../property/PropertyContext'

export function ProductionPage() {
  const { currentProperty } = useProperty()
  const [records, setRecords] = useState<ProductionRecord[]>([])
  const [error, setError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [productName, setProductName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState('kg')
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))

  function loadRecords() {
    setListError(null)
    productionApi.list().then(setRecords).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadRecords()
  }, [currentProperty])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await productionApi.create({ productName, quantity: Number(quantity), unit, productionDate: date })
      setProductName('')
      setQuantity('')
      loadRecords()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Produção</h1>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-terra">Novo registro</h2>
          {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
          <input
            required
            placeholder="Produto (ex: Leite, Ovos, Milho)"
            value={productName}
            onChange={(e) => setProductName(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <div className="flex gap-2">
            <input
              required
              type="number"
              step="0.001"
              placeholder="Quantidade"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="w-1/2 rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <input
              required
              placeholder="Unidade"
              value={unit}
              onChange={(e) => setUnit(e.target.value)}
              className="w-1/2 rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
          </div>
          <input
            required
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <button className="w-full rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
            Registrar
          </button>
        </form>

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Histórico</h2>
          {listError && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Data</th>
                <th className="pb-2">Produto</th>
                <th className="pb-2 text-right">Quantidade</th>
              </tr>
            </thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id} className="border-t border-terra/5">
                  <td className="py-2 text-terra/50">{r.productionDate}</td>
                  <td className="py-2">{r.productName}</td>
                  <td className="py-2 text-right">
                    {r.quantity} {r.unit}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
