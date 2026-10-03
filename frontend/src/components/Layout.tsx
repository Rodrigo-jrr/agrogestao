import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useProperty } from '../property/PropertyContext'
import { Logo } from './Logo'
import { NewPropertyModal } from './NewPropertyModal'
import { VerifyEmailBanner } from './VerifyEmailBanner'
import { outlineButtonClass } from './ui'

const coreNav = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/financeiro', label: 'Financeiro' },
  { to: '/estoque', label: 'Estoque' },
  { to: '/producao', label: 'Produção' },
  { to: '/compras', label: 'Compras' },
  { to: '/vendas', label: 'Vendas' },
  { to: '/agricultura', label: 'Agricultura' },
  { to: '/pecuaria', label: 'Pecuária' },
  { to: '/maquinas', label: 'Máquinas' },
  { to: '/atividades', label: 'Atividades' },
  { to: '/equipe', label: 'Equipe' },
  { to: '/propriedade', label: 'Propriedade' },
]

/** Só proprietários consultam o histórico (o servidor também bloqueia). */
const ownerNav = [{ to: '/historico', label: 'Histórico' }]

export function Layout() {
  const { user, logout, isPlatformAdmin } = useAuth()
  const { properties, currentProperty, selectProperty, refresh } = useProperty()
  const [menuOpen, setMenuOpen] = useState(false)
  const [showNewProperty, setShowNewProperty] = useState(false)
  const location = useLocation()

  // No celular o menu é uma gaveta: fecha ao navegar e com Esc.
  useEffect(() => setMenuOpen(false), [location.pathname])
  useEffect(() => {
    if (!menuOpen) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setMenuOpen(false)
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [menuOpen])

  return (
    <div className="flex min-h-screen bg-terreiro font-sans">
      {/* Altura da tela inteira e fixa na rolagem: o rodapé da barra (usuário, propriedade, admin) fica sempre visível. */}
      {menuOpen && (
        <button
          type="button"
          aria-label="Fechar menu"
          onClick={() => setMenuOpen(false)}
          className="fixed inset-0 z-10 cursor-default bg-terra/40 md:hidden"
        />
      )}
      <aside
        id="app-sidebar"
        aria-label="Menu principal"
        className={`${menuOpen ? 'flex' : 'hidden'} fixed inset-y-0 left-0 z-20 w-64 flex-col bg-mata md:sticky md:top-0 md:flex md:h-screen md:shrink-0`}
      >
        <div className="px-4 py-5">
          <Logo size={26} variant="light" />
        </div>
        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-3">
          {[...coreNav, ...(currentProperty?.role === 'OWNER' ? ownerNav : [])].map((item) => (
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
        </nav>

        <div className="border-t border-terreiro/10 px-4 py-4">
          <p className="text-sm font-medium text-terreiro">{user?.name}</p>
          <p className="text-xs text-terreiro/50">{currentProperty?.name}</p>
        </div>
      </aside>

      {/* min-w-0: sem isso, tabelas largas esticam a coluna em vez de rolar dentro do próprio card. */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-terra/10 bg-terreiro px-4 py-3">
          <button
            type="button"
            aria-expanded={menuOpen}
            aria-controls="app-sidebar"
            className={`${outlineButtonClass} md:hidden`}
            onClick={() => setMenuOpen((v) => !v)}
          >
            {menuOpen ? 'Fechar' : 'Menu'}
          </button>

          <select
            className="rounded-md border border-terra/15 bg-white px-2 py-1.5 text-sm text-terra focus:border-rust focus:outline-none"
            value={currentProperty?.id ?? ''}
            onChange={(e) => {
              if (e.target.value === 'new') {
                setShowNewProperty(true)
                return
              }
              selectProperty(Number(e.target.value))
            }}
          >
            {properties.map((property) => (
              <option key={property.id} value={property.id}>
                {property.name}
              </option>
            ))}
            <option value="new">+ Nova propriedade</option>
          </select>

          <div className="flex items-center gap-3 text-sm text-terra/70">
            {isPlatformAdmin && (
              <NavLink
                to="/admin"
                className="rounded-full border border-trigo-dark/50 bg-trigo/15 px-3 py-1 text-xs font-medium text-trigo-dark hover:bg-trigo/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-trigo-dark"
              >
                Administração
              </NavLink>
            )}
            <NavLink
              to="/perfil"
              aria-label={`${user?.name ?? ''} — meu perfil`}
              className={({ isActive }) =>
                `rounded-md px-2 py-1 hover:bg-terra/5 hover:text-terra focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
                  isActive ? 'font-medium text-terra' : ''
                }`
              }
            >
              <span className="hidden sm:inline">{user?.name}</span>
              <span className="sm:hidden">Perfil</span>
            </NavLink>
            <button
              onClick={logout}
              className={outlineButtonClass}
            >
              Sair
            </button>
          </div>
        </header>

        <VerifyEmailBanner />

        <main className="flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>

      {showNewProperty && currentProperty && (
        <NewPropertyModal
          organizationId={currentProperty.organizationId}
          onClose={() => setShowNewProperty(false)}
          onCreated={async (propertyId) => {
            localStorage.setItem('agro.propertyId', String(propertyId))
            await refresh()
            setShowNewProperty(false)
          }}
        />
      )}
    </div>
  )
}
