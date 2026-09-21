import { useEffect, useState, type FormEvent } from 'react'
import { activitiesApi, financeApi, purchasesApi, salesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type {
  AccountPayable,
  AccountReceivable,
  Activity,
  CostCenter,
  Customer,
  FinancialTransaction,
  Supplier,
  TransactionType,
} from '../api/types'
import { useProperty } from '../property/PropertyContext'

type Tab = 'transactions' | 'payables' | 'receivables' | 'costCenters'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const inputClass =
  'w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust'

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

  function loadAll() {
    setListError(null)
    financeApi.listTransactions().then(setTransactions).catch((err) => setListError(apiErrorMessage(err)))
    financeApi.listPayables().then(setPayables).catch((err) => setListError(apiErrorMessage(err)))
    financeApi.listReceivables().then(setReceivables).catch((err) => setListError(apiErrorMessage(err)))
    financeApi.listCostCenters().then(setCostCenters).catch((err) => setListError(apiErrorMessage(err)))
    purchasesApi.listSuppliers().then(setSuppliers).catch((err) => setListError(apiErrorMessage(err)))
    salesApi.listCustomers().then(setCustomers).catch((err) => setListError(apiErrorMessage(err)))
    activitiesApi.list().then(setActivities).catch((err) => setListError(apiErrorMessage(err)))
  }

  useEffect(() => {
    if (currentProperty) loadAll()
  }, [currentProperty])

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Financeiro</h1>
      {listError && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}

      <div className="flex gap-2 border-b border-terra/10">
        {(
          [
            ['transactions', 'Lançamentos'],
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

      {tab === 'transactions' && (
        <TransactionsTab transactions={transactions} costCenters={costCenters} onChanged={loadAll} />
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
  onChanged,
}: {
  transactions: FinancialTransaction[]
  costCenters: CostCenter[]
  onChanged: () => void
}) {
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
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
        <select value={type} onChange={(e) => setType(e.target.value as TransactionType)} className={inputClass}>
          <option value="EXPENSE">Despesa</option>
          <option value="INCOME">Receita</option>
        </select>
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
        <input required type="date" value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        <select value={costCenterId} onChange={(e) => setCostCenterId(e.target.value)} className={inputClass}>
          <option value="">Centro de custo (opcional)</option>
          {costCenters.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
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

      <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-semibold text-terra">Lançamentos do mês</h2>
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
                <td className="py-2 text-terra/50">{t.transactionDate}</td>
                <td className="py-2">{t.description}</td>
                <td className="py-2">{t.type === 'INCOME' ? 'Receita' : 'Despesa'}</td>
                <td className={`py-2 text-right ${t.type === 'INCOME' ? 'text-mata' : 'text-rust'}`}>
                  {formatCurrency(t.amount)}
                </td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => startEdit(t)} className="text-rust hover:underline">
                    Editar
                  </button>
                  <button onClick={() => handleDelete(t.id)} className="ml-3 text-rust hover:underline">
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
  const [dueDate, setDueDate] = useState(() => new Date().toISOString().slice(0, 10))
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
                <td className="py-2 text-terra/50">{p.dueDate}</td>
                <td className="py-2">{p.description}</td>
                <td className="py-2 text-right">{formatCurrency(p.amount)}</td>
                <td className="py-2 text-right">{p.paid ? 'Pago' : 'Pendente'}</td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => toggle(p)} className="text-rust hover:underline">
                    {p.paid ? 'Desfazer' : 'Marcar pago'}
                  </button>
                  <button onClick={() => startEdit(p)} className="ml-3 text-rust hover:underline">
                    Editar
                  </button>
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
  const [dueDate, setDueDate] = useState(() => new Date().toISOString().slice(0, 10))
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
                <td className="py-2 text-terra/50">{r.dueDate}</td>
                <td className="py-2">{r.description}</td>
                <td className="py-2 text-right">{formatCurrency(r.amount)}</td>
                <td className="py-2 text-right">{r.received ? 'Recebido' : 'Pendente'}</td>
                <td className="py-2 text-right whitespace-nowrap">
                  <button onClick={() => toggle(r)} className="text-rust hover:underline">
                    {r.received ? 'Desfazer' : 'Marcar recebido'}
                  </button>
                  <button onClick={() => startEdit(r)} className="ml-3 text-rust hover:underline">
                    Editar
                  </button>
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
