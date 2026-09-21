import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { apiErrorMessage } from '../api/client'
import { AuthShell } from '../components/AuthShell'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
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
      navigate('/dashboard')
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

        <label className="mb-1 block text-sm font-medium text-terra">E-mail</label>
        <input
          type="email"
          required
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <label className="mb-1 block text-sm font-medium text-terra">Senha</label>
        <input
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mb-6 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-rust px-3 py-2.5 text-sm font-semibold text-terreiro hover:bg-rust-dark disabled:opacity-60"
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
