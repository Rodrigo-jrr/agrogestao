import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Logo'
import { primaryButtonClass } from '../components/ui'

export function NotFoundPage() {
  const { isAuthenticated } = useAuth()
  const { pathname } = useLocation()
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-terreiro px-6 text-center font-sans text-terra">
      <Logo size={28} />
      <h1 className="mt-8 font-serif text-3xl font-semibold">Página não encontrada</h1>
      <p className="mt-2 max-w-md text-sm text-terra/65">
        Não existe nada em <code className="rounded bg-terra/5 px-1 py-0.5 text-xs">{pathname}</code>. O endereço pode
        ter sido digitado errado ou a página mudou de lugar.
      </p>
      <Link to={isAuthenticated ? '/dashboard' : '/login'} className={`mt-6 ${primaryButtonClass}`}>
        {isAuthenticated ? 'Ir para o dashboard' : 'Ir para o login'}
      </Link>
    </main>
  )
}
