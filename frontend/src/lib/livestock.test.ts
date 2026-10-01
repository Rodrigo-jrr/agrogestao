import { describe, expect, it } from 'vitest'
import { formatAge, healthTitle, overallDailyGain, parseTagList, relativeDay, weightHistory } from './livestock'

describe('formatAge', () => {
  const today = new Date(2026, 9, 1) // 01/10/2026

  it('mostra dias, meses ou anos e meses', () => {
    expect(formatAge('2026-09-16', today)).toBe('15 dias')
    expect(formatAge('2026-09-30', today)).toBe('1 dia')
    expect(formatAge('2026-02-01', today)).toBe('8 meses')
    expect(formatAge('2024-07-01', today)).toBe('2 anos e 3 meses')
    expect(formatAge('2025-10-01', today)).toBe('1 ano')
    expect(formatAge(null, today)).toBe('—')
  })

  it('só completa o mês no dia do aniversário', () => {
    expect(formatAge('2026-09-02', today)).toBe('29 dias')
    expect(formatAge('2026-09-01', today)).toBe('1 mês')
    expect(formatAge('2026-08-02', today)).toBe('1 mês')
  })
})

describe('parseTagList', () => {
  it('lê um brinco por linha, com nome opcional separado por ; , ou tab', () => {
    expect(parseTagList('101\n 102; Mimosa \n\n103,Estrela\n104\tBoneca')).toEqual([
      { tag: '101', name: null },
      { tag: '102', name: 'Mimosa' },
      { tag: '103', name: 'Estrela' },
      { tag: '104', name: 'Boneca' },
    ])
  })

  it('ignora linhas vazias e sem brinco', () => {
    expect(parseTagList('\n ; sem brinco\n')).toEqual([])
  })
})

describe('weightHistory', () => {
  const event = (id: number, date: string, weight: number | null, type: 'ENTRADA' | 'PESAGEM' = 'PESAGEM') => ({
    id,
    type,
    date,
    animalId: 1,
    animalTag: '1',
    batchId: null,
    batchName: null,
    headCount: null,
    weight,
    fromBatchName: null,
    toBatchName: null,
    tradeId: null,
    amount: null,
    description: null,
    applicationId: null,
    withdrawalUntil: null,
  })

  it('ordena por data e calcula o ganho médio diário entre pesagens', () => {
    const points = weightHistory([
      event(3, '2026-09-30', 230),
      event(1, '2026-08-01', 200, 'ENTRADA'),
      event(2, '2026-09-01', 215),
      event(4, '2026-09-15', null),
    ])
    expect(points.map((p) => p.weight)).toEqual([200, 215, 230])
    expect(points.map((p) => p.dailyGain)).toEqual([null, 0.484, 0.517])
    expect(overallDailyGain(points)).toBe(0.5)
  })

  it('sem dois pesos em datas diferentes não há ganho', () => {
    expect(overallDailyGain(weightHistory([event(1, '2026-09-01', 200)]))).toBeNull()
  })
})

describe('relativeDay', () => {
  it('fala como gente', () => {
    expect(relativeDay('2026-10-01', '2026-10-01')).toBe('hoje')
    expect(relativeDay('2026-10-02', '2026-10-01')).toBe('amanhã')
    expect(relativeDay('2026-10-04', '2026-10-01')).toBe('em 3 dias')
    expect(relativeDay('2026-09-29', '2026-10-01')).toBe('há 2 dias')
  })
})

describe('healthTitle', () => {
  it('não repete o tipo quando o nome já começa com ele', () => {
    expect(healthTitle('VACINA', 'Vacina Aftosa')).toBe('Vacina Aftosa')
    expect(healthTitle('VACINA', 'Raiva')).toBe('vacina Raiva')
    expect(healthTitle('MEDICAMENTO', 'Ivermectina', true)).toBe('Medicamento Ivermectina')
  })
})
