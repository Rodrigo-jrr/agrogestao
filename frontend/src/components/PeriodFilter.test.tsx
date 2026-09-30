import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { PeriodFilter, presetPeriod } from './PeriodFilter'

describe('PeriodFilter', () => {
  const today = new Date(2026, 1, 10) // 10/02/2026

  it('calcula os atalhos a partir de hoje', () => {
    expect(presetPeriod('thisMonth', today)).toEqual({ from: '2026-02-01', to: '2026-02-28' })
    expect(presetPeriod('lastMonth', today)).toEqual({ from: '2026-01-01', to: '2026-01-31' })
    expect(presetPeriod('last3Months', today)).toEqual({ from: '2025-12-01', to: '2026-02-28' })
    expect(presetPeriod('thisYear', today)).toEqual({ from: '2026-01-01', to: '2026-12-31' })
  })

  it('marca o atalho ativo e avisa a troca', async () => {
    const onChange = vi.fn()
    render(<PeriodFilter value={presetPeriod('thisMonth')} onChange={onChange} />)

    expect(screen.getByRole('button', { name: 'Este mês' })).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(screen.getByRole('button', { name: 'Este ano' }))
    expect(onChange).toHaveBeenCalledWith(presetPeriod('thisYear'))
  })
})
