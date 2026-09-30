import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { safeNext } from '../auth/safeNext'
import { apiErrorMessage } from '../api/client'
import { AuthShell } from '../components/AuthShell'
import { inputClass, primaryButtonClass } from '../components/ui'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  // Ex.: vindo da página de convite, volta para ela depois de entrar.
  const next = safeNext(searchParams.get('next'))
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email, password)
      navigate(next ?? '/dashboard')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="Entrar">
      <p className="mb-6 text-sm text-terra/60">Acesse sua propriedade</p>

      <form onSubmit={handleSubmit}>
        {error && <p className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

        <label htmlFor="login-email" className="mb-1 block text-sm font-medium text-terra">E-mail</label>
        <input
          id="login-email"
          type="email"
          required
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`mb-4 ${inputClass}`}
        />

        <div className="mb-1 flex items-baseline justify-between">
          <label htmlFor="login-password" className="block text-sm font-medium text-terra">Senha</label>
          <Link to="/esqueci-senha" className="text-xs text-terra/60 hover:text-rust hover:underline">
            Esqueci minha senha
          </Link>
        </div>
        <input
          id="login-password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={`mb-6 ${inputClass}`}
        />

        <button
          type="submit"
          disabled={loading}
          className={`w-full ${primaryButtonClass}`}
        >
          {loading ? 'Entrando...' : 'Entrar'}
        </button>

        <div className="my-4 flex items-center gap-3 text-xs text-terra/40">
          <span className="h-px flex-1 bg-terra/10" />
          ou
          <span className="h-px flex-1 bg-terra/10" />
        </div>

        <Link
          to="/register"
          className="block w-full rounded-md border border-terra/15 px-3 py-2.5 text-center text-sm font-semibold text-terra hover:bg-terra/5"
        >
          Criar conta
        </Link>
      </form>
    </AuthShell>
  )
}
