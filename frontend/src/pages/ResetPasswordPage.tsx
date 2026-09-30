import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { useUrlToken } from '../auth/useUrlToken'
import { AuthShell } from '../components/AuthShell'
import { inputClass, primaryButtonClass } from '../components/ui'

const MIN_LENGTH = 8

export function ResetPasswordPage() {
  const token = useUrlToken()
  const { logout } = useAuth()
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  const mismatch = confirmation.length > 0 && confirmation !== password

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    if (password !== confirmation) {
      setError('As senhas não conferem.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      await authApi.resetPassword(token, password)
      // Sessões antigas deixam de valer no servidor; limpamos a local também.
      logout()
      setDone(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <AuthShell title="Link incompleto">
        <p className="mb-6 text-sm text-terra/70">
          Este endereço não traz o código de redefinição. Abra o link direto do e-mail ou peça um novo.
        </p>
        <Link to="/esqueci-senha" className={`block w-full text-center ${primaryButtonClass}`}>
          Pedir novo link
        </Link>
      </AuthShell>
    )
  }

  if (done) {
    return (
      <AuthShell title="Senha alterada">
        <p role="status" className="mb-6 text-sm text-terra/70">
          Pronto. Use a nova senha para entrar. Por segurança, encerramos as sessões que estavam abertas em outros
          aparelhos.
        </p>
        <Link to="/login" className={`block w-full text-center ${primaryButtonClass}`}>
          Entrar
        </Link>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Criar nova senha">
      <p className="mb-6 text-sm text-terra/60">Escolha uma senha com pelo menos {MIN_LENGTH} caracteres.</p>
      <form onSubmit={handleSubmit}>
        {error && (
          <div role="alert" className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
            {error}{' '}
            {error.includes('expirado') && (
              <Link to="/esqueci-senha" className="font-medium underline">
                Pedir novo link
              </Link>
            )}
          </div>
        )}
        <label htmlFor="reset-password" className="mb-1 block text-sm font-medium text-terra">
          Nova senha
        </label>
        <input
          id="reset-password"
          type="password"
          required
          minLength={MIN_LENGTH}
          maxLength={72}
          autoComplete="new-password"
          autoFocus
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={`mb-4 ${inputClass}`}
        />
        <label htmlFor="reset-confirmation" className="mb-1 block text-sm font-medium text-terra">
          Repita a nova senha
        </label>
        <input
          id="reset-confirmation"
          type="password"
          required
          autoComplete="new-password"
          aria-invalid={mismatch}
          aria-describedby={mismatch ? 'reset-mismatch' : undefined}
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          className={`${inputClass} ${mismatch ? 'border-rust' : ''}`}
        />
        <p id="reset-mismatch" className="mt-1 mb-5 min-h-5 text-xs text-rust">
          {mismatch ? 'As senhas não conferem.' : ''}
        </p>
        <button type="submit" disabled={loading} className={`w-full ${primaryButtonClass}`}>
          {loading ? 'Salvando…' : 'Salvar nova senha'}
        </button>
      </form>
    </AuthShell>
  )
}
