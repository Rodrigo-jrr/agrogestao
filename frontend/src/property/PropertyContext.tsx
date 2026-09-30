import { createContext, useContext, useCallback, useEffect, useState, type ReactNode } from 'react'
import { propertiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { PropertySummary } from '../api/types'
import { useAuth } from '../auth/AuthContext'

interface PropertyContextValue {
  properties: PropertySummary[]
  currentProperty: PropertySummary | null
  loading: boolean
  error: string | null
  selectProperty: (id: number) => void
  refresh: () => Promise<void>
}

const PropertyContext = createContext<PropertyContextValue | undefined>(undefined)

export function PropertyProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [properties, setProperties] = useState<PropertySummary[]>([])
  const [currentProperty, setCurrentProperty] = useState<PropertySummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Para qual estado de login a lista atual foi carregada. Logo após entrar, a lista ainda é a de
  // "deslogado" (vazia) até a busca terminar: sem isso, a rota protegida mandaria para o onboarding.
  const [loadedFor, setLoadedFor] = useState<boolean | null>(null)

  const applyCurrent = useCallback((list: PropertySummary[]) => {
    const storedId = localStorage.getItem('agro.propertyId')
    const found = list.find((p) => String(p.id) === storedId) ?? list[0] ?? null
    setCurrentProperty(found)
    if (found) {
      localStorage.setItem('agro.propertyId', String(found.id))
    } else {
      localStorage.removeItem('agro.propertyId')
    }
  }, [])

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setProperties([])
      setCurrentProperty(null)
      setLoading(false)
      setLoadedFor(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const list = await propertiesApi.listMine()
      setProperties(list)
      applyCurrent(list)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoadedFor(true)
      setLoading(false)
    }
  }, [isAuthenticated, applyCurrent])

  useEffect(() => {
    refresh()
  }, [refresh])

  function selectProperty(id: number) {
    const found = properties.find((p) => p.id === id) ?? null
    setCurrentProperty(found)
    if (found) {
      localStorage.setItem('agro.propertyId', String(found.id))
    }
  }

  return (
    <PropertyContext.Provider
      value={{
        properties,
        currentProperty,
        loading: loading || loadedFor !== isAuthenticated,
        error,
        selectProperty,
        refresh,
      }}
    >
      {children}
    </PropertyContext.Provider>
  )
}

export function useProperty() {
  const context = useContext(PropertyContext)
  if (!context) throw new Error('useProperty deve ser usado dentro de PropertyProvider')
  return context
}
