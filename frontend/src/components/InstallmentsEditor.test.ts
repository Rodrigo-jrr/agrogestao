import { describe, expect, it } from 'vitest'
import { installmentsSum, splitInstallments } from './InstallmentsEditor'

describe('parcelas', () => {
  it('divide em centavos e joga a sobra do arredondamento na última parcela', () => {
    const plan = splitInstallments(100, 3, '2026-10-10', 30)
    expect(plan.map((i) => i.amount)).toEqual([33.33, 33.33, 33.34])
    expect(installmentsSum(plan)).toBe(100)
  })

  it('espaça os vencimentos pelo intervalo, atravessando meses', () => {
    const plan = splitInstallments(90, 3, '2026-12-15', 30)
    expect(plan.map((i) => i.dueDate)).toEqual(['2026-12-15', '2027-01-14', '2027-02-13'])
  })

  it('ignora valores inválidos na soma', () => {
    expect(installmentsSum([{ dueDate: '2026-10-01', amount: 10.1 }, { dueDate: '2026-11-01', amount: Number.NaN }])).toBe(10.1)
  })
})
