import { useEffect, useState, type FormEvent } from 'react'
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
  Supplier,
  TransactionType,
} from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { inputClass, linkDangerClass, linkNeutralClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { PeriodFilter, presetPeriod, type Period } from '../components/PeriodFilter'
import { formatCurrency, formatDate, todayIso } from '../lib/format'

type Tab = 'cashFlow' | 'transactions' | 'payables' | 'receivables' | 'costCenters'

export function FinancePage() {
  const { currentProperty } = useProperty()
  const [tab, setTab] = useState<Tab>('transactions')

  const [transactions, setTransactions] = useState<FinancialTransaction[]>([])
  const [payables, setPayables] = useState<AccountPayable[]>([])
  const [receivables, setReceivables] = useState<AccountReceivable[]>([])
  const [costCenters, setCostCenters] = useState<CostCenter[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [activities, setActivities] = useState<Activity[]>([])
  const [listError, setListError] = useState<string | null>(null)
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))

  function loadTransactions() {
    financeApi
      .listTransactions(period.from, period.to)
      .then(setTransactions)
      .catch((err) => setListError(apiErrorMessage(err)))
  }

  function loadAll() {
    setListError(null)
    loadTransactions()
    financeApi.listPayables().then(setPayables).catch((err) => setListError(apiErrorMessage(err)))
    financeApi.listReceivables().then(setReceivables).catch((err) => setListError(apiErrorMessage(err)))
    financeApi.listCostCenters().then(setCostCenters).catch((err) => setListError(apiErrorMessage(err)))
    purchasesApi.listSuppliers().then(setSuppliers).catch((err) => setListError(apiErrorMessage(err)))
    salesApi.listCustomers().then(setCustomers).catch((err) => setListError(apiErrorMessage(err)))
    activitiesApi.list().then(setActivities).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty])

  useEffect(() => {
    if (currentProperty) loadTransactions()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period])

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Financeiro</h1>
      {listError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}

      <div className="flex gap-2 border-b border-terra/10">
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
            onClick={() => setTab(key)}
            className={`px-3 py-2 text-sm font-medium ${
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
          transactions={transactions}
          costCenters={costCenters}
          period={period}
          onPeriodChange={setPeriod}
          onChanged={loadAll}
        />
      )}
      {tab === 'payables' && (
        <PayablesTab payables={payables} suppliers={suppliers} onChanged={loadAll} />
      )}
      {tab === 'receivables' && (
        <ReceivablesTab receivables={receivables} customers={customers} onChanged={loadAll} />
      )}
      {tab === 'costCenters' && (
        <CostCentersTab costCenters={costCenters} activities={activities} onChanged={loadAll} />
      )}
    </div>
  )
}

function TransactionsTab({
  transactions,
  costCenters,
  period,
  onPeriodChange,
  onChanged,
}: {
  transactions: FinancialTransaction[]
  costCenters: CostCenter[]
  period: Period
  onPeriodChange: (period: Period) => void
  onChanged: () => void
}) {
  const income = transactions.filter((t) => t.type === 'INCOME').reduce((sum, t) => sum + t.amount, 0)
  const expense = transactions.filter((t) => t.type === 'EXPENSE').reduce((sum, t) => sum + t.amount, 0)
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

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir este lançamento?')) return
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
        <select
          aria-label="Tipo"
          value={type}
          onChange={(e) => setType(e.target.value as TransactionType)}
          className={inputClass}
        >
          <option value="EXPENSE">Despesa</option>
          <option value="INCOME">Receita</option>
        </select>
        <input
          required
          aria-label="Descrição"
          placeholder="Descrição"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="number"
          step="0.01"
          aria-label="Valor"
          placeholder="Valor"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="date"
          aria-label="Data"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          className={inputClass}
        />
        <select aria-label="Centro de custo" value={costCenterId} onChange={(e) => setCostCenterId(e.target.value)} className={inputClass}>
          <option value="">Centro de custo (opcional)</option>
          {costCenters.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
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
        <div className="overflow-x-auto">
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
                    Nenhum lançamento neste período.
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

function PayablesTab({
  payables,
  suppliers,
  onChanged,
}: {
  payables: AccountPayable[]
  suppliers: Supplier[]
  onChanged: () => void
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

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir esta conta a pagar?')) return
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
        <input
          required
          placeholder="Descrição"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="number"
          step="0.01"
          placeholder="Valor"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
          className={inputClass}
        />
        <select value={supplierId} onChange={(e) => setSupplierId(e.target.value)} className={inputClass}>
          <option value="">Fornecedor (opcional)</option>
          {suppliers.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </select>
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
                  <button onClick={() => handleDelete(p.id)} className="ml-3 text-rust hover:underline">
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
            {payables.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-terra/40">
                  Nenhuma conta a pagar.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function ReceivablesTab({
  receivables,
  customers,
  onChanged,
}: {
  receivables: AccountReceivable[]
  customers: Customer[]
  onChanged: () => void
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

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir esta conta a receber?')) return
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
        <input
          required
          placeholder="Descrição"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="number"
          step="0.01"
          placeholder="Valor"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          className={inputClass}
        />
        <input
          required
          type="date"
          value={dueDate}
          onChange={(e) => setDueDate(e.target.value)}
          className={inputClass}
        />
        <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} className={inputClass}>
          <option value="">Cliente (opcional)</option>
          {customers.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
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
                  <button onClick={() => handleDelete(r.id)} className="ml-3 text-rust hover:underline">
                    Excluir
                  </button>
                </td>
              </tr>
            ))}
            {receivables.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-center text-terra/40">
                  Nenhuma conta a receber.
                </td>
              </tr>
            )}
          </tbody>
        </table>
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

  async function handleDelete(id: number) {
    if (!window.confirm('Excluir este centro de custo?')) return
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
        <input
          required
          placeholder="Nome (ex: Pecuária, Leite, Administrativo)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
        <select value={activityId} onChange={(e) => setActivityId(e.target.value)} className={inputClass}>
          <option value="">Atividade vinculada (opcional)</option>
          {activities.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
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
            <div className="overflow-x-auto">
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
