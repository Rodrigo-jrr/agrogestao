import { useEffect, useState, type FormEvent } from 'react'
import { inventoryApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { MovementType, Product } from '../api/types'
import { useProperty } from '../property/PropertyContext'

export function InventoryPage() {
  const { currentProperty } = useProperty()
  const [products, setProducts] = useState<Product[]>([])
  const [error, setError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [unit, setUnit] = useState('kg')
  const [minStock, setMinStock] = useState('0')

  const [movementProductId, setMovementProductId] = useState('')
  const [movementType, setMovementType] = useState<MovementType>('IN')
  const [movementQuantity, setMovementQuantity] = useState('')
  const [movementDate, setMovementDate] = useState(() => new Date().toISOString().slice(0, 10))

  function loadProducts() {
    setListError(null)
    inventoryApi.listProducts().then(setProducts).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadProducts()
  }, [currentProperty])

  function startEdit(product: Product) {
    setEditingId(product.id)
    setName(product.name)
    setCategory(product.category ?? '')
    setUnit(product.unit)
    setMinStock(String(product.minStock))
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setName('')
    setCategory('')
    setUnit('kg')
    setMinStock('0')
    setError(null)
  }

  async function handleSubmitProduct(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = { name, category: category || undefined, unit, minStock: Number(minStock) }
      if (editingId) {
        await inventoryApi.updateProduct(editingId, payload)
      } else {
        await inventoryApi.createProduct(payload)
      }
      cancelEdit()
      loadProducts()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDeleteProduct(id: number) {
    if (!window.confirm('Excluir este produto?')) return
    try {
      await inventoryApi.removeProduct(id)
      if (editingId === id) cancelEdit()
      loadProducts()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  async function handleCreateMovement(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await inventoryApi.createMovement({
        productId: Number(movementProductId),
        type: movementType,
        quantity: Number(movementQuantity),
        movementDate,
      })
      setMovementQuantity('')
      loadProducts()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Estoque</h1>
      {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <form
          onSubmit={handleSubmitProduct}
          className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm"
        >
          <h2 className="text-sm font-semibold text-terra">{editingId ? 'Editar produto' : 'Novo produto'}</h2>
          <input
            required
            placeholder="Nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <input
            placeholder="Categoria"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <div className="flex gap-2">
            <input
              required
              placeholder="Unidade (kg, L, un...)"
              value={unit}
              onChange={(e) => setUnit(e.target.value)}
              className="w-1/2 rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <input
              required
              type="number"
              min="0"
              step="0.001"
              placeholder="Estoque mínimo"
              value={minStock}
              onChange={(e) => setMinStock(e.target.value)}
              className="w-1/2 rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
          </div>
          <div className="flex gap-2">
            <button className="flex-1 rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
              {editingId ? 'Salvar' : 'Adicionar'}
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

        <form onSubmit={handleCreateMovement} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="text-sm font-semibold text-terra">Registrar movimentação</h2>
          <select
            required
            value={movementProductId}
            onChange={(e) => setMovementProductId(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          >
            <option value="">Selecione o produto</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
          <select
            value={movementType}
            onChange={(e) => setMovementType(e.target.value as MovementType)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          >
            <option value="IN">Entrada</option>
            <option value="OUT">Saída</option>
            <option value="ADJUST">Ajuste (saldo final)</option>
          </select>
          <input
            required
            type="number"
            min="0"
            step="0.001"
            placeholder="Quantidade"
            value={movementQuantity}
            onChange={(e) => setMovementQuantity(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <input
            required
            type="date"
            value={movementDate}
            onChange={(e) => setMovementDate(e.target.value)}
            className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
          />
          <button className="w-full rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
            Registrar
          </button>
        </form>

        <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm md:col-span-1">
          <h2 className="mb-3 text-sm font-semibold text-terra">Saldo atual</h2>
          {listError && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Produto</th>
                <th className="pb-2 text-right">Saldo</th>
                <th className="pb-2" />
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className="border-t border-terra/5">
                  <td className="py-2">{p.name}</td>
                  <td
                    className={`py-2 text-right ${p.currentQuantity < p.minStock ? 'font-semibold text-trigo-dark' : ''}`}
                  >
                    {p.currentQuantity} {p.unit}
                  </td>
                  <td className="py-2 text-right whitespace-nowrap">
                    <button onClick={() => startEdit(p)} className="text-rust hover:underline">
                      Editar
                    </button>
                    <button onClick={() => handleDeleteProduct(p.id)} className="ml-3 text-rust hover:underline">
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && !listError && (
                <tr>
                  <td colSpan={3} className="py-4 text-center text-terra/40">
                    Nenhum produto cadastrado.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
