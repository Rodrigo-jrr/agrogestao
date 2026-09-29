import { useEffect, useState, type FormEvent } from 'react'
import { inventoryApi, salesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { Customer, Product, Sale } from '../api/types'
import { useProperty } from '../property/PropertyContext'

interface ItemRow {
  productId: string
  quantity: string
  unitPrice: string
}

export function SalesPage() {
  const { currentProperty } = useProperty()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [sales, setSales] = useState<Sale[]>([])
  const [error, setError] = useState<string | null>(null)
  const [customerError, setCustomerError] = useState<string | null>(null)
  const [listError, setListError] = useState<string | null>(null)

  const [customerId, setCustomerId] = useState('')
  const [saleDate, setSaleDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [items, setItems] = useState<ItemRow[]>([{ productId: '', quantity: '', unitPrice: '' }])

  const [editingCustomerId, setEditingCustomerId] = useState<number | null>(null)
  const [customerName, setCustomerName] = useState('')

  function loadAll() {
    setListError(null)
    salesApi.listCustomers().then(setCustomers).catch((err) => setListError(apiErrorMessage(err)))
    inventoryApi.listProducts().then(setProducts).catch((err) => setListError(apiErrorMessage(err)))
    salesApi.list().then(setSales).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
  }, [currentProperty])

  async function handleCancel(id: number) {
    if (!window.confirm('Cancelar esta venda? Os itens voltarão ao estoque e o lançamento de receita será removido.')) return
    setListError(null)
    try {
      await salesApi.cancel(id)
      loadAll()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  function updateItem(index: number, field: keyof ItemRow, value: string) {
    setItems((current) => current.map((row, i) => (i === index ? { ...row, [field]: value } : row)))
  }

  function startEditCustomer(customer: Customer) {
    setEditingCustomerId(customer.id)
    setCustomerName(customer.name)
    setCustomerError(null)
  }

  function cancelEditCustomer() {
    setEditingCustomerId(null)
    setCustomerName('')
    setCustomerError(null)
  }

  async function handleSubmitCustomer(event: FormEvent) {
    event.preventDefault()
    if (!customerName.trim()) return
    setCustomerError(null)
    try {
      if (editingCustomerId) {
        await salesApi.updateCustomer(editingCustomerId, { name: customerName })
      } else {
        await salesApi.createCustomer({ name: customerName })
      }
      cancelEditCustomer()
      loadAll()
    } catch (err) {
      setCustomerError(apiErrorMessage(err))
    }
  }

  async function handleDeleteCustomer(id: number) {
    if (!window.confirm('Excluir este cliente?')) return
    try {
      await salesApi.removeCustomer(id)
      if (editingCustomerId === id) cancelEditCustomer()
      loadAll()
    } catch (err) {
      setCustomerError(apiErrorMessage(err))
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await salesApi.create({
        customerId: Number(customerId),
        saleDate,
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
      <h1 className="font-serif text-2xl font-semibold text-terra">Vendas</h1>
      {listError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="space-y-4">
          <form
            onSubmit={handleSubmitCustomer}
            className="space-y-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm"
          >
            <h2 className="text-sm font-semibold text-terra">
              {editingCustomerId ? 'Editar cliente' : 'Novo cliente'}
            </h2>
            {customerError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{customerError}</p>}
            <input
              placeholder="Nome do cliente"
              value={customerName}
              onChange={(e) => setCustomerName(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <div className="flex gap-2">
              <button className="flex-1 rounded-md border border-rust px-3 py-2 text-sm font-medium text-rust hover:bg-rust/10">
                {editingCustomerId ? 'Salvar' : 'Adicionar cliente'}
              </button>
              {editingCustomerId && (
                <button
                  type="button"
                  onClick={cancelEditCustomer}
                  className="rounded-md border border-terra/15 px-3 py-2 text-sm text-terra hover:bg-terra/5"
                >
                  Cancelar
                </button>
              )}
            </div>
            {customers.length > 0 && (
              <ul className="mt-2 divide-y divide-terra/5 text-sm">
                {customers.map((c) => (
                  <li key={c.id} className="flex items-center justify-between py-1.5">
                    <span>{c.name}</span>
                    <span className="whitespace-nowrap">
                      <button onClick={() => startEditCustomer(c)} className="text-rust hover:underline">
                        Editar
                      </button>
                      <button onClick={() => handleDeleteCustomer(c.id)} className="ml-3 text-rust hover:underline">
                        Excluir
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </form>

          <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
            <h2 className="text-sm font-semibold text-terra">Nova venda</h2>
            {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
            <select
              required
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            >
              <option value="">Cliente</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            <input
              required
              type="date"
              value={saleDate}
              onChange={(e) => setSaleDate(e.target.value)}
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
              Registrar venda
            </button>
          </form>
        </div>

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Histórico de vendas</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Data</th>
                <th className="pb-2">Cliente</th>
                <th className="pb-2 text-right">Total</th>
                <th className="pb-2" />
              </tr>
            </thead>
            <tbody>
              {sales.map((s) => (
                <tr key={s.id} className={`border-t border-terra/5 ${s.canceled ? 'text-terra/40 line-through' : ''}`}>
                  <td className="py-2 text-terra/50">{s.saleDate}</td>
                  <td className="py-2">{customers.find((c) => c.id === s.customerId)?.name ?? s.customerId}</td>
                  <td className="py-2 text-right">
                    {s.totalAmount.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}
                  </td>
                  <td className="py-2 text-right whitespace-nowrap no-underline">
                    {s.canceled ? (
                      <span className="inline-block text-xs text-terra/50 [text-decoration:none]">Cancelada</span>
                    ) : (
                      <button onClick={() => handleCancel(s.id)} className="text-rust hover:underline">
                        Cancelar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {sales.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-4 text-center text-terra/40">
                    Nenhuma venda registrada.
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
