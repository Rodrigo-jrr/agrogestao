import { useId, useState } from 'react'
import type { Animal, InstallmentInput, PaymentType } from '../../api/types'
import { InstallmentsEditor } from '../../components/InstallmentsEditor'
import { inputClass } from '../../components/ui'
import { sexLabels, speciesLabels } from '../../lib/livestock'

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

/** Etiqueta "em carência até dd/mm" (o animal não deve ir para abate nem ordenha comercial). */
export function WithdrawalBadge({ until }: { until: string | null | undefined }) {
  if (!until) return null
  const [, m, d] = until.split('-')
  return (
    <span
      title={`Em carência até ${d}/${m}`}
      className="ml-1 inline-block rounded-full bg-trigo/20 px-1.5 py-0.5 text-[11px] font-medium whitespace-nowrap text-trigo-dark"
    >
      carência até {d}/{m}
    </span>
  )
}

/** Lista de animais ativos com filtro e caixas de seleção (venda, aplicação sanitária...). */
export function AnimalPicker({
  legend,
  animals,
  chosen,
  onToggle,
  emptyText = 'Nenhum animal ativo.',
}: {
  legend: string
  animals: Animal[]
  chosen: Set<number>
  onToggle: (id: number) => void
  emptyText?: string
}) {
  const [filter, setFilter] = useState('')
  const term = filter.trim().toLowerCase()
  const shown = animals.filter(
    (a) =>
      !term ||
      a.tag.toLowerCase().includes(term) ||
      (a.name ?? '').toLowerCase().includes(term) ||
      (a.batchName ?? '').toLowerCase().includes(term),
  )
  return (
    <fieldset>
      <legend className={labelClass}>
        {legend}{' '}
        {chosen.size > 0 && (
          <span className="font-normal">({chosen.size === 1 ? '1 marcado' : `${chosen.size} marcados`})</span>
        )}
      </legend>
      <input
        type="search"
        aria-label="Filtrar animais por brinco, nome ou lote"
        placeholder="Filtrar por brinco, nome ou lote"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        className={`${inputClass} mb-2`}
      />
      <div className="max-h-56 overflow-y-auto rounded-md border border-terra/10">
        {shown.length === 0 ? (
          <p className="p-3 text-sm text-terra/50">{animals.length === 0 ? emptyText : 'Nenhum animal com esse filtro.'}</p>
        ) : (
          <ul>
            {shown.map((a) => (
              <li key={a.id} className="border-b border-terra/5 last:border-b-0">
                <label className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm hover:bg-terra/5">
                  <input type="checkbox" checked={chosen.has(a.id)} onChange={() => onToggle(a.id)} className="accent-rust" />
                  <span className="font-medium text-terra">{a.tag}</span>
                  <span className="min-w-0 truncate text-terra/55">
                    {[a.name, speciesLabels[a.species], sexLabels[a.sex], a.batchName].filter(Boolean).join(' · ')}
                  </span>
                  <WithdrawalBadge until={a.withdrawalUntil} />
                </label>
              </li>
            ))}
          </ul>
        )}
      </div>
    </fieldset>
  )
}
