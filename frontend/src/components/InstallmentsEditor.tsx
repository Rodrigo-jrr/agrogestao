import { useEffect, useId, useState } from 'react'
import type { InstallmentInput } from '../api/types'
import { formatCurrency, toIsoDate } from '../lib/format'
import { inputClass } from './ui'

const MAX_INSTALLMENTS = 48
const labelClass = 'mb-1 block text-xs font-medium text-terra/70'

function addDays(isoDate: string, days: number) {
  const [y, m, d] = isoDate.split('-').map(Number)
  return toIsoDate(new Date(y, m - 1, d + days))
}

/** Divide o total em parcelas iguais em centavos; a diferença do arredondamento vai para a última. */
export function splitInstallments(total: number, count: number, firstDueDate: string, intervalDays: number): InstallmentInput[] {
  const cents = Math.round(total * 100)
  const base = Math.floor(cents / count)
  return Array.from({ length: count }, (_, i) => ({
    dueDate: addDays(firstDueDate, i * intervalDays),
    amount: (i === count - 1 ? cents - base * (count - 1) : base) / 100,
  }))
}

export function installmentsSum(installments: InstallmentInput[]) {
  return Math.round(installments.reduce((sum, i) => sum + (Number.isFinite(i.amount) ? i.amount : 0), 0) * 100) / 100
}

/**
 * Plano de parcelas: gera parcelas iguais a partir de quantidade, 1º vencimento e intervalo, e permite
 * ajustar datas e valores de cada uma. Mudar total, quantidade ou datas base gera o plano de novo.
 */
export function InstallmentsEditor({
  total,
  operationDate,
  value,
  onChange,
}: {
  total: number
  operationDate: string
  value: InstallmentInput[]
  onChange: (installments: InstallmentInput[]) => void
}) {
  const ids = useId()
  const [count, setCount] = useState(1)
  const [firstDueDate, setFirstDueDate] = useState(() => addDays(operationDate, 30))
  const [intervalDays, setIntervalDays] = useState(30)

  useEffect(() => {
    if (total > 0 && count >= 1 && count <= MAX_INSTALLMENTS && firstDueDate) {
      onChange(splitInstallments(total, count, firstDueDate, Math.max(1, intervalDays)))
    } else {
      onChange([])
    }
    // onChange muda a cada render do pai; o plano só deve ser refeito quando os parâmetros mudam.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [total, count, firstDueDate, intervalDays])

  const sum = installmentsSum(value)
  const difference = Math.round((total - sum) * 100) / 100

  function updateRow(index: number, patch: Partial<InstallmentInput>) {
    onChange(value.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  return (
    <fieldset className="space-y-3 rounded-lg border border-terra/10 bg-terreiro/40 p-3">
      <legend className="px-1 text-xs font-semibold text-terra">Parcelas</legend>
      <div className="grid grid-cols-3 gap-2">
        <div>
          <label htmlFor={`${ids}-count`} className={labelClass}>
            Quantidade
          </label>
          <input
            id={`${ids}-count`}
            type="number"
            min={1}
            max={MAX_INSTALLMENTS}
            value={count}
            onChange={(e) => setCount(Math.min(MAX_INSTALLMENTS, Math.max(1, Number(e.target.value) || 1)))}
            className={`${inputClass} py-1.5`}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-first`} className={labelClass}>
            1º vencimento
          </label>
          <input
            id={`${ids}-first`}
            type="date"
            min={operationDate}
            value={firstDueDate}
            onChange={(e) => setFirstDueDate(e.target.value)}
            className={`${inputClass} py-1.5`}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-interval`} className={labelClass}>
            A cada (dias)
          </label>
          <input
            id={`${ids}-interval`}
            type="number"
            min={1}
            max={365}
            disabled={count === 1}
            value={intervalDays}
            onChange={(e) => setIntervalDays(Math.max(1, Number(e.target.value) || 30))}
            className={`${inputClass} py-1.5 disabled:opacity-50`}
          />
        </div>
      </div>

      {value.length > 0 && (
        <table className="w-full text-sm">
          <caption className="sr-only">Vencimento e valor de cada parcela</caption>
          <thead>
            <tr className="text-left text-xs text-terra/55">
              <th className="pb-1 font-medium">Parcela</th>
              <th className="pb-1 font-medium">Vencimento</th>
              <th className="pb-1 text-right font-medium">Valor</th>
            </tr>
          </thead>
          <tbody>
            {value.map((row, index) => (
              <tr key={index}>
                <td className="py-0.5 pr-2 text-terra/60 tabular-nums">
                  {index + 1}/{value.length}
                </td>
                <td className="py-0.5 pr-2">
                  <input
                    type="date"
                    aria-label={`Vencimento da parcela ${index + 1}`}
                    min={operationDate}
                    value={row.dueDate}
                    onChange={(e) => updateRow(index, { dueDate: e.target.value })}
                    className={`${inputClass} py-1`}
                  />
                </td>
                <td className="py-0.5">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    aria-label={`Valor da parcela ${index + 1}`}
                    value={Number.isFinite(row.amount) ? row.amount : ''}
                    onChange={(e) => updateRow(index, { amount: e.target.valueAsNumber })}
                    className={`${inputClass} py-1 text-right`}
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <p className={`text-xs ${difference === 0 ? 'text-terra/55' : 'font-medium text-rust'}`} role="status">
        Soma das parcelas: {formatCurrency(sum)}
        {difference !== 0 &&
          ` — ${difference > 0 ? 'faltam' : 'sobram'} ${formatCurrency(Math.abs(difference))} para fechar o total de ${formatCurrency(total)}`}
      </p>
    </fieldset>
  )
}
