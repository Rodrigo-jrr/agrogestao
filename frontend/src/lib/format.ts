/** Formatação no padrão brasileiro e datas no fuso local (evita o "amanhã" do toISOString após 21h). */

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const number = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 })
const date = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' })

export function formatCurrency(value: number) {
  return currency.format(value)
}

export function formatNumber(value: number) {
  return number.format(value)
}

/** "2026-09-10" -> "10/09/2026" (a data da API não tem fuso; tratada como UTC para não mudar o dia). */
export function formatDate(isoDate: string) {
  return date.format(new Date(`${isoDate.slice(0, 10)}T00:00:00Z`))
}

/** Data local no formato da API (AAAA-MM-DD). */
export function toIsoDate(value: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

export function todayIso() {
  return toIsoDate(new Date())
}

/** "2026-09-30T15:48:23" (horário do servidor, sem fuso) -> "30/09/2026 15:48". */
export function formatDateTime(isoDateTime: string) {
  const time = isoDateTime.slice(11, 16)
  return time ? `${formatDate(isoDateTime)} ${time}` : formatDate(isoDateTime)
}
