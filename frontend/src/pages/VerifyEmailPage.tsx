import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useUrlToken } from '../auth/useUrlToken'
import { AuthShell } from '../components/AuthShell'
import { primaryButtonClass } from '../components/ui'

type Status = 'checking' | 'verified' | 'failed'

export function VerifyEmailPage() {
  const token = useUrlToken()
  const { isAuthenticated, refreshMe } = useAuth()
  const [status, setStatus] = useState<Status>(token ? 'checking' : 'failed')
  const [error, setError] = useState<string | null>(token ? null : 'Este endereço não traz o código de confirmação.')
  const started = useRef(false)

  useEffect(() => {
    // O token é de uso único: evita a segunda chamada do StrictMode em desenvolvimento.
    if (!token || started.current) return
    started.current = true
    authApi
      .verifyEmail(token)
      .then(() => {
        setStatus('verified')
        refreshMe()
      })
      .catch((err) => {
        setStatus('failed')
        setError(apiErrorMessage(err))
      })
  }, [token, refreshMe])

  const next = isAuthenticated ? { to: '/dashboard', label: 'Ir para o painel' } : { to: '/login', label: 'Entrar' }

  return (
    <AuthShell title={status === 'verified' ? 'E-mail confirmado' : status === 'checking' ? 'Confirmando…' : 'Não foi possível confirmar'}>
      <p role="status" className="mb-6 text-sm text-terra/70">
        {status === 'checking' && 'Só um instante.'}
        {status === 'verified' && 'Obrigado! Seu e-mail está confirmado.'}
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
