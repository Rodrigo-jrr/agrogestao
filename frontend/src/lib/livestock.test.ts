import { describe, expect, it } from 'vitest'
import { formatAge, parseTagList } from './livestock'

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
