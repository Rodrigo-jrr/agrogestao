import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from './Logo'

const highlights = [
  'Financeiro, estoque e produção integrados',
  'Compras e vendas com atualização automática de saldo',
  'Uma conta, várias propriedades',
]

interface AuthShellProps {
  title: string
  children: ReactNode
}

export function AuthShell({ title, children }: AuthShellProps) {
  return (
    <div className="flex min-h-screen font-sans">
      <div className="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-mata p-10 text-terreiro md:flex">
        <Logo size={26} variant="light" />

        <div>
          <h2 className="mb-6 font-serif text-4xl font-semibold leading-tight">
            Sua propriedade, organizada de ponta a ponta.
          </h2>
          <ul className="space-y-3">
            {highlights.map((item) => (
              <li key={item} className="flex items-center gap-2 text-sm text-terreiro/85">
                <span className="text-trigo">✓</span>
                {item}
              </li>
            ))}
          </ul>
        </div>

        <p className="text-xs uppercase tracking-widest text-terreiro/50">
          Propriedade rural — Gestão digital
        </p>
      </div>

      <div className="flex w-full flex-col items-center justify-center bg-terreiro px-6 py-12 md:w-1/2">
        <div className="w-full max-w-sm">
          <div className="mb-8 md:hidden">
            <Logo size={26} />
          </div>
          <h1 className="mb-1 font-serif text-3xl font-semibold text-terra">{title}</h1>
          {children}
          <nav aria-label="Documentos" className="mt-10 flex gap-4 text-xs text-terra/50">
            <Link to="/termos" className="hover:text-terra hover:underline">
              Termos de uso
            </Link>
            <Link to="/privacidade" className="hover:text-terra hover:underline">
              Política de privacidade
            </Link>
          </nav>
        </div>
      </div>
    </div>
  )
}
