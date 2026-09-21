import { createContext, useContext, useState, type ReactNode } from 'react'
import { authApi } from '../api/endpoints'

interface AuthUser {
  userId: number
  name: string
  email: string
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem('agro.user')
  return raw ? (JSON.parse(raw) as AuthUser) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser())

  function persist(response: { token: string; userId: number; name: string; email: string }) {
    localStorage.setItem('agro.token', response.token)
    const authUser = { userId: response.userId, name: response.name, email: response.email }
    localStorage.setItem('agro.user', JSON.stringify(authUser))
    setUser(authUser)
  }

  async function login(email: string, password: string) {
    const response = await authApi.login({ email, password })
    persist(response)
  }

  async function register(name: string, email: string, password: string) {
    const response = await authApi.register({ name, email, password })
    persist(response)
  }

  function logout() {
    localStorage.removeItem('agro.token')
    localStorage.removeItem('agro.user')
    localStorage.removeItem('agro.propertyId')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return context
}
