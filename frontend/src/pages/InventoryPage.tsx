import { useEffect, useId, useMemo, useRef, useState, type FormEvent } from 'react'
import { inventoryApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { InventoryMovement, MovementType, Product } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { PeriodFilter, presetPeriod, type Period } from '../components/PeriodFilter'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../lib/format'

const labelClass = 'mb-1 block text-xs font-medium text-terra/70'
const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'

const movementLabels: Record<MovementType, string> = { IN: 'Entrada', OUT: 'Saída', ADJUST: 'Ajuste' }

function normalize(text: string | null | undefined) {
  return (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

export function InventoryPage() {
  const { currentProperty } = useProperty()
  const canManage = currentProperty?.role === 'OWNER' || currentProperty?.role === 'MANAGER'
  const [products, setProducts] = useState<Product[]>([])
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [showArchived, setShowArchived] = useState(false)
  const [search, setSearch] = useState('')
  const [listError, setListError] = useState<string | null>(null)
  const [historyProductId, setHistoryProductId] = useState('')
  const [historyVersion, setHistoryVersion] = useState(0)
  const historyRef = useRef<HTMLElement>(null)
  const ids = useId()

  function loadProducts() {
    setListError(null)
    inventoryApi
      .listProducts(true)
      .then(setProducts)
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoadingProducts(false))
  }

  useEffect(() => {
    if (currentProperty) loadProducts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty])

  function afterChange() {
    loadProducts()
    setHistoryVersion((v) => v + 1)
  }

  const activeProducts = products.filter((p) => !p.archived)
  const visibleProducts = useMemo(() => {
    const term = normalize(search.trim())
    return products
      .filter((p) => showArchived || !p.archived)
      .filter((p) => !term || normalize(p.name).includes(term) || normalize(p.category).includes(term))
  }, [products, showArchived, search])
  const totalValue = activeProducts.reduce((sum, p) => sum + p.stockValue, 0)
  const belowMinimum = activeProducts.filter((p) => p.currentQuantity < p.minStock).length

  async function runAction(action: () => Promise<unknown>) {
    setListError(null)
    try {
      await action()
      afterChange()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  function showHistory(productId: number) {
    setHistoryProductId(String(productId))
    historyRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="font-serif text-2xl font-semibold text-terra">Estoque</h1>
        <dl className="flex flex-wrap gap-x-6 gap-y-1 text-sm">
          <div className="flex gap-1.5">
            <dt className="text-terra/55">Valor em estoque</dt>
            <dd className="font-semibold text-terra">{formatCurrency(totalValue)}</dd>
          </div>
          <div className="flex gap-1.5">
            <dt className="text-terra/55">Produtos ativos</dt>
            <dd className="font-medium text-terra">{activeProducts.length}</dd>
          </div>
          <div className="flex gap-1.5">
            <dt className="text-terra/55">Abaixo do mínimo</dt>
            <dd className={`font-medium ${belowMinimum > 0 ? 'text-rust' : 'text-terra'}`}>{belowMinimum}</dd>
          </div>
        </dl>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4">
          <ProductForm key={currentProperty?.id} onSaved={afterChange} editing={null} />
          <MovementForm products={activeProducts} onSaved={afterChange} />
        </div>

        <section className={`${cardClass} lg:col-span-2`} aria-labelledby={`${ids}-products`}>
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 id={`${ids}-products`} className="text-sm font-semibold text-terra">
              Produtos
            </h2>
            <div className="flex flex-wrap items-center gap-3">
              <label className="flex items-center gap-1.5 text-xs text-terra/70">
                <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
                Mostrar arquivados
              </label>
              <label htmlFor={`${ids}-search`} className="sr-only">
                Buscar produto
              </label>
              <input
                id={`${ids}-search`}
                type="search"
                placeholder="Buscar produto ou categoria"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className={`${inputClass} w-56 py-1.5`}
              />
            </div>
          </div>
          {listError && (
            <p role="alert" className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
              {listError}
            </p>
          )}
          <ProductTable
            products={visibleProducts}
            loading={loadingProducts}
            searching={search.trim() !== ''}
            canManage={canManage}
            onSaved={afterChange}
            onHistory={showHistory}
            onArchive={(p) => runAction(() => inventoryApi.archiveProduct(p.id))}
            onUnarchive={(p) => runAction(() => inventoryApi.unarchiveProduct(p.id))}
            onDelete={(p) => {
              if (!window.confirm(`Excluir o produto ${p.name}? Esta ação não pode ser desfeita.`)) return
              runAction(() => inventoryApi.removeProduct(p.id))
            }}
          />
        </section>
      </div>

      <section ref={historyRef} className={cardClass} aria-labelledby={`${ids}-history`}>
        <MovementHistory
          headingId={`${ids}-history`}
          products={products}
          productId={historyProductId}
          onProductChange={setHistoryProductId}
          reloadKey={historyVersion}
        />
      </section>
    </div>
  )
}

function ProductForm({ editing, onSaved, onCancel }: { editing: Product | null; onSaved: () => void; onCancel?: () => void }) {
  const ids = useId()
  const [name, setName] = useState(editing?.name ?? '')
  const [category, setCategory] = useState(editing?.category ?? '')
  const [unit, setUnit] = useState(editing?.unit ?? 'kg')
  const [minStock, setMinStock] = useState(editing ? String(editing.minStock) : '0')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      const payload = { name, category: category || undefined, unit, minStock: Number(minStock) }
      if (editing) await inventoryApi.updateProduct(editing.id, payload)
      else await inventoryApi.createProduct(payload)
      if (!editing) {
        setName('')
        setCategory('')
        setMinStock('0')
      }
      onSaved()
      onCancel?.()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className={editing ? 'space-y-3' : `${cardClass} space-y-3`}>
      {!editing && <h2 className="text-sm font-semibold text-terra">Novo produto</h2>}
      {error && (
        <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
          {error}
        </p>
      )}
      <div>
        <label htmlFor={`${ids}-name`} className={labelClass}>
          Nome
        </label>
        <input id={`${ids}-name`} required maxLength={150} value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
      </div>
      <div>
        <label htmlFor={`${ids}-category`} className={labelClass}>
          Categoria <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input
          id={`${ids}-category`}
          maxLength={60}
          placeholder="Ex.: Insumos, Rações, Medicamentos"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className={inputClass}
        />
      </div>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <label htmlFor={`${ids}-unit`} className={labelClass}>
            Unidade
          </label>
          <input
            id={`${ids}-unit`}
            required
            maxLength={20}
            placeholder="kg, L, un, sc"
            value={unit}
            onChange={(e) => setUnit(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-min`} className={labelClass}>
            Estoque mínimo
          </label>
          <input
            id={`${ids}-min`}
            required
            type="number"
            min="0"
            step="0.001"
            value={minStock}
            onChange={(e) => setMinStock(e.target.value)}
            className={inputClass}
          />
        </div>
      </div>
      <div className="flex gap-2">
        <button disabled={saving} className={`flex-1 ${primaryButtonClass}`}>
          {saving ? 'Salvando…' : editing ? 'Salvar' : 'Adicionar produto'}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className={outlineButtonClass}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}

function MovementForm({ products, onSaved }: { products: Product[]; onSaved: () => void }) {
  const ids = useId()
  const [productId, setProductId] = useState('')
  const [type, setType] = useState<MovementType>('IN')
  const [quantity, setQuantity] = useState('')
  const [unitCost, setUnitCost] = useState('')
  const [date, setDate] = useState(todayIso)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const selected = products.find((p) => String(p.id) === productId)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaved(false)
    setSaving(true)
    try {
      await inventoryApi.createMovement({
        productId: Number(productId),
        type,
        quantity: Number(quantity),
        unitCost: type !== 'OUT' && unitCost ? Number(unitCost) : undefined,
        movementDate: date,
        notes: notes || undefined,
      })
      setQuantity('')
      setUnitCost('')
      setNotes('')
      setSaved(true)
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className={`${cardClass} space-y-3`}>
      <h2 className="text-sm font-semibold text-terra">Registrar movimentação</h2>
      <p className="text-xs text-terra/50">Compras e vendas já movimentam o estoque sozinhas.</p>
      {error && (
        <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
          {error}
        </p>
      )}
      {saved && (
        <p role="status" className="rounded-md bg-mata/10 px-3 py-2 text-sm text-mata">
          Movimentação registrada.
        </p>
      )}
      <div>
        <label htmlFor={`${ids}-product`} className={labelClass}>
          Produto
        </label>
        <select id={`${ids}-product`} required value={productId} onChange={(e) => setProductId(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} ({formatNumber(p.currentQuantity)} {p.unit})
            </option>
          ))}
        </select>
      </div>
      <fieldset>
        <legend className={labelClass}>Tipo</legend>
        <div className="grid grid-cols-3 gap-1">
          {(['IN', 'OUT', 'ADJUST'] as MovementType[]).map((t) => (
            <label
              key={t}
              className={`cursor-pointer rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                type === t ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
              }`}
            >
              <input type="radio" name={`${ids}-type`} value={t} checked={type === t} onChange={() => setType(t)} className="sr-only" />
              {movementLabels[t]}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <label htmlFor={`${ids}-qty`} className={labelClass}>
            {type === 'ADJUST' ? 'Saldo correto' : 'Quantidade'} {selected && <span className="font-normal">({selected.unit})</span>}
          </label>
          <input
            id={`${ids}-qty`}
            required
            type="number"
            min={type === 'ADJUST' ? '0' : '0.001'}
            step="0.001"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            Data
          </label>
          <input id={`${ids}-date`} required type="date" value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </div>
      </div>
      {type === 'OUT' ? (
        <p className="text-xs text-terra/55">
          A saída usa o custo médio atual{selected ? ` (${formatCurrency(selected.averageCost)} por ${selected.unit})` : ''}.
        </p>
      ) : (
        <div>
          <label htmlFor={`${ids}-cost`} className={labelClass}>
            {type === 'IN' ? 'Custo unitário' : 'Novo custo médio'} <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input
            id={`${ids}-cost`}
            type="number"
            min="0"
            step="0.01"
            value={unitCost}
            onChange={(e) => setUnitCost(e.target.value)}
            aria-describedby={`${ids}-cost-help`}
            className={inputClass}
          />
          <p id={`${ids}-cost-help`} className="mt-1 text-xs text-terra/50">
            {type === 'IN'
              ? 'Sem custo, a entrada não altera o custo médio (ex.: doação, produção própria).'
              : 'Deixe em branco para manter o custo médio atual.'}
          </p>
        </div>
      )}
      <div>
        <label htmlFor={`${ids}-notes`} className={labelClass}>
          Observação <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <input id={`${ids}-notes`} maxLength={255} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
      </div>
      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Registrando…' : 'Registrar'}
      </button>
    </form>
  )
}

function ProductTable({
  products,
  loading,
  searching,
  canManage,
  onSaved,
  onHistory,
  onArchive,
  onUnarchive,
  onDelete,
}: {
  products: Product[]
  loading: boolean
  searching: boolean
  canManage: boolean
  onSaved: () => void
  onHistory: (id: number) => void
  onArchive: (p: Product) => void
  onUnarchive: (p: Product) => void
  onDelete: (p: Product) => void
}) {
  const [editingId, setEditingId] = useState<number | null>(null)

  if (loading) return <p className="py-6 text-center text-sm text-terra/50">Carregando produtos…</p>

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-sm">
        <thead>
          <tr className="text-left text-xs text-terra/55">
            <th className="pb-2 font-medium">Produto</th>
            <th className="pb-2 text-right font-medium">Saldo</th>
            <th className="pb-2 text-right font-medium">Custo médio</th>
            <th className="pb-2 text-right font-medium">Valor</th>
            <th className="pb-2 text-right font-medium">Mínimo</th>
            <th className="pb-2">
              <span className="sr-only">Ações</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) =>
            editingId === p.id ? (
              <tr key={p.id} className="border-t border-terra/5 bg-terreiro/40">
                <td colSpan={6} className="p-3">
                  <ProductForm editing={p} onSaved={onSaved} onCancel={() => setEditingId(null)} />
                </td>
              </tr>
            ) : (
              <tr key={p.id} className={`border-t border-terra/5 ${p.archived ? 'text-terra/45' : ''}`}>
                <td className="py-2 pr-3">
                  <div className="font-medium">
                    {p.name}
                    {p.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivado</span>}
                  </div>
                  {p.category && <div className="text-xs text-terra/50">{p.category}</div>}
                </td>
                <td className={`py-2 text-right tabular-nums ${!p.archived && p.currentQuantity < p.minStock ? 'font-medium text-rust' : ''}`}>
                  {formatNumber(p.currentQuantity)} {p.unit}
                </td>
                <td className="py-2 text-right tabular-nums">{formatCurrency(p.averageCost)}</td>
                <td className="py-2 text-right tabular-nums">{formatCurrency(p.stockValue)}</td>
                <td className="py-2 text-right tabular-nums text-terra/60">{formatNumber(p.minStock)}</td>
                <td className="py-2 pl-3 text-right whitespace-nowrap">
                  <button onClick={() => onHistory(p.id)} className={linkNeutralClass}>
                    Histórico
                  </button>
                  {!p.archived && (
                    <button onClick={() => setEditingId(p.id)} className={`ml-3 ${linkNeutralClass}`}>
                      Editar
                    </button>
                  )}
                  {canManage &&
                    (p.archived ? (
                      <button onClick={() => onUnarchive(p)} className={`ml-3 ${linkNeutralClass}`}>
                        Reativar
                      </button>
                    ) : (
                      <>
                        <button onClick={() => onArchive(p)} className={`ml-3 ${linkNeutralClass}`}>
                          Arquivar
                        </button>
                        <button onClick={() => onDelete(p)} className={`ml-3 ${linkDangerClass}`}>
                          Excluir
                        </button>
                      </>
                    ))}
                </td>
              </tr>
            ),
          )}
          {products.length === 0 && (
            <tr>
              <td colSpan={6} className="py-6 text-center text-sm text-terra/50">
                {searching ? 'Nenhum produto encontrado. Tente outro nome ou categoria.' : 'Nenhum produto cadastrado ainda.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

function MovementHistory({
  headingId,
  products,
  productId,
  onProductChange,
  reloadKey,
}: {
  headingId: string
  products: Product[]
  productId: string
  onProductChange: (id: string) => void
  reloadKey: number
}) {
  const { currentProperty } = useProperty()
  const ids = useId()
  const [period, setPeriod] = useState<Period>(() => presetPeriod('last3Months'))
  const [movements, setMovements] = useState<InventoryMovement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!currentProperty) return
    let active = true
    setLoading(true)
    setError(null)
    inventoryApi
      .listMovements({ productId: productId ? Number(productId) : undefined, from: period.from, to: period.to })
      .then((list) => active && setMovements(list))
      .catch((err) => active && setError(apiErrorMessage(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [currentProperty, productId, period, reloadKey])

  return (
    <>
      <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
        <h2 id={headingId} className="text-sm font-semibold text-terra">
          Histórico de movimentações
        </h2>
        <div className="flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor={`${ids}-product`} className={labelClass}>
              Produto
            </label>
            <select id={`${ids}-product`} value={productId} onChange={(e) => onProductChange(e.target.value)} className={`${inputClass} py-1`}>
              <option value="">Todos</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                  {p.archived ? ' (arquivado)' : ''}
                </option>
              ))}
            </select>
          </div>
          <PeriodFilter value={period} onChange={setPeriod} />
        </div>
      </div>
      {error && (
        <p role="alert" className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
          {error}
        </p>
      )}
      <div className="overflow-x-auto" aria-busy={loading}>
        <table className="w-full min-w-[720px] text-sm">
          <thead>
            <tr className="text-left text-xs text-terra/55">
              <th className="pb-2 font-medium">Data</th>
              <th className="pb-2 font-medium">Produto</th>
              <th className="pb-2 font-medium">Tipo</th>
              <th className="pb-2 text-right font-medium">Quantidade</th>
              <th className="pb-2 text-right font-medium">Custo unit.</th>
              <th className="pb-2 text-right font-medium">Total</th>
              <th className="pb-2 pl-3 font-medium">Observação</th>
            </tr>
          </thead>
          <tbody className={loading ? 'opacity-50' : ''}>
            {movements.map((m) => (
              <tr key={m.id} className="border-t border-terra/5">
                <td className="py-2 whitespace-nowrap text-terra/60">{formatDate(m.movementDate)}</td>
                <td className="py-2">{m.productName}</td>
                <td className={`py-2 ${m.type === 'IN' ? 'text-mata' : m.type === 'OUT' ? 'text-rust' : 'text-terra/70'}`}>
                  {movementLabels[m.type]}
                </td>
                <td className="py-2 text-right tabular-nums">
                  {m.type === 'IN' ? '+' : m.type === 'OUT' ? '−' : '='} {formatNumber(m.quantity)} {m.unit}
                </td>
                <td className="py-2 text-right tabular-nums">{m.unitCost != null ? formatCurrency(m.unitCost) : '—'}</td>
                <td className="py-2 text-right tabular-nums">
                  {m.unitCost != null && m.type !== 'ADJUST' ? formatCurrency(m.unitCost * m.quantity) : '—'}
                </td>
                <td className="py-2 pl-3 text-terra/60">{m.notes ?? ''}</td>
              </tr>
            ))}
            {!loading && movements.length === 0 && (
              <tr>
                <td colSpan={7} className="py-6 text-center text-sm text-terra/50">
                  Nenhuma movimentação neste período. Experimente um período maior.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}
