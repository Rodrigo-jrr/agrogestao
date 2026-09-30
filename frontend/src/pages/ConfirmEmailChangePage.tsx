import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useUrlToken } from '../auth/useUrlToken'
import { AuthShell } from '../components/AuthShell'
import { primaryButtonClass } from '../components/ui'

type Status = 'checking' | 'confirmed' | 'failed'

/** Link enviado ao endereço novo na troca de e-mail. Funciona com ou sem login. */
export function ConfirmEmailChangePage() {
  const token = useUrlToken()
  const { isAuthenticated, refreshMe } = useAuth()
  const [status, setStatus] = useState<Status>(token ? 'checking' : 'failed')
  const [error, setError] = useState<string | null>(token ? null : 'Este endereço não traz o código de confirmação.')
  const started = useRef(false)

  useEffect(() => {
    // Uso único: evita a segunda chamada do StrictMode em desenvolvimento.
    if (!token || started.current) return
    started.current = true
    authApi
      .confirmEmailChange(token)
      .then(() => {
        setStatus('confirmed')
        refreshMe()
      })
      .catch((err) => {
        setStatus('failed')
        setError(apiErrorMessage(err))
      })
  }, [token, refreshMe])

  const next = isAuthenticated ? { to: '/perfil', label: 'Ver meu perfil' } : { to: '/login', label: 'Entrar' }

  return (
    <AuthShell
      title={status === 'confirmed' ? 'E-mail alterado' : status === 'checking' ? 'Confirmando…' : 'Não foi possível confirmar'}
    >
      <p role="status" className="mb-6 text-sm text-terra/70">
        {status === 'checking' && 'Só um instante.'}
        {status === 'confirmed' && 'Pronto! A partir de agora, entre na sua conta com o novo e-mail.'}
        {status === 'failed' && error}
      </p>
      {status !== 'checking' && (
        <Link to={next.to} className={`block w-full text-center ${primaryButtonClass}`}>
          {next.label}
        </Link>
      )}
    </AuthShell>
  )
}
