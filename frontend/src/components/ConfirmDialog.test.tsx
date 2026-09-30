import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { ConfirmProvider, useConfirm } from './ConfirmDialog'

function Harness() {
  const confirm = useConfirm()
  const [result, setResult] = useState('nada')
  return (
    <>
      <button
        onClick={async () =>
          setResult(
            String(
              await confirm({
                title: 'Excluir produto?',
                message: 'O histórico continua.',
                confirmLabel: 'Excluir produto',
              }),
            ),
          )
        }
      >
        Abrir
      </button>
      <p>resultado: {result}</p>
    </>
  )
}

function setup() {
  render(
    <ConfirmProvider>
      <Harness />
    </ConfirmProvider>,
  )
}

describe('ConfirmDialog', () => {
  it('confirma com o botão que nomeia a ação', async () => {
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir' }))

    expect(screen.getByRole('heading', { name: 'Excluir produto?' })).toBeInTheDocument()
    expect(screen.getByText('O histórico continua.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Excluir produto' }))
    expect(await screen.findByText('resultado: true')).toBeInTheDocument()
  })

  it('começa com o foco em "Voltar", que cancela', async () => {
    setup()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir' }))

    const back = screen.getByRole('button', { name: 'Voltar' })
    expect(back).toHaveFocus()
    await userEvent.click(back)
    expect(await screen.findByText('resultado: false')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Excluir produto?' })).not.toBeInTheDocument()
  })
})
