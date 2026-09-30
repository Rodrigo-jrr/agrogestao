import { useState } from 'react'
import { meApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/** Aviso não bloqueante: o app continua funcionando sem a confirmação (só o painel de admin exige). */
export function VerifyEmailBanner() {
  const { user, emailVerified } = useAuth()
  const [state, setState] = useState<'idle' | 'sending' | 'sent'>('idle')
  const [error, setError] = useState<string | null>(null)

  if (!user || emailVerified !== false) return null

  async function resend() {
    setState('sending')
    setError(null)
    try {
      await meApi.resendVerification()
      setState('sent')
    } catch (err) {
      setState('idle')
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div role="status" className="border-b border-trigo/40 bg-trigo/15 px-4 py-2.5 text-sm text-terra">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span>
          Confirme seu e-mail <strong className="font-medium">{user.email}</strong> para poder recuperar a senha com
          segurança.
        </span>
        {state === 'sent' ? (
          <span className="text-mata">Enviamos um novo link. Confira sua caixa de entrada.</span>
        ) : (
          <button
            onClick={resend}
            disabled={state === 'sending'}
            className="cursor-pointer font-medium text-rust underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust disabled:opacity-60"
          >
            {state === 'sending' ? 'Enviando…' : 'Reenviar e-mail de confirmação'}
          </button>
        )}
        {error && <span className="text-rust">{error}</span>}
      </div>
    </div>
  )
}
