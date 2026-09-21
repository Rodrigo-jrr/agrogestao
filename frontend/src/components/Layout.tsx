import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useProperty } from '../property/PropertyContext'
import { Logo } from './Logo'

const coreNav = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/financeiro', label: 'Financeiro' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/producao', label: 'Produção' },
  { to: '/compras', label: 'Compras' },
  { to: '/vendas', label: 'Vendas' },
  { to: '/atividades', label: 'Atividades' },
]

const comingSoonNav = ['Pecuária', 'Agricultura']

export function Layout() {
  const { user, logout } = useAuth()
  const { properties, currentProperty, selectProperty } = useProperty()
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="flex min-h-screen bg-terreiro font-sans">
      <aside
        className={`${menuOpen ? 'block' : 'hidden'} md:block fixed md:static z-20 h-full w-64 bg-mata`}
      >
        <div className="px-4 py-5">
          <Logo size={26} variant="light" />
        </div>
        <nav className="flex flex-col gap-1 px-3">
          {coreNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive ? 'bg-rust text-terreiro' : 'text-terreiro/70 hover:bg-white/5 hover:text-terreiro'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
          <div className="mt-4 border-t border-terreiro/10 pt-3">
            {comingSoonNav.map((label) => (
              <div key={label} className="cursor-not-allowed rounded-md px-3 py-2 text-sm text-terreiro/35">
                {label} <span className="text-xs">(em breve)</span>
              </div>
            ))}
          </div>
        </nav>

        <div className="absolute bottom-0 w-64 border-t border-terreiro/10 px-4 py-4">
          <p className="text-sm font-medium text-terreiro">{user?.name}</p>
          <p className="text-xs text-terreiro/50">{currentProperty?.name}</p>
        </div>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-terra/10 bg-terreiro px-4 py-3">
          <button
            className="rounded-md border border-terra/15 px-3 py-1.5 text-sm md:hidden"
            onClick={() => setMenuOpen((v) => !v)}
          >
            Menu
          </button>

          <select
            className="rounded-md border border-terra/15 bg-white px-2 py-1.5 text-sm text-terra focus:border-rust focus:outline-none"
            value={currentProperty?.id ?? ''}
            onChange={(e) => selectProperty(Number(e.target.value))}
          >
            {properties.map((property) => (
              <option key={property.id} value={property.id}>
                {property.name}
              </option>
            ))}
          </select>

          <div className="flex items-center gap-3 text-sm text-terra/70">
            <span className="hidden sm:inline">{user?.name}</span>
            <button
              onClick={logout}
              className="rounded-md border border-terra/15 px-3 py-1.5 hover:bg-terra/5"
            >
              Sair
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
