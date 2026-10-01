import { useId, useState } from 'react'
import type { InstallmentInput, PaymentType } from '../../api/types'
import { InstallmentsEditor } from '../../components/InstallmentsEditor'
import { inputClass } from '../../components/ui'

export const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'
export const labelClass = 'mb-1 block text-xs font-medium text-terra/70'

/** À vista (lançamento no caixa) ou a prazo (uma conta por parcela), como em Compras e Vendas. */
export function PaymentFields({
  kind,
  total,
  date,
  paymentType,
  onPaymentType,
  installments,
  onInstallments,
}: {
  kind: 'purchase' | 'sale'
  total: number
  date: string
  paymentType: PaymentType
  onPaymentType: (value: PaymentType) => void
  installments: InstallmentInput[]
  onInstallments: (value: InstallmentInput[]) => void
}) {
  const ids = useId()
  return (
    <>
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
              className={`flex cursor-pointer items-center justify-center gap-2 rounded-md border px-2 py-1.5 text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                paymentType === value ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
              }`}
            >
              <input
                type="radio"
                name={`${ids}-payment`}
                value={value}
                checked={paymentType === value}
                onChange={() => onPaymentType(value)}
                className="sr-only"
              />
              {label}
            </label>
          ))}
        </div>
        <p className="mt-1 text-xs text-terra/55">
          {paymentType === 'CASH'
            ? 'Lançado no caixa na data da operação.'
            : `Cada parcela vira uma conta em Financeiro › ${kind === 'purchase' ? 'A pagar' : 'A receber'}.`}
        </p>
      </fieldset>
      {paymentType === 'INSTALLMENTS' && (
        <InstallmentsEditor total={total} operationDate={date} value={installments} onChange={onInstallments} />
      )}
    </>
  )
}

/** Fornecedor/cliente: escolhe da lista ou cadastra na hora, sem sair da tela. */
export function PartySelect({
  label,
  parties,
  value,
  onChange,
  onCreate,
}: {
  label: string
  parties: { id: number; name: string }[]
  value: string
  onChange: (value: string) => void
  onCreate: (name: string) => Promise<{ id: number }>
}) {
  const ids = useId()
  const [adding, setAdding] = useState(false)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function create() {
    if (!name.trim()) return
    setError(null)
    try {
      const created = await onCreate(name.trim())
      onChange(String(created.id))
      setName('')
      setAdding(false)
    } catch {
      setError('Não foi possível cadastrar. Tente de novo.')
    }
  }

  return (
    <div>
      <div className="flex items-baseline justify-between">
        <label htmlFor={`${ids}-party`} className={labelClass}>
          {label}
        </label>
        <button
          type="button"
          onClick={() => setAdding((v) => !v)}
          className="cursor-pointer text-xs text-rust hover:underline"
        >
          {adding ? 'Cancelar' : `+ Novo ${label.toLowerCase()}`}
        </button>
      </div>
      {adding ? (
        <div className="flex gap-2">
          <input
            aria-label={`Nome do novo ${label.toLowerCase()}`}
            value={name}
            maxLength={150}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                create()
              }
            }}
            className={inputClass}
          />
          <button type="button" onClick={create} className="cursor-pointer rounded-md border border-rust px-3 text-sm text-rust hover:bg-rust/10">
            Salvar
          </button>
        </div>
      ) : (
        <select id={`${ids}-party`} required value={value} onChange={(e) => onChange(e.target.value)} className={inputClass}>
          <option value="">Selecione</option>
          {parties.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
      )}
      {error && <p className="mt-1 text-xs text-rust">{error}</p>}
    </div>
  )
}
