import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { AuthShell } from '../components/AuthShell'
import { inputClass, primaryButtonClass } from '../components/ui'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await authApi.forgotPassword(email)
      setSentTo(email)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (sentTo) {
    return (
      <AuthShell title="Confira seu e-mail">
        <div role="status" className="space-y-4 text-sm text-terra/75">
          <p>
            Se existir uma conta com <strong className="text-terra">{sentTo}</strong>, enviamos um link para você criar
            uma nova senha. O link vale por 1 hora.
          </p>
          <p>Não chegou? Veja a caixa de spam ou espere alguns minutos antes de pedir de novo.</p>
        </div>
        <Link to="/login" className={`mt-6 block w-full text-center ${primaryButtonClass}`}>
          Voltar para o login
        </Link>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Esqueci minha senha">
      <p className="mb-6 text-sm text-terra/60">Informe o e-mail da sua conta. Vamos enviar um link para criar uma senha nova.</p>
      <form onSubmit={handleSubmit}>
        {error && <p role="alert" className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}
        <label htmlFor="forgot-email" className="mb-1 block text-sm font-medium text-terra">
          E-mail
        </label>
        <input
          id="forgot-email"
          type="email"
          required
          autoComplete="email"
          autoFocus
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`mb-6 ${inputClass}`}
        />
        <button type="submit" disabled={loading} className={`w-full ${primaryButtonClass}`}>
          {loading ? 'Enviando…' : 'Enviar link'}
        </button>
        <Link to="/login" className="mt-4 block text-center text-sm text-terra/60 hover:text-terra hover:underline">
          Lembrei a senha, voltar para o login
        </Link>
      </form>
    </AuthShell>
  )
}
