import { useState, type FormEvent } from 'react'
import { meApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { TERMS_VERSION } from '../lib/legal'
import { AuthShell } from './AuthShell'
import { FormMessage } from './FormMessage'
import { TermsCheckbox } from './TermsCheckbox'
import { outlineButtonClass, primaryButtonClass } from './ui'

/** Quem ainda não aceitou a versão atual dos termos (conta antiga ou termos atualizados) aceita antes de continuar. */
export function TermsGate() {
  const { refreshMe, logout } = useAuth()
  const [checked, setChecked] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function accept(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await meApi.acceptTerms(TERMS_VERSION)
      await refreshMe()
    } catch (err) {
      setError(apiErrorMessage(err))
      setSaving(false)
    }
  }

  return (
    <AuthShell title="Termos atualizados">
      <p className="mb-6 text-sm leading-relaxed text-terra/70">
        Para continuar usando o AgroGestão, leia e aceite os Termos de uso e a Política de privacidade. Eles explicam
        como seus dados são usados e protegidos, e como baixar ou excluir seus dados quando quiser.
      </p>
      <form onSubmit={accept} className="space-y-4">
        <FormMessage error={error} />
        <TermsCheckbox checked={checked} onChange={setChecked} />
        <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
          {saving ? 'Salvando…' : 'Aceitar e continuar'}
        </button>
        <button type="button" onClick={logout} className={`w-full ${outlineButtonClass}`}>
          Sair
        </button>
      </form>
    </AuthShell>
  )
}
