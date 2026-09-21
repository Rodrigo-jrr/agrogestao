import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useProperty } from '../property/PropertyContext'

export function ProtectedRoute() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Outlet />
}

export function RequirePropertyRoute() {
  const { properties, loading, error, refresh } = useProperty()
  if (loading) return <div className="p-6 text-terra/50">Carregando...</div>
  if (error) {
    return (
      <div className="p-6">
        <p className="mb-3 text-sm text-rust">{error}</p>
        <button
          onClick={refresh}
          className="rounded-md border border-terra/15 px-3 py-1.5 text-sm text-terra hover:bg-terra/5"
        >
          Tentar novamente
        </button>
      </div>
    )
  }
  if (properties.length === 0) return <Navigate to="/onboarding" replace />
  return <Outlet />
}
