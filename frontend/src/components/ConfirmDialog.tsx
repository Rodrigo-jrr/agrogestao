import { createContext, useCallback, useContext, useEffect, useId, useRef, useState, type ReactNode } from 'react'
import { outlineButtonClass } from './ui'

export interface ConfirmOptions {
  title: string
  /** O que vai acontecer, em linguagem direta (consequências e se dá para desfazer). */
  message: ReactNode
  /** Verbo da ação, ex.: "Excluir produto". Nunca "OK". */
  confirmLabel: string
  cancelLabel?: string
  /** "danger" para ações que apagam ou desfazem algo. */
  tone?: 'danger' | 'default'
}

type Confirm = (options: ConfirmOptions) => Promise<boolean>

const ConfirmContext = createContext<Confirm | undefined>(undefined)

/**
 * Diálogo de confirmação do app (substitui window.confirm). Usa o <dialog> nativo com showModal():
 * foco preso no diálogo, Esc cancela, fundo escurecido e foco devolvido ao botão que abriu.
 */
export function ConfirmProvider({ children }: { children: ReactNode }) {
  const [options, setOptions] = useState<ConfirmOptions | null>(null)
  const resolver = useRef<((value: boolean) => void) | null>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const cancelRef = useRef<HTMLButtonElement>(null)
  const returnFocus = useRef<HTMLElement | null>(null)
  const ids = useId()

  const confirm = useCallback<Confirm>((next) => {
    returnFocus.current = document.activeElement as HTMLElement | null
    setOptions(next)
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve
    })
  }, [])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog || !options) return
    if (!dialog.open) dialog.showModal()
    // Foco começa em "Cancelar": Enter por engano não executa uma ação destrutiva.
    cancelRef.current?.focus()
  }, [options])

  function close(result: boolean) {
    dialogRef.current?.close()
    setOptions(null)
    resolver.current?.(result)
    resolver.current = null
    returnFocus.current?.focus()
  }

  const danger = options?.tone !== 'default'

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <dialog
        ref={dialogRef}
        role="alertdialog"
        aria-labelledby={`${ids}-title`}
        aria-describedby={`${ids}-message`}
        onCancel={(e) => {
          e.preventDefault()
          close(false)
        }}
        onClick={(e) => {
          // Clique no fundo (fora do conteúdo) cancela.
          if (e.target === dialogRef.current) close(false)
        }}
        className="m-auto w-[min(28rem,calc(100vw-2rem))] rounded-2xl border border-terra/10 bg-white p-0 text-terra shadow-xl backdrop:bg-terra/40 motion-safe:backdrop:transition-opacity"
      >
        {options && (
          <div className="p-5">
            <h2 id={`${ids}-title`} className="font-serif text-xl font-semibold">
              {options.title}
            </h2>
            <div id={`${ids}-message`} className="mt-2 text-sm leading-relaxed text-terra/75">
              {options.message}
            </div>
            <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button ref={cancelRef} type="button" onClick={() => close(false)} className={`cursor-pointer ${outlineButtonClass}`}>
                {options.cancelLabel ?? 'Voltar'}
              </button>
              <button
                type="button"
                onClick={() => close(true)}
                className={`cursor-pointer rounded-md px-3 py-2 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 ${
                  danger
                    ? 'bg-rust text-terreiro hover:bg-rust-dark focus-visible:ring-rust'
                    : 'bg-mata text-terreiro hover:bg-mata-dark focus-visible:ring-mata'
                }`}
              >
                {options.confirmLabel}
              </button>
            </div>
          </div>
        )}
      </dialog>
    </ConfirmContext.Provider>
  )
}

export function useConfirm(): Confirm {
  const context = useContext(ConfirmContext)
  if (!context) throw new Error('useConfirm deve ser usado dentro de ConfirmProvider')
  return context
}
