import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination'

describe('Pagination', () => {
  it('não aparece sem registros', () => {
    const { container } = render(<Pagination page={0} totalPages={0} totalElements={0} onChange={() => {}} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('navega entre páginas e trava nas pontas', async () => {
    const onChange = vi.fn()
    render(<Pagination page={0} totalPages={3} totalElements={120} onChange={onChange} />)

    expect(screen.getByText('120 registros')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(onChange).toHaveBeenCalledWith(1)
  })
})
