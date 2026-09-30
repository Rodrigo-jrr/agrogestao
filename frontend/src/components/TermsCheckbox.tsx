import { useId } from 'react'
import { Link } from 'react-router-dom'

/** Aceite dos termos e da política. Os links abrem em outra aba para não perder o que já foi digitado. */
export function TermsCheckbox({ checked, onChange }: { checked: boolean; onChange: (checked: boolean) => void }) {
  const id = useId()
  return (
    <div className="flex items-start gap-2 text-sm text-terra/80">
      <input
        id={id}
        type="checkbox"
        required
        // Nome completo explícito: alguns leitores de tela omitem o texto dos links dentro do rótulo.
        aria-label="Li e aceito os Termos de uso e a Política de privacidade"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="mt-0.5 h-4 w-4 shrink-0 cursor-pointer accent-rust"
      />
      <label htmlFor={id} className="cursor-pointer">
        Li e aceito os{' '}
        <Link to="/termos" target="_blank" rel="noopener" className="font-medium text-rust hover:underline">
          Termos de uso
        </Link>{' '}
        e a{' '}
        <Link to="/privacidade" target="_blank" rel="noopener" className="font-medium text-rust hover:underline">
          Política de privacidade
        </Link>
        .
      </label>
    </div>
  )
}
