import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { apiErrorMessage } from '../api/client'
import { AuthShell } from '../components/AuthShell'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await register(name, email, password)
      navigate('/onboarding')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title="Criar conta">
      <p className="mb-6 text-sm text-terra/60">Comece a organizar sua propriedade</p>

      <form onSubmit={handleSubmit}>
        {error && <p className="mb-4 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{error}</p>}

        <label className="mb-1 block text-sm font-medium text-terra">Nome</label>
        <input
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="mb-4 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

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
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mb-6 w-full rounded-md border border-terra/15 bg-white px-3 py-2 text-sm text-terra placeholder:text-terra/40 focus:border-rust focus:outline-none focus:ring-1 focus:ring-rust"
        />

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-rust px-3 py-2.5 text-sm font-semibold text-terreiro hover:bg-rust-dark disabled:opacity-60"
        >
          {loading ? 'Criando...' : 'Criar conta'}
        </button>

        <p className="mt-4 text-center text-sm text-terra/60">
          Já tem conta?{' '}
          <Link to="/login" className="font-medium text-rust hover:underline">
            Entrar
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}
