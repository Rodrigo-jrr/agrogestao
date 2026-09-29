import { useEffect, useState, type FormEvent } from 'react'
import { inventoryApi, purchasesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Product, Purchase, Supplier } from '../api/types'
import { useProperty } from '../property/PropertyContext'

interface ItemRow {
  productId: string
  quantity: string
  unitPrice: string
}

export function PurchasesPage() {
  const { currentProperty } = useProperty()
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [purchases, setPurchases] = useState<Purchase[]>([])
  const [error, setError] = useState<string | null>(null)
  const [supplierError, setSupplierError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [supplierId, setSupplierId] = useState('')
  const [purchaseDate, setPurchaseDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [items, setItems] = useState<ItemRow[]>([{ productId: '', quantity: '', unitPrice: '' }])

  const [editingSupplierId, setEditingSupplierId] = useState<number | null>(null)
  const [supplierName, setSupplierName] = useState('')

  function loadAll() {
    setListError(null)
    purchasesApi.listSuppliers().then(setSuppliers).catch((err) => setListError(apiErrorMessage(err)))
    inventoryApi.listProducts().then(setProducts).catch((err) => setListError(apiErrorMessage(err)))
    purchasesApi.list().then(setPurchases).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
  }, [currentProperty])

  async function handleCancel(id: number) {
    if (!window.confirm('Cancelar esta compra? O estoque dos itens será estornado e o lançamento de despesa removido.')) return
    setListError(null)
    try {
      await purchasesApi.cancel(id)
      loadAll()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  function updateItem(index: number, field: keyof ItemRow, value: string) {
    setItems((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  function startEditSupplier(supplier: Supplier) {
    setEditingSupplierId(supplier.id)
    setSupplierName(supplier.name)
    setSupplierError(null)
  }

  function cancelEditSupplier() {
    setEditingSupplierId(null)
    setSupplierName('')
    setSupplierError(null)
  }

  async function handleSubmitSupplier(event: FormEvent) {
    event.preventDefault()
    if (!supplierName.trim()) return
    setSupplierError(null)
    try {
      if (editingSupplierId) {
        await purchasesApi.updateSupplier(editingSupplierId, { name: supplierName })
      } else {
        await purchasesApi.createSupplier({ name: supplierName })
      }
      cancelEditSupplier()
      loadAll()
    } catch (err) {
      setSupplierError(apiErrorMessage(err))
    }
  }

  async function handleDeleteSupplier(id: number) {
    if (!window.confirm('Excluir este fornecedor?')) return
    try {
      await purchasesApi.removeSupplier(id)
      if (editingSupplierId === id) cancelEditSupplier()
      loadAll()
    } catch (err) {
      setSupplierError(apiErrorMessage(err))
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await purchasesApi.create({
        supplierId: Number(supplierId),
        purchaseDate,
        items: items
          .filter((i) => i.productId && i.quantity && i.unitPrice)
          .map((i) => ({ productId: Number(i.productId), quantity: Number(i.quantity), unitPrice: Number(i.unitPrice) })),
      })
      setItems([{ productId: '', quantity: '', unitPrice: '' }])
      loadAll()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Compras</h1>
      {listError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="space-y-4">
          <form
            onSubmit={handleSubmitSupplier}
            className="space-y-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm"
          >
            <h2 className="text-sm font-semibold text-terra">
              {editingSupplierId ? 'Editar fornecedor' : 'Novo fornecedor'}
            </h2>
            {supplierError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{supplierError}</p>}
            <input
              placeholder="Nome do fornecedor"
              value={supplierName}
              onChange={(e) => setSupplierName(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <div className="flex gap-2">
              <button className="flex-1 rounded-md border border-rust px-3 py-2 text-sm font-medium text-rust hover:bg-rust/10">
                {editingSupplierId ? 'Salvar' : 'Adicionar fornecedor'}
              </button>
              {editingSupplierId && (
                <button
                  type="button"
                  onClick={cancelEditSupplier}
                  className="rounded-md border border-terra/15 px-3 py-2 text-sm text-terra hover:bg-terra/5"
                >
                  Cancelar
                </button>
              )}
            </div>
            {suppliers.length > 0 && (
              <ul className="mt-2 divide-y divide-terra/5 text-sm">
                {suppliers.map((s) => (
                  <li key={s.id} className="flex items-center justify-between py-1.5">
                    <span>{s.name}</span>
                    <span className="whitespace-nowrap">
                      <button onClick={() => startEditSupplier(s)} className="text-rust hover:underline">
                        Editar
                      </button>
                      <button onClick={() => handleDeleteSupplier(s.id)} className="ml-3 text-rust hover:underline">
                        Excluir
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </form>

          <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
            <h2 className="text-sm font-semibold text-terra">Nova compra</h2>
            {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
            <select
              required
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            >
              <option value="">Fornecedor</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
            <input
              required
              type="date"
              value={purchaseDate}
              onChange={(e) => setPurchaseDate(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />

            {items.map((row, index) => (
              <div key={index} className="flex gap-1">
                <select
                  value={row.productId}
                  onChange={(e) => updateItem(index, 'productId', e.target.value)}
                  className="flex-1 rounded-md border border-terra/20 bg-white px-2 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
                >
                  <option value="">Produto</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  step="0.001"
                  placeholder="Qtd"
                  value={row.quantity}
                  onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                  className="w-16 rounded-md border border-terra/20 bg-white px-2 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
                />
                <input
                  type="number"
                  step="0.01"
                  placeholder="Preço"
                  value={row.unitPrice}
                  onChange={(e) => updateItem(index, 'unitPrice', e.target.value)}
                  className="w-20 rounded-md border border-terra/20 bg-white px-2 py-1.5 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
                />
              </div>
            ))}
            <button
              type="button"
              onClick={() => setItems((current) => [...current, { productId: '', quantity: '', unitPrice: '' }])}
              className="text-sm text-rust hover:underline"
            >
              + adicionar item
            </button>

            <button className="w-full rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
              Registrar compra
            </button>
          </form>
        </div>

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Histórico de compras</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Data</th>
                <th className="pb-2">Fornecedor</th>
                <th className="pb-2 text-right">Total</th>
                <th className="pb-2" />
              </tr>
            </thead>
            <tbody>
              {purchases.map((p) => (
                <tr key={p.id} className={`border-t border-terra/5 ${p.canceled ? 'text-terra/40 line-through' : ''}`}>
                  <td className="py-2 text-terra/50">{p.purchaseDate}</td>
                  <td className="py-2">{suppliers.find((s) => s.id === p.supplierId)?.name ?? p.supplierId}</td>
                  <td className="py-2 text-right">
                    {p.totalAmount.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                  </td>
                  <td className="py-2 text-right whitespace-nowrap no-underline">
                    {p.canceled ? (
                      <span className="inline-block text-xs text-terra/50 [text-decoration:none]">Cancelada</span>
                    ) : (
                      <button onClick={() => handleCancel(p.id)} className="text-rust hover:underline">
                        Cancelar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {purchases.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-4 text-center text-terra/40">
                    Nenhuma compra registrada.
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
