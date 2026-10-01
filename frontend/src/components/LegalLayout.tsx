import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Logo } from './Logo'
import { linkNeutralClass } from './ui'
import { TERMS_UPDATED_AT } from '../lib/legal'

/** Página de leitura (termos, privacidade): coluna estreita, texto em tamanho confortável. */
export function LegalLayout({ title, children }: { title: string; children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  return (
    <div className="min-h-screen bg-terreiro font-sans text-terra">
      <header className="border-b border-terra/10 px-4 py-4 md:px-8">
        <Link to="/" aria-label="AgroGestão — início" className="inline-block">
          <Logo size={24} />
        </Link>
      </header>
      <main className="mx-auto max-w-2xl px-4 py-10 md:py-14">
        {/* Página acessada direto (sem sessão ou pelo link do rodapé de login/cadastro), sem histórico pra voltar. */}
        <Link to={isAuthenticated ? '/dashboard' : '/login'} className={`text-sm ${linkNeutralClass}`}>
          {isAuthenticated ? '← Voltar' : '← Voltar para o login'}
        </Link>
        <h1 className="mt-4 font-serif text-3xl font-semibold md:text-4xl">{title}</h1>
        <p className="mt-2 text-sm text-terra/60">Atualizado em {TERMS_UPDATED_AT}.</p>
        <div className="mt-8 space-y-4 text-base leading-relaxed text-terra/85">{children}</div>
        <nav aria-label="Documentos" className="mt-12 flex flex-wrap gap-x-6 gap-y-2 border-t border-terra/10 pt-6 text-sm">
          <Link to="/termos" className="text-rust hover:underline">
            Termos de uso
          </Link>
          <Link to="/privacidade" className="text-rust hover:underline">
            Política de privacidade
          </Link>
        </nav>
      </main>
    </div>
  )
}

export function LegalSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-3 pt-4">
      <h2 className="font-serif text-xl font-semibold text-terra">{title}</h2>
      {children}
    </section>
  )
}

/** Dado do responsável; destacado enquanto ainda não foi preenchido. */
export function LegalField({ value }: { value: string }) {
  if (value.startsWith('[')) {
    return <mark className="rounded bg-trigo/30 px-1 text-terra">{value}</mark>
  }
  return <>{value}</>
}
