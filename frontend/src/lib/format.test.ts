import { describe, expect, it } from 'vitest'
import { formatCurrency, formatDate, formatDateTime, toIsoDate } from './format'

describe('formatação', () => {
  it('formata reais no padrão brasileiro', () => {
    expect(formatCurrency(1234.5).replace(/\s/g, ' ')).toBe('R$ 1.234,50')
  })

  it('mostra a data da API sem mudar o dia por causa do fuso', () => {
    expect(formatDate('2026-09-01')).toBe('01/09/2026')
    expect(formatDate('2026-12-31')).toBe('31/12/2026')
  })

  it('mostra data e hora do servidor sem converter fuso', () => {
    expect(formatDateTime('2026-09-30T23:48:05')).toBe('30/09/2026 23:48')
  })

  it('gera a data local (não a UTC) no formato da API', () => {
    // 23h do dia 30 no horário local continua sendo dia 30, mesmo que em UTC já seja dia 1º.
    expect(toIsoDate(new Date(2026, 8, 30, 23, 30))).toBe('2026-09-30')
  })
})
