import { Fragment, useEffect, useId, useState, type FormEvent } from 'react'
import { inventoryApi, purchasesApi, salesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { InstallmentInput, InstallmentView, PaymentType, Product, TradeDetailItem } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { InstallmentsEditor, installmentsSum } from '../components/InstallmentsEditor'
import {
  inputClass,
  linkDangerClass,
  linkNeutralClass,
  outlineButtonClass,
  primaryButtonClass,
  secondaryButtonClass,
} from '../components/ui'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../lib/format'

/** Compras e vendas têm o mesmo fluxo; esta página atende as duas a partir de uma configuração. */
export type TradeKind = 'purchase' | 'sale'

interface Party {
  id: number
  name: string
}

interface TradeRow {
  id: number
  partyId: number
  date: string
  totalAmount: number
  canceled: boolean
  paymentType: PaymentType
  installmentCount: number
}

interface TradeDetailView {
  partyName: string | null
  items: TradeDetailItem[]
  installments: InstallmentView[]
  totalCost: number | null
  margin: number | null
}

interface NewTrade {
  partyId: number
  date: string
  items: { productId: number; quantity: number; unitPrice: number }[]
  paymentType: PaymentType
  installments?: InstallmentInput[]
}

interface TradeConfig {
  title: string
  party: string
  partyLower: string
  newOperation: string
  submit: string
  dateLabel: string
  history: string
  empty: string
  settledWord: string
  cancelConfirm: (row: TradeRow) => string
  api: {
    listParties: () => Promise<Party[]>
    createParty: (name: string) => Promise<unknown>
    updateParty: (id: number, name: string) => Promise<unknown>
    removeParty: (id: number) => Promise<unknown>
    list: () => Promise<TradeRow[]>
    create: (data: NewTrade) => Promise<unknown>
    cancel: (id: number) => Promise<unknown>
    detail: (id: number) => Promise<TradeDetailView>
  }
}

const configs: Record<TradeKind, TradeConfig> = {
  purchase: {
    title: 'Compras',
    party: 'Fornecedor',
    partyLower: 'fornecedor',
    newOperation: 'Nova compra',
    submit: 'Registrar compra',
    dateLabel: 'Data da compra',
    history: 'Histórico de compras',
    empty: 'Nenhuma compra registrada ainda.',
    settledWord: 'paga',
    cancelConfirm: (row) =>
      row.paymentType === 'CASH'
        ? 'Cancelar esta compra? O estoque dos itens será estornado e o lançamento de despesa removido.'
        : 'Cancelar esta compra? O estoque será estornado e as parcelas removidas de "A pagar". Só é possível se nenhuma parcela foi paga.',
    api: {
      listParties: () => purchasesApi.listSuppliers(),
      createParty: (name) => purchasesApi.createSupplier({ name }),
      updateParty: (id, name) => purchasesApi.updateSupplier(id, { name }),
      removeParty: (id) => purchasesApi.removeSupplier(id),
      list: () =>
        purchasesApi.list().then((rows) => rows.map((p) => ({ ...p, partyId: p.supplierId, date: p.purchaseDate }))),
      create: ({ partyId, date, ...rest }) => purchasesApi.create({ supplierId: partyId, purchaseDate: date, ...rest }),
      cancel: (id) => purchasesApi.cancel(id),
      detail: (id) =>
        purchasesApi.detail(id).then((d) => ({ ...d, partyName: d.supplierName, totalCost: null, margin: null })),
    },
  },
  sale: {
    title: 'Vendas',
    party: 'Cliente',
    partyLower: 'cliente',
    newOperation: 'Nova venda',
    submit: 'Registrar venda',
    dateLabel: 'Data da venda',
    history: 'Histórico de vendas',
    empty: 'Nenhuma venda registrada ainda.',
    settledWord: 'recebida',
    cancelConfirm: (row) =>
      row.paymentType === 'CASH'
        ? 'Cancelar esta venda? Os itens voltarão ao estoque e o lançamento de receita será removido.'
        : 'Cancelar esta venda? Os itens voltarão ao estoque e as parcelas serão removidas de "A receber". Só é possível se nenhuma parcela foi recebida.',
    api: {
      listParties: () => salesApi.listCustomers(),
      createParty: (name) => salesApi.createCustomer({ name }),
      updateParty: (id, name) => salesApi.updateCustomer(id, { name }),
      removeParty: (id) => salesApi.removeCustomer(id),
      list: () => salesApi.list().then((rows) => rows.map((s) => ({ ...s, partyId: s.customerId, date: s.saleDate }))),
      create: ({ partyId, date, ...rest }) => salesApi.create({ customerId: partyId, saleDate: date, ...rest }),
      cancel: (id) => salesApi.cancel(id),
      detail: (id) => salesApi.detail(id).then((d) => ({ ...d, partyName: d.customerName })),
    },
  },
}

const labelClass = 'mb-1 block text-xs font-medium text-terra/70'
const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'

export function TradePage({ kind }: { kind: TradeKind }) {
  const config = configs[kind]
  const { currentProperty } = useProperty()
  const canManage = currentProperty?.role === 'OWNER' || currentProperty?.role === 'MANAGER'
  const [parties, setParties] = useState<Party[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [rows, setRows] = useState<TradeRow[]>([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState<string | null>(null)

  function loadAll() {
    setListError(null)
    Promise.all([config.api.listParties(), inventoryApi.listProducts(), config.api.list()])
      .then(([partyList, productList, tradeList]) => {
        setParties(partyList)
        setProducts(productList)
        setRows(tradeList)
      })
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty, kind])

  async function handleCancel(row: TradeRow) {
    if (!window.confirm(config.cancelConfirm(row))) return
    setListError(null)
    try {
      await config.api.cancel(row.id)
      loadAll()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">{config.title}</h1>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
        <div className="space-y-4 lg:col-span-2">
          <TradeForm key={`${kind}-${currentProperty?.id}`} config={config} kind={kind} parties={parties} products={products} onSaved={loadAll} />
          <PartyManager config={config} parties={parties} onChanged={loadAll} />
        </div>

        <section className={`${cardClass} lg:col-span-3`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">{config.history}</h2>
          {listError && (
            <p role="alert" className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
              {listError}
            </p>
          )}
          <HistoryTable
            config={config}
            kind={kind}
            rows={rows}
            parties={parties}
            loading={loading}
            canCancel={canManage}
            onCancel={handleCancel}
          />
        </section>
      </div>
    </div>
  )
}

interface ItemRow {
  productId: string
  quantity: string
  unitPrice: string
}

const emptyItem: ItemRow = { productId: '', quantity: '', unitPrice: '' }

function TradeForm({
  config,
  kind,
  parties,
  products,
  onSaved,
}: {
  config: TradeConfig
  kind: TradeKind
  parties: Party[]
  products: Product[]
  onSaved: () => void
}) {
  const ids = useId()
  const [partyId, setPartyId] = useState('')
  const [date, setDate] = useState(todayIso)
  const [items, setItems] = useState<ItemRow[]>([{ ...emptyItem }])
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [installments, setInstallments] = useState<InstallmentInput[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)

  const lineTotal = (row: ItemRow) => (Number(row.quantity) || 0) * (Number(row.unitPrice) || 0)
  const total = Math.round(items.reduce((sum, row) => sum + lineTotal(row), 0) * 100) / 100

  function updateItem(index: number, patch: Partial<ItemRow>) {
    setItems((current) => current.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSaved(false)
    const filled = items.filter((i) => i.productId && i.quantity && i.unitPrice)
    if (filled.length === 0) {
      setError('Adicione ao menos um item com produto, quantidade e preço.')
      return
    }
    if (paymentType === 'INSTALLMENTS' && installmentsSum(installments) !== total) {
      setError('A soma das parcelas precisa ser igual ao total.')
      return
    }
    setSaving(true)
    try {
      await config.api.create({
        partyId: Number(partyId),
        date,
        items: filled.map((i) => ({ productId: Number(i.productId), quantity: Number(i.quantity), unitPrice: Number(i.unitPrice) })),
        paymentType,
        installments: paymentType === 'INSTALLMENTS' ? installments : undefined,
      })
      setItems([{ ...emptyItem }])
      setPaymentType('CASH')
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
      <h2 className="text-sm font-semibold text-terra">{config.newOperation}</h2>
      {error && (
        <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
          {error}
        </p>
      )}
      {saved && (
        <p role="status" className="rounded-md bg-mata/10 px-3 py-2 text-sm text-mata">
          {kind === 'purchase' ? 'Compra registrada.' : 'Venda registrada.'}
        </p>
      )}
      <div className="grid grid-cols-2 gap-2">
        <div>
          <label htmlFor={`${ids}-party`} className={labelClass}>
            {config.party}
          </label>
          <select id={`${ids}-party`} required value={partyId} onChange={(e) => setPartyId(e.target.value)} className={inputClass}>
            <option value="">Selecione</option>
            {parties.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            {config.dateLabel}
          </label>
          <input id={`${ids}-date`} required type="date" value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </div>
      </div>
      {parties.length === 0 && (
        <p className="text-xs text-terra/55">Cadastre um {config.partyLower} logo abaixo para começar.</p>
      )}

      <fieldset>
        <legend className={labelClass}>Itens</legend>
        <div className="space-y-2">
          {items.map((row, index) => {
            const product = products.find((p) => String(p.id) === row.productId)
            const overStock = kind === 'sale' && product && Number(row.quantity) > product.currentQuantity
            return (
              <div key={index} className="rounded-md border border-terra/10 p-2">
                <div className="flex gap-2">
                  <select
                    aria-label={`Produto do item ${index + 1}`}
                    value={row.productId}
                    onChange={(e) => updateItem(index, { productId: e.target.value })}
                    className={`${inputClass} flex-1 py-1.5`}
                  >
                    <option value="">Produto</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                        {kind === 'sale' ? ` (${formatNumber(p.currentQuantity)} ${p.unit} disp.)` : ''}
                      </option>
                    ))}
                  </select>
                  {items.length > 1 && (
                    <button
                      type="button"
                      aria-label={`Remover item ${index + 1}`}
                      onClick={() => setItems((current) => current.filter((_, i) => i !== index))}
                      className="cursor-pointer rounded-md px-2 text-terra/50 hover:bg-terra/5 hover:text-rust"
                    >
                      ×
                    </button>
                  )}
                </div>
                <div className="mt-2 grid grid-cols-3 items-end gap-2">
                  <div>
                    <label htmlFor={`${ids}-qty-${index}`} className="block text-xs text-terra/60">
                      Qtd {product ? `(${product.unit})` : ''}
                    </label>
                    <input
                      id={`${ids}-qty-${index}`}
                      type="number"
                      step="0.001"
                      min="0.001"
                      value={row.quantity}
                      onChange={(e) => updateItem(index, { quantity: e.target.value })}
                      className={`${inputClass} py-1`}
                    />
                  </div>
                  <div>
                    <label htmlFor={`${ids}-price-${index}`} className="block text-xs text-terra/60">
                      Preço unit.
                    </label>
                    <input
                      id={`${ids}-price-${index}`}
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={row.unitPrice}
                      onChange={(e) => updateItem(index, { unitPrice: e.target.value })}
                      className={`${inputClass} py-1`}
                    />
                  </div>
                  <p className="pb-1.5 text-right text-sm tabular-nums text-terra/70">{formatCurrency(lineTotal(row))}</p>
                </div>
                {overStock && (
                  <p className="mt-1 text-xs text-rust">
                    Só há {formatNumber(product.currentQuantity)} {product.unit} em estoque.
                  </p>
                )}
              </div>
            )
          })}
        </div>
        <button
          type="button"
          onClick={() => setItems((current) => [...current, { ...emptyItem }])}
          className={`mt-2 text-sm ${linkNeutralClass}`}
        >
          + adicionar item
        </button>
      </fieldset>

      <div className="flex items-baseline justify-between border-t border-terra/10 pt-2">
        <span className="text-sm text-terra/60">Total</span>
        <span className="font-serif text-xl font-semibold text-terra tabular-nums">{formatCurrency(total)}</span>
      </div>

      <fieldset>
        <legend className={labelClass}>Pagamento</legend>
        <div className="grid grid-cols-2 gap-1">
          {(
            [
              ['CASH', 'À vista'],
              ['INSTALLMENTS', 'A prazo'],
            ] as [PaymentType, string][]
          ).map(([value, label]) => (
            <label
              key={value}
              className={`cursor-pointer rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                paymentType === value ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
              }`}
            >
              <input
                type="radio"
                name={`${ids}-payment`}
                value={value}
                checked={paymentType === value}
                onChange={() => setPaymentType(value)}
                className="sr-only"
              />
              {label}
            </label>
          ))}
        </div>
        <p className="mt-1 text-xs text-terra/55">
          {paymentType === 'CASH'
            ? `Lançado no caixa na data da ${kind === 'purchase' ? 'compra' : 'venda'}.`
            : `Cada parcela vira uma conta em Financeiro › ${kind === 'purchase' ? 'A pagar' : 'A receber'}.`}
        </p>
      </fieldset>

      {paymentType === 'INSTALLMENTS' && (
        <InstallmentsEditor total={total} operationDate={date} value={installments} onChange={setInstallments} />
      )}

      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Registrando…' : config.submit}
      </button>
    </form>
  )
}

function PartyManager({ config, parties, onChanged }: { config: TradeConfig; parties: Party[]; onChanged: () => void }) {
  const ids = useId()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setEditingId(null)
    setName('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) return
    setError(null)
    try {
      if (editingId) await config.api.updateParty(editingId, name.trim())
      else await config.api.createParty(name.trim())
      reset()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(party: Party) {
    if (!window.confirm(`Excluir o ${config.partyLower} ${party.name}?`)) return
    try {
      await config.api.removeParty(party.id)
      if (editingId === party.id) reset()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <section className={`${cardClass} space-y-2`}>
      <form onSubmit={handleSubmit} className="space-y-2">
        <label htmlFor={`${ids}-name`} className="block text-sm font-semibold text-terra">
          {editingId ? `Editar ${config.partyLower}` : `Novo ${config.partyLower}`}
        </label>
        {error && (
          <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
            {error}
          </p>
        )}
        <div className="flex gap-2">
          <input
            id={`${ids}-name`}
            maxLength={150}
            placeholder={`Nome do ${config.partyLower}`}
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
          <button className={`whitespace-nowrap ${secondaryButtonClass}`}>{editingId ? 'Salvar' : 'Adicionar'}</button>
          {editingId && (
            <button type="button" onClick={reset} className={outlineButtonClass}>
              Cancelar
            </button>
          )}
        </div>
      </form>
      {parties.length > 0 && (
        <ul className="divide-y divide-terra/5 text-sm">
          {parties.map((p) => (
            <li key={p.id} className="flex items-center justify-between py-1.5">
              <span>{p.name}</span>
              <span className="whitespace-nowrap">
                <button
                  onClick={() => {
                    setEditingId(p.id)
                    setName(p.name)
                    setError(null)
                  }}
                  className={linkNeutralClass}
                >
                  Editar
                </button>
                <button onClick={() => handleDelete(p)} className={`ml-3 ${linkDangerClass}`}>
                  Excluir
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function HistoryTable({
  config,
  kind,
  rows,
  parties,
  loading,
  canCancel,
  onCancel,
}: {
  config: TradeConfig
  kind: TradeKind
  rows: TradeRow[]
  parties: Party[]
  loading: boolean
  canCancel: boolean
  onCancel: (row: TradeRow) => void
}) {
  const [openId, setOpenId] = useState<number | null>(null)
  const [details, setDetails] = useState<Record<number, TradeDetailView>>({})
  const [detailError, setDetailError] = useState<string | null>(null)

  async function toggle(row: TradeRow) {
    if (openId === row.id) {
      setOpenId(null)
      return
    }
    setOpenId(row.id)
    setDetailError(null)
    try {
      const detail = await config.api.detail(row.id)
      setDetails((current) => ({ ...current, [row.id]: detail }))
    } catch (err) {
      setDetailError(apiErrorMessage(err))
    }
  }

  if (loading) return <p className="py-6 text-center text-sm text-terra/50">Carregando…</p>

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[560px] text-sm">
        <thead>
          <tr className="text-left text-xs text-terra/55">
            <th className="pb-2 font-medium">Data</th>
            <th className="pb-2 font-medium">{config.party}</th>
            <th className="pb-2 font-medium">Pagamento</th>
            <th className="pb-2 text-right font-medium">Total</th>
            <th className="pb-2">
              <span className="sr-only">Ações</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const open = openId === row.id
            const detail = details[row.id]
            return (
              <Fragment key={row.id}>
                <tr className={`border-t border-terra/5 ${row.canceled ? 'text-terra/45' : ''}`}>
                  <td className="py-2 whitespace-nowrap text-terra/60">{formatDate(row.date)}</td>
                  <td className={`py-2 ${row.canceled ? 'line-through' : ''}`}>
                    {parties.find((p) => p.id === row.partyId)?.name ?? `#${row.partyId}`}
                  </td>
                  <td className="py-2 text-terra/70">
                    {row.paymentType === 'CASH' ? 'À vista' : `A prazo · ${row.installmentCount}x`}
                  </td>
                  <td className={`py-2 text-right tabular-nums ${row.canceled ? 'line-through' : ''}`}>
                    {formatCurrency(row.totalAmount)}
                  </td>
                  <td className="py-2 pl-3 text-right whitespace-nowrap">
                    <button onClick={() => toggle(row)} aria-expanded={open} className={linkNeutralClass}>
                      {open ? 'Ocultar' : 'Detalhes'}
                    </button>
                    {row.canceled ? (
                      <span className="ml-3 text-xs text-terra/50">Cancelada</span>
                    ) : (
                      canCancel && (
                        <button onClick={() => onCancel(row)} className={`ml-3 ${linkDangerClass}`}>
                          Cancelar
                        </button>
                      )
                    )}
                  </td>
                </tr>
                {open && (
                  <tr className="bg-terreiro/50">
                    <td colSpan={5} className="px-3 py-3">
                      {detailError && !detail && <p className="text-sm text-rust">{detailError}</p>}
                      {!detail && !detailError && <p className="text-sm text-terra/50">Carregando detalhes…</p>}
                      {detail && <TradeDetails kind={kind} detail={detail} settledWord={config.settledWord} />}
                    </td>
                  </tr>
                )}
              </Fragment>
            )
          })}
          {rows.length === 0 && (
            <tr>
              <td colSpan={5} className="py-6 text-center text-sm text-terra/50">
                {config.empty}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

function TradeDetails({ kind, detail, settledWord }: { kind: TradeKind; detail: TradeDetailView; settledWord: string }) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <div>
        <h3 className="mb-1 text-xs font-semibold text-terra/70">Itens</h3>
        <table className="w-full text-sm">
          <tbody>
            {detail.items.map((item, i) => (
              <tr key={i} className="border-t border-terra/10 first:border-t-0">
                <td className="py-1 pr-2">{item.productName}</td>
                <td className="py-1 pr-2 text-right tabular-nums text-terra/70">
                  {formatNumber(item.quantity)} {item.unit} × {formatCurrency(item.unitPrice)}
                </td>
                <td className="py-1 text-right tabular-nums">{formatCurrency(item.total)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {kind === 'sale' && detail.margin != null && detail.totalCost != null && (
          <p className="mt-2 text-xs text-terra/60">
            Custo dos produtos {formatCurrency(detail.totalCost)} · Margem{' '}
            <strong className={detail.margin < 0 ? 'text-rust' : 'text-mata'}>{formatCurrency(detail.margin)}</strong>
          </p>
        )}
      </div>
      {detail.installments.length > 0 && (
        <div>
          <h3 className="mb-1 text-xs font-semibold text-terra/70">Parcelas</h3>
          <table className="w-full text-sm">
            <tbody>
              {detail.installments.map((i) => (
                <tr key={i.accountId} className="border-t border-terra/10 first:border-t-0">
                  <td className="py-1 pr-2 tabular-nums text-terra/60">
                    {i.number}/{i.count}
                  </td>
                  <td className="py-1 pr-2">{formatDate(i.dueDate)}</td>
                  <td className="py-1 pr-2 text-right tabular-nums">{formatCurrency(i.amount)}</td>
                  <td className={`py-1 text-right text-xs ${i.settled ? 'text-mata' : 'text-terra/55'}`}>
                    {i.settled ? settledWord : 'em aberto'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
