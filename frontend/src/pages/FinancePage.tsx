import { useEffect, useState, type FormEvent } from 'react'
import { financeApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { AccountPayable, AccountReceivable, FinancialTransaction, TransactionType } from '../api/types'
import { useProperty } from '../property/PropertyContext'

type Tab = 'transactions' | 'payables' | 'receivables'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function FinancePage() {
  const { currentProperty } = useProperty()
  const [tab, setTab] = useState<Tab>('transactions')
  const [transactions, setTransactions] = useState<FinancialTransaction[]>([])
  const [payables, setPayables] = useState<AccountPayable[]>([])
  const [receivables, setReceivables] = useState<AccountReceivable[]>([])
  const [error, setError] = useState<string | null>(null)

  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))

  function loadAll() {
    financeApi.listTransactions().then(setTransactions).catch(() => {})
    financeApi.listPayables().then(setPayables).catch(() => {})
    financeApi.listReceivables().then(setReceivables).catch(() => {})
  }

  useEffect(() => {
    if (currentProperty) loadAll()
  }, [currentProperty])

  async function handleCreateTransaction(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await financeApi.createTransaction({ type, description, amount: Number(amount), transactionDate: date })
      setDescription('')
      setAmount('')
      loadAll()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Financeiro</h1>

      <div className="flex gap-2 border-b border-terra/10">
        {(
          [
            ['transactions', 'Lançamentos'],
            ['payables', 'A pagar'],
            ['receivables', 'A receber'],
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
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <form onSubmit={handleCreateTransaction} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
            <h2 className="text-sm font-semibold text-terra">Novo lançamento</h2>
            {error && <p className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
            <select
              value={type}
              onChange={(e) => setType(e.target.value as TransactionType)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            >
              <option value="EXPENSE">Despesa</option>
              <option value="INCOME">Receita</option>
            </select>
            <input
              required
              placeholder="Descrição"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <input
              required
              type="number"
              step="0.01"
              placeholder="Valor"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <input
              required
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full rounded-md border border-terra/20 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
            />
            <button className="w-full rounded-md bg-rust px-3 py-2 text-sm font-medium text-white hover:bg-rust-dark">
              Adicionar
            </button>
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'payables' && (
        <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
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
                  <td className="py-2 text-right">
                    {!p.paid && (
                      <button
                        onClick={() => financeApi.payPayable(p.id).then(loadAll)}
                        className="text-rust hover:underline"
                      >
                        Marcar como pago
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'receivables' && (
        <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
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
                  <td className="py-2 text-right">
                    {!r.received && (
                      <button
                        onClick={() => financeApi.receiveReceivable(r.id).then(loadAll)}
                        className="text-rust hover:underline"
                      >
                        Marcar como recebido
                      </button>
                    )}
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
