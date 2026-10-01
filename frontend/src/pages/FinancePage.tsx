import { useEffect, useId, useState, type FormEvent, type ReactNode } from 'react'
import { activitiesApi, financeApi, purchasesApi, salesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type {
  AccountPayable,
  CashFlow,
  AccountReceivable,
  Activity,
  CostCenter,
  Customer,
  FinancialTransaction,
  SettlementStatus,
  Supplier,
  TransactionTotals,
  TransactionType,
} from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { fieldLabelClass, inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { PeriodFilter, presetPeriod, type Period } from '../components/PeriodFilter'
import { formatCurrency, formatDate, todayIso } from '../lib/format'
import { Pagination } from '../components/Pagination'
import { usePaged } from '../lib/usePaged'
import { useDebounced } from '../lib/useDebounced'
import { useConfirm } from '../components/ConfirmDialog'

type Tab = 'cashFlow' | 'transactions' | 'payables' | 'receivables' | 'costCenters'

export function FinancePage() {
  const { currentProperty } = useProperty()
  const [tab, setTab] = useState<Tab>('transactions')

  const [costCenters, setCostCenters] = useState<CostCenter[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [activities, setActivities] = useState<Activity[]>([])
  const [listError, setListError] = useState<string | null>(null)
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))
  const enabled = !!currentProperty

  const [txType, setTxType] = useState<TransactionType | ''>('')
  const [txSearch, setTxSearch] = useState('')
  const txQuery = useDebounced(txSearch)
  const transactions = usePaged(
    (page) =>
      financeApi.listTransactions({ from: period.from, to: period.to, type: txType || undefined, q: txQuery || undefined, page }),
    [currentProperty, period, txType, txQuery],
    enabled,
  )
  const [totals, setTotals] = useState<TransactionTotals | null>(null)
  useEffect(() => {
    if (!enabled) return
    financeApi.transactionTotals(period.from, period.to).then(setTotals).catch((err) => setListError(apiErrorMessage(err)))
  }, [enabled, currentProperty, period, transactions.version])

  const [payableStatus, setPayableStatus] = useState<SettlementStatus>('OPEN')
  const [payableSearch, setPayableSearch] = useState('')
  const payableQuery = useDebounced(payableSearch)
  const payables = usePaged(
    (page) => financeApi.listPayables({ status: payableStatus, q: payableQuery || undefined, page }),
    [currentProperty, payableStatus, payableQuery],
    enabled,
  )

  const [receivableStatus, setReceivableStatus] = useState<SettlementStatus>('OPEN')
  const [receivableSearch, setReceivableSearch] = useState('')
  const receivableQuery = useDebounced(receivableSearch)
  const receivables = usePaged(
    (page) => financeApi.listReceivables({ status: receivableStatus, q: receivableQuery || undefined, page }),
    [currentProperty, receivableStatus, receivableQuery],
    enabled,
  )

  function loadAll() {
    setListError(null)
    transactions.reload()
    payables.reload()
    receivables.reload()
    financeApi.listCostCenters().then(setCostCenters).catch((err) => setListError(apiErrorMessage(err)))
    purchasesApi.listSuppliers().then(setSuppliers).catch((err) => setListError(apiErrorMessage(err)))
    salesApi.listCustomers().then(setCustomers).catch((err) => setListError(apiErrorMessage(err)))
    activitiesApi.list().then(setActivities).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty])


  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Financeiro</h1>
      {listError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}

      <div role="tablist" aria-label="Seções do financeiro" className="flex gap-2 overflow-x-auto border-b border-terra/10">
        {(
          [
            ['transactions', 'Lançamentos'],
            ['cashFlow', 'Fluxo de caixa'],
            ['payables', 'A pagar'],
            ['receivables', 'A receber'],
            ['costCenters', 'Centros de custo'],
          ] as [Tab, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={`shrink-0 cursor-pointer px-3 py-2 text-sm font-medium whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
              tab === key ? 'border-b-2 border-rust text-rust' : 'text-terra/50'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'cashFlow' && <CashFlowTab />}

      {tab === 'transactions' && (
        <TransactionsTab
          transactions={transactions.items}
          loading={transactions.loading}
          totals={totals}
          costCenters={costCenters}
          period={period}
          onPeriodChange={setPeriod}
          onChanged={loadAll}
          toolbar={
            <ListToolbar search={txSearch} onSearch={setTxSearch} searchLabel="Buscar lançamento">
              <select
                aria-label="Tipo de lançamento"
                value={txType}
                onChange={(e) => setTxType(e.target.value as TransactionType | '')}
                className={`${inputClass} w-36 py-1`}
              >
                <option value="">Todos os tipos</option>
                <option value="INCOME">Receitas</option>
                <option value="EXPENSE">Despesas</option>
              </select>
            </ListToolbar>
          }
          pagination={<Pagination {...pageProps(transactions)} />}
        />
      )}
      {tab === 'payables' && (
        <PayablesTab
          payables={payables.items}
          loading={payables.loading}
          suppliers={suppliers}
          onChanged={loadAll}
          toolbar={
            <ListToolbar search={payableSearch} onSearch={setPayableSearch} searchLabel="Buscar conta">
              <StatusFilter value={payableStatus} onChange={setPayableStatus} settledLabel="Pagas" />
            </ListToolbar>
          }
          pagination={<Pagination {...pageProps(payables)} />}
        />
      )}
      {tab === 'receivables' && (
        <ReceivablesTab
          receivables={receivables.items}
          loading={receivables.loading}
          customers={customers}
          onChanged={loadAll}
          toolbar={
            <ListToolbar search={receivableSearch} onSearch={setReceivableSearch} searchLabel="Buscar conta">
              <StatusFilter value={receivableStatus} onChange={setReceivableStatus} settledLabel="Recebidas" />
            </ListToolbar>
          }
          pagination={<Pagination {...pageProps(receivables)} />}
        />
      )}
      {tab === 'costCenters' && (
        <CostCentersTab costCenters={costCenters} activities={activities} onChanged={loadAll} />
      )}
    </div>
  )
}

function TransactionsTab({
  transactions,
  loading,
  totals,
  costCenters,
  period,
  onPeriodChange,
  onChanged,
  toolbar,
  pagination,
}: {
  transactions: FinancialTransaction[]
  loading: boolean
  totals: TransactionTotals | null
  costCenters: CostCenter[]
  period: Period
  onPeriodChange: (period: Period) => void
  onChanged: () => void
  toolbar: ReactNode
  pagination: ReactNode
}) {
  const income = totals?.income ?? 0
  const expense = totals?.expense ?? 0
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(todayIso)
  const [costCenterId, setCostCenterId] = useState('')

  function startEdit(t: FinancialTransaction) {
    setEditingId(t.id)
    setType(t.type)
    setDescription(t.description)
    setAmount(String(t.amount))
    setDate(t.transactionDate)
    setCostCenterId(t.costCenterId ? String(t.costCenterId) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setType('EXPENSE')
    setDescription('')
    setAmount('')
    setCostCenterId('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = {
        type,
        description,
        amount: Number(amount),
        transactionDate: date,
        costCenterId: costCenterId ? Number(costCenterId) : undefined,
      }
      if (editingId) {
        await financeApi.updateTransaction(editingId, payload)
      } else {
        await financeApi.createTransaction(payload)
      }
      cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const formId = useId()
  const confirm = useConfirm()

  async function handleDelete(id: number) {
    const transaction = transactions.find((t) => t.id === id)
    const ok = await confirm({
      title: 'Excluir lançamento?',
      message: transaction
        ? `"${transaction.description}" (${formatCurrency(transaction.amount)}, ${formatDate(transaction.transactionDate)}) sai do caixa e dos relatórios. Não dá para desfazer.`
        : 'O lançamento sai do caixa e dos relatórios. Não dá para desfazer.',
      confirmLabel: 'Excluir lançamento',
    })
    if (!ok) return
    try {
      await financeApi.removeTransaction(id)
      if (editingId === id) cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-terra">{editingId ? 'Editar lançamento' : 'Novo lançamento'}</h2>
        {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
        <div>
          <label htmlFor={`${formId}-tipo`} className={fieldLabelClass}>
            Tipo
          </label>
          <select
            id={`${formId}-tipo`}
            value={type}
            onChange={(e) => setType(e.target.value as TransactionType)}
            className={inputClass}
          >
            <option value="EXPENSE">Despesa</option>
            <option value="INCOME">Receita</option>
          </select>
        </div>
        <div>
          <label htmlFor={`${formId}-descricao`} className={fieldLabelClass}>
            Descrição
          </label>
          <input
            required
            id={`${formId}-descricao`}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-valor`} className={fieldLabelClass}>
            Valor
          </label>
          <input
            required
            type="number"
            step="0.01"
            id={`${formId}-valor`}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-data`} className={fieldLabelClass}>
            Data
          </label>
          <input
            required
            type="date"
            id={`${formId}-data`}
            value={date}
            onChange={(e) => setDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-centro-de-custo`} className={fieldLabelClass}>
            Centro de custo <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select
            id={`${formId}-centro-de-custo`}
            value={costCenterId}
            onChange={(e) => setCostCenterId(e.target.value)}
            className={inputClass}
          >
            <option value="">Nenhum</option>
            {costCenters.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div className="flex gap-2">
          <button className={`flex-1 ${primaryButtonClass}`}>{editingId ? 'Salvar' : 'Adicionar'}</button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className={outlineButtonClass}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <div className="mb-3 space-y-3">
          <h2 className="text-sm font-semibold text-terra">Lançamentos do período</h2>
          <PeriodFilter value={period} onChange={onPeriodChange} />
          {toolbar}
          <dl className="flex flex-wrap gap-x-6 gap-y-1 text-sm">
            <div className="flex gap-1.5">
              <dt className="text-terra/55">Receitas</dt>
              <dd className="font-medium text-mata">{formatCurrency(income)}</dd>
            </div>
            <div className="flex gap-1.5">
              <dt className="text-terra/55">Despesas</dt>
              <dd className="font-medium text-rust">{formatCurrency(expense)}</dd>
            </div>
            <div className="flex gap-1.5">
              <dt className="text-terra/55">Resultado</dt>
              <dd className={`font-semibold ${income - expense < 0 ? 'text-rust' : 'text-terra'}`}>
                {formatCurrency(income - expense)}
              </dd>
            </div>
          </dl>
        </div>
        <div className="relative overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2">Data</th>
                <th className="pb-2">Descrição</th>
                <th className="pb-2">Tipo</th>
                <th className="pb-2 text-right">Valor</th>
                <th className="pb-2" />
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id} className="border-t border-terra/5">
                  <td className="py-2 whitespace-nowrap text-terra/50">{formatDate(t.transactionDate)}</td>
                  <td className="py-2">{t.description}</td>
                  <td className="py-2">{t.type === 'INCOME' ? 'Receita' : 'Despesa'}</td>
                  <td className={`py-2 text-right ${t.type === 'INCOME' ? 'text-mata' : 'text-rust'}`}>
                    {formatCurrency(t.amount)}
                  </td>
                  <td className="py-2 text-right whitespace-nowrap">
                    <button onClick={() => startEdit(t)} className={linkNeutralClass}>
                      Editar
                    </button>
                    <button onClick={() => handleDelete(t.id)} className={`ml-3 ${linkDangerClass}`}>
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-4 text-center text-terra/40">
                    {loading ? 'Carregando…' : 'Nenhum lançamento encontrado. Experimente outro período ou filtro.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {pagination}
      </div>
    </div>
  )
}

function PayablesTab({
  payables,
  loading,
  suppliers,
  onChanged,
  toolbar,
  pagination,
}: {
  payables: AccountPayable[]
  loading: boolean
  suppliers: Supplier[]
  onChanged: () => void
  toolbar: ReactNode
  pagination: ReactNode
}) {
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [dueDate, setDueDate] = useState(todayIso)
  const [supplierId, setSupplierId] = useState('')

  function startEdit(p: AccountPayable) {
    setEditingId(p.id)
    setDescription(p.description)
    setAmount(String(p.amount))
    setDueDate(p.dueDate)
    setSupplierId(p.supplierId ? String(p.supplierId) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setDescription('')
    setAmount('')
    setSupplierId('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = {
        description,
        amount: Number(amount),
        dueDate,
        supplierId: supplierId ? Number(supplierId) : undefined,
      }
      if (editingId) {
        await financeApi.updatePayable(editingId, payload)
      } else {
        await financeApi.createPayable(payload)
      }
      cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const formId = useId()
  const confirm = useConfirm()

  async function handleDelete(id: number) {
    const payable = payables.find((p) => p.id === id)
    const ok = await confirm({
      title: 'Excluir conta a pagar?',
      message: `${payable ? `"${payable.description}" (${formatCurrency(payable.amount)})` : 'A conta'} deixa de aparecer em "A pagar" e no fluxo de caixa previsto. Não dá para desfazer.`,
      confirmLabel: 'Excluir conta',
    })
    if (!ok) return
    try {
      await financeApi.removePayable(id)
      if (editingId === id) cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function toggle(p: AccountPayable) {
    try {
      if (p.paid) await financeApi.unpayPayable(p.id)
      else await financeApi.payPayable(p.id)
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-terra">{editingId ? 'Editar conta a pagar' : 'Nova conta a pagar'}</h2>
        {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
        <div>
          <label htmlFor={`${formId}-descricao`} className={fieldLabelClass}>
            Descrição
          </label>
          <input
            required
            id={`${formId}-descricao`}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-valor`} className={fieldLabelClass}>
            Valor
          </label>
          <input
            required
            type="number"
            step="0.01"
            min="0.01"
            id={`${formId}-valor`}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-vencimento`} className={fieldLabelClass}>
            Vencimento
          </label>
          <input
            required
            type="date"
            id={`${formId}-vencimento`}
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-fornecedor`} className={fieldLabelClass}>
            Fornecedor <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select
            id={`${formId}-fornecedor`}
            value={supplierId}
            onChange={(e) => setSupplierId(e.target.value)}
            className={inputClass}
          >
            <option value="">Nenhum</option>
            {suppliers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
        <div className="flex gap-2">
          <button className={`flex-1 ${primaryButtonClass}`}>{editingId ? 'Salvar' : 'Adicionar'}</button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className={outlineButtonClass}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <div className="mb-3">{toolbar}</div>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-terra/50">
              <th className="pb-2">Vencimento</th>
              <th className="pb-2">Descrição</th>
              <th className="pb-2 text-right">Valor</th>
              <th className="pb-2 text-right">Status</th>
              <th className="pb-2" />
            </tr>
          </thead>
          <tbody>
            {payables.map((p) => (
              <tr key={p.id} className="border-t border-terra/5">
                <td className="py-2 whitespace-nowrap text-terra/50">{formatDate(p.dueDate)}</td>
                <td className="py-2">{p.description}</td>
                <td className="py-2 text-right">{formatCurrency(p.amount)}</td>
                <td className="py-2 text-right">{p.paid ? 'Pago' : 'Pendente'}</td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => toggle(p)} className="text-rust hover:underline">
                    {p.paid ? 'Desfazer' : 'Marcar pago'}
                  </button>
                  {!p.paid && (
                    <button onClick={() => startEdit(p)} className="ml-3 text-rust hover:underline">
                      Editar
                    </button>
                  )}
                  {p.purchaseId == null && p.livestockTradeId == null && (
                    <button onClick={() => handleDelete(p.id)} className="ml-3 text-rust hover:underline">
                      Excluir
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {payables.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-terra/40">
                  {loading ? 'Carregando…' : 'Nenhuma conta encontrada com esses filtros.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {pagination}
      </div>
    </div>
  )
}

function ReceivablesTab({
  receivables,
  loading,
  customers,
  onChanged,
  toolbar,
  pagination,
}: {
  receivables: AccountReceivable[]
  loading: boolean
  customers: Customer[]
  onChanged: () => void
  toolbar: ReactNode
  pagination: ReactNode
}) {
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [dueDate, setDueDate] = useState(todayIso)
  const [customerId, setCustomerId] = useState('')

  function startEdit(r: AccountReceivable) {
    setEditingId(r.id)
    setDescription(r.description)
    setAmount(String(r.amount))
    setDueDate(r.dueDate)
    setCustomerId(r.customerId ? String(r.customerId) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setDescription('')
    setAmount('')
    setCustomerId('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = {
        description,
        amount: Number(amount),
        dueDate,
        customerId: customerId ? Number(customerId) : undefined,
      }
      if (editingId) {
        await financeApi.updateReceivable(editingId, payload)
      } else {
        await financeApi.createReceivable(payload)
      }
      cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const formId = useId()
  const confirm = useConfirm()

  async function handleDelete(id: number) {
    const receivable = receivables.find((r) => r.id === id)
    const ok = await confirm({
      title: 'Excluir conta a receber?',
      message: `${receivable ? `"${receivable.description}" (${formatCurrency(receivable.amount)})` : 'A conta'} deixa de aparecer em "A receber" e no fluxo de caixa previsto. Não dá para desfazer.`,
      confirmLabel: 'Excluir conta',
    })
    if (!ok) return
    try {
      await financeApi.removeReceivable(id)
      if (editingId === id) cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function toggle(r: AccountReceivable) {
    try {
      if (r.received) await financeApi.unreceiveReceivable(r.id)
      else await financeApi.receiveReceivable(r.id)
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-terra">
          {editingId ? 'Editar conta a receber' : 'Nova conta a receber'}
        </h2>
        {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
        <div>
          <label htmlFor={`${formId}-descricao`} className={fieldLabelClass}>
            Descrição
          </label>
          <input
            required
            id={`${formId}-descricao`}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-valor`} className={fieldLabelClass}>
            Valor
          </label>
          <input
            required
            type="number"
            step="0.01"
            min="0.01"
            id={`${formId}-valor`}
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-vencimento`} className={fieldLabelClass}>
            Vencimento
          </label>
          <input
            required
            type="date"
            id={`${formId}-vencimento`}
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-cliente`} className={fieldLabelClass}>
            Cliente <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select
            id={`${formId}-cliente`}
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            className={inputClass}
          >
            <option value="">Nenhum</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div className="flex gap-2">
          <button className={`flex-1 ${primaryButtonClass}`}>{editingId ? 'Salvar' : 'Adicionar'}</button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className={outlineButtonClass}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <div className="mb-3">{toolbar}</div>
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-terra/50">
              <th className="pb-2">Vencimento</th>
              <th className="pb-2">Descrição</th>
              <th className="pb-2 text-right">Valor</th>
              <th className="pb-2 text-right">Status</th>
              <th className="pb-2" />
            </tr>
          </thead>
          <tbody>
            {receivables.map((r) => (
              <tr key={r.id} className="border-t border-terra/5">
                <td className="py-2 whitespace-nowrap text-terra/50">{formatDate(r.dueDate)}</td>
                <td className="py-2">{r.description}</td>
                <td className="py-2 text-right">{formatCurrency(r.amount)}</td>
                <td className="py-2 text-right">{r.received ? 'Recebido' : 'Pendente'}</td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => toggle(r)} className="text-rust hover:underline">
                    {r.received ? 'Desfazer' : 'Marcar recebido'}
                  </button>
                  {!r.received && (
                    <button onClick={() => startEdit(r)} className="ml-3 text-rust hover:underline">
                      Editar
                    </button>
                  )}
                  {r.saleId == null && r.livestockTradeId == null && (
                    <button onClick={() => handleDelete(r.id)} className="ml-3 text-rust hover:underline">
                      Excluir
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {receivables.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-terra/40">
                  {loading ? 'Carregando…' : 'Nenhuma conta encontrada com esses filtros.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {pagination}
      </div>
    </div>
  )
}

function CostCentersTab({
  costCenters,
  activities,
  onChanged,
}: {
  costCenters: CostCenter[]
  activities: Activity[]
  onChanged: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [name, setName] = useState('')
  const [activityId, setActivityId] = useState('')

  function startEdit(c: CostCenter) {
    setEditingId(c.id)
    setName(c.name)
    setActivityId(c.activityId ? String(c.activityId) : '')
    setError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setName('')
    setActivityId('')
    setError(null)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const payload = { name, activityId: activityId ? Number(activityId) : undefined }
      if (editingId) {
        await financeApi.updateCostCenter(editingId, payload)
      } else {
        await financeApi.createCostCenter(payload)
      }
      cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  const formId = useId()
  const confirm = useConfirm()

  async function handleDelete(id: number) {
    const costCenter = costCenters.find((c) => c.id === id)
    const ok = await confirm({
      title: `Excluir o centro de custo ${costCenter?.name ?? ''}?`,
      message: 'Se houver lançamentos ligados a ele, a exclusão será recusada. Não dá para desfazer.',
      confirmLabel: 'Excluir centro de custo',
    })
    if (!ok) return
    try {
      await financeApi.removeCostCenter(id)
      if (editingId === id) cancelEdit()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <h2 className="text-sm font-semibold text-terra">
          {editingId ? 'Editar centro de custo' : 'Novo centro de custo'}
        </h2>
        {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
        <div>
          <label htmlFor={`${formId}-nome`} className={fieldLabelClass}>
            Nome
          </label>
          <input
            required
            id={`${formId}-nome`}
            maxLength={150}
            placeholder="Ex.: Pecuária, Leite, Administrativo"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${formId}-atividade-vinculada`} className={fieldLabelClass}>
            Atividade vinculada <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select
            id={`${formId}-atividade-vinculada`}
            value={activityId}
            onChange={(e) => setActivityId(e.target.value)}
            className={inputClass}
          >
            <option value="">Nenhum</option>
            {activities.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
        </div>
        <div className="flex gap-2">
          <button className={`flex-1 ${primaryButtonClass}`}>{editingId ? 'Salvar' : 'Adicionar'}</button>
          {editingId && (
            <button type="button" onClick={cancelEdit} className={outlineButtonClass}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-terra/50">
              <th className="pb-2">Nome</th>
              <th className="pb-2">Atividade</th>
              <th className="pb-2" />
            </tr>
          </thead>
          <tbody>
            {costCenters.map((c) => (
              <tr key={c.id} className="border-t border-terra/5">
                <td className="py-2">{c.name}</td>
                <td className="py-2 text-terra/50">
                  {activities.find((a) => a.id === c.activityId)?.name ?? '-'}
                </td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => startEdit(c)} className="text-rust hover:underline">
                    Editar
                  </button>
                  <button onClick={() => handleDelete(c.id)} className="ml-3 text-rust hover:underline">
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
            {costCenters.length === 0 && (
              <tr>
                <td colSpan={3} className="py-4 text-center text-terra/40">
                  Nenhum centro de custo cadastrado.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function monthOffset(offset: number) {
  const d = new Date()
  d.setDate(1)
  d.setMonth(d.getMonth() + offset)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

function formatMonth(month: string) {
  const [year, m] = month.split('-').map(Number)
  const name = new Date(year, m - 1, 1).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')
  return `${name}/${String(year).slice(2)}`
}

function CashFlowTab() {
  const { currentProperty } = useProperty()
  const [from, setFrom] = useState(() => monthOffset(-3))
  const [to, setTo] = useState(() => monthOffset(3))
  const [data, setData] = useState<CashFlow | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!currentProperty || !from || !to) return
    setError(null)
    financeApi
      .cashFlow(from, to)
      .then(setData)
      .catch((err) => {
        setData(null)
        setError(apiErrorMessage(err))
      })
  }, [currentProperty, from, to])

  const currentMonth = monthOffset(0)
  const maxBar = data
    ? Math.max(1, ...data.months.flatMap((m) => [m.income + m.pendingReceivables, m.expense + m.pendingPayables]))
    : 1
  const last = data?.months[data.months.length - 1]

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <label className="text-xs text-terra/60">
          De
          <input type="month" value={from} onChange={(e) => setFrom(e.target.value)} className={`${inputClass} mt-1`} />
        </label>
        <label className="text-xs text-terra/60">
          Até
          <input type="month" value={to} onChange={(e) => setTo(e.target.value)} className={`${inputClass} mt-1`} />
        </label>
      </div>
      {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

      {data && last && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <SummaryCard label="Saldo inicial" value={data.openingBalance} />
            <SummaryCard label="Saldo realizado" value={last.balance} />
            <SummaryCard label="Saldo projetado" value={last.projectedBalance} highlight />
            <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
              <p className="text-xs text-terra/50">Vencidas antes do período</p>
              <p className="mt-1 text-sm text-mata">+ {formatCurrency(data.overdueReceivables)}</p>
              <p className="text-sm text-rust">- {formatCurrency(data.overduePayables)}</p>
            </div>
          </div>

          <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
            <div className="mb-3 flex flex-wrap gap-4 text-xs text-terra/60">
              <span className="flex items-center gap-1">
                <span className="inline-block h-2.5 w-2.5 rounded-sm bg-mata" /> Entradas
              </span>
              <span className="flex items-center gap-1">
                <span className="inline-block h-2.5 w-2.5 rounded-sm bg-rust" /> Saídas
              </span>
              <span className="flex items-center gap-1">
                <span className="inline-block h-2.5 w-2.5 rounded-sm bg-terra/15" /> Em aberto (previsto)
              </span>
            </div>
            <div className="relative overflow-x-auto">
              <table className="w-full min-w-[640px] text-sm">
                <thead>
                  <tr className="text-left text-terra/50">
                    <th className="pb-2">Mês</th>
                    <th className="pb-2 w-1/4">Movimento</th>
                    <th className="pb-2 text-right">Entradas</th>
                    <th className="pb-2 text-right">Saídas</th>
                    <th className="pb-2 text-right">Resultado</th>
                    <th className="pb-2 text-right">Saldo</th>
                    <th className="pb-2 text-right">Projetado</th>
                  </tr>
                </thead>
                <tbody>
                  {data.months.map((m) => (
                    <tr
                      key={m.month}
                      className={`border-t border-terra/5 ${m.month === currentMonth ? 'bg-trigo/10' : ''}`}
                    >
                      <td className="py-2 pr-2 font-medium whitespace-nowrap capitalize">{formatMonth(m.month)}</td>
                      <td className="py-2 pr-3">
                        <Bar realized={m.income} pending={m.pendingReceivables} max={maxBar} color="bg-mata" />
                        <Bar realized={m.expense} pending={m.pendingPayables} max={maxBar} color="bg-rust" />
                      </td>
                      <td className="py-2 pl-2 text-right whitespace-nowrap text-mata">
                        {formatCurrency(m.income)}
                        {m.pendingReceivables > 0 && (
                          <div className="text-xs text-terra/40">+{formatCurrency(m.pendingReceivables)} previsto</div>
                        )}
                      </td>
                      <td className={`py-2 pl-2 text-right whitespace-nowrap ${m.expense > 0 ? 'text-rust' : ''}`}>
                        {formatCurrency(m.expense)}
                        {m.pendingPayables > 0 && (
                          <div className="text-xs text-terra/40">+{formatCurrency(m.pendingPayables)} previsto</div>
                        )}
                      </td>
                      <td className={`py-2 pl-2 text-right whitespace-nowrap ${m.net < 0 ? 'text-rust' : ''}`}>{formatCurrency(m.net)}</td>
                      <td className={`py-2 pl-2 text-right font-medium whitespace-nowrap ${m.balance < 0 ? 'text-rust' : ''}`}>
                        {formatCurrency(m.balance)}
                      </td>
                      <td className={`py-2 pl-2 text-right whitespace-nowrap ${m.projectedBalance < 0 ? 'text-rust' : 'text-terra/70'}`}>
                        {formatCurrency(m.projectedBalance)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-3 text-xs text-terra/40">
              Saldo = lançamentos realizados. Projetado = saldo + contas a pagar/receber em aberto até o mês.
            </p>
          </div>
        </>
      )}
    </div>
  )
}

function SummaryCard({ label, value, highlight }: { label: string; value: number; highlight?: boolean }) {
  return (
    <div className={`rounded-2xl p-4 shadow-sm ${highlight ? 'bg-mata text-terreiro' : 'border border-terra/10 bg-white'}`}>
      <p className={`text-xs ${highlight ? 'text-terreiro/70' : 'text-terra/50'}`}>{label}</p>
      <p className={`mt-1 font-serif text-xl font-semibold ${!highlight && value < 0 ? 'text-rust' : ''}`}>
        {formatCurrency(value)}
      </p>
    </div>
  )
}

function Bar({ realized, pending, max, color }: { realized: number; pending: number; max: number; color: string }) {
  return (
    <div className="my-0.5 flex h-2 w-full overflow-hidden rounded-full bg-terra/5">
      <div className={color} style={{ width: `${(realized / max) * 100}%` }} />
      <div className="bg-terra/15" style={{ width: `${(pending / max) * 100}%` }} />
    </div>
  )
}

function pageProps(list: { page: number; totalPages: number; totalElements: number; setPage: (page: number) => void }) {
  return { page: list.page, totalPages: list.totalPages, totalElements: list.totalElements, onChange: list.setPage }
}

function ListToolbar({
  search,
  onSearch,
  searchLabel,
  children,
}: {
  search: string
  onSearch: (value: string) => void
  searchLabel: string
  children?: ReactNode
}) {
  const id = useId()
  return (
    <div className="flex flex-wrap items-center gap-2">
      {children}
      <label htmlFor={id} className="sr-only">
        {searchLabel}
      </label>
      <input
        id={id}
        type="search"
        placeholder="Buscar pela descrição"
        value={search}
        onChange={(e) => onSearch(e.target.value)}
        className={`${inputClass} w-56 py-1`}
      />
    </div>
  )
}

function StatusFilter({
  value,
  onChange,
  settledLabel,
}: {
  value: SettlementStatus
  onChange: (status: SettlementStatus) => void
  settledLabel: string
}) {
  const options: [SettlementStatus, string][] = [
    ['OPEN', 'Em aberto'],
    ['SETTLED', settledLabel],
    ['ALL', 'Todas'],
  ]
  return (
    <div role="group" aria-label="Situação" className="flex gap-1">
      {options.map(([key, label]) => (
        <button
          key={key}
          type="button"
          aria-pressed={value === key}
          onClick={() => onChange(key)}
          className={`cursor-pointer rounded-full border px-3 py-1 text-xs font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
            value === key ? 'border-rust bg-rust text-terreiro' : 'border-terra/15 text-terra/70 hover:bg-terra/5'
          }`}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
