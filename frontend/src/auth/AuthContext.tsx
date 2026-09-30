import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { authApi, meApi } from '../api/endpoints'
import type { AuthResponse, MeInfo } from '../api/types'

interface AuthUser {
  userId: number
  name: string
  email: string
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  /** Só controla o que a interface mostra; o servidor valida o acesso de admin em toda requisição. */
  isPlatformAdmin: boolean
  /** null enquanto ainda não carregou. */
  emailVerified: boolean | null
  /** E-mail novo aguardando confirmação. */
  pendingEmail: string | null
  /** false: precisa aceitar a versão atual dos termos; null enquanto carrega. */
  termsAccepted: boolean | null
  refreshMe: () => Promise<void>
  /** Guarda um token recebido fora do login (troca de senha, cadastro por convite). */
  startSession: (response: AuthResponse) => void
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string, termsVersion: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem('agro.user')
  return raw ? (JSON.parse(raw) as AuthUser) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser())
  const [me, setMe] = useState<MeInfo | null>(null)
  // Ignora dados de outro usuário (ex.: logo após trocar de conta).
  const currentMe = user && me?.userId === user.userId ? me : null

  const userId = user?.userId
  const refreshMe = useCallback(async () => {
    if (!userId) return
    try {
      const info = await meApi.get()
      setMe(info)
      // Nome e e-mail podem ter mudado (perfil, troca de e-mail confirmada em outro aparelho).
      setUser((current) => {
        if (!current || current.userId !== info.userId) return current
        if (current.name === info.name && current.email === info.email) return current
        const updated = { ...current, name: info.name, email: info.email }
        localStorage.setItem('agro.user', JSON.stringify(updated))
        return updated
      })
    } catch {
      setMe(null)
    }
  }, [userId])

  useEffect(() => {
    refreshMe()
  }, [refreshMe])

  function persist(response: AuthResponse) {
    localStorage.setItem('agro.token', response.token)
    const authUser = { userId: response.userId, name: response.name, email: response.email }
    localStorage.setItem('agro.user', JSON.stringify(authUser))
    setUser(authUser)
  }

  async function login(email: string, password: string) {
    const response = await authApi.login({ email, password })
    persist(response)
  }

  async function register(name: string, email: string, password: string, termsVersion: string) {
    const response = await authApi.register({ name, email, password, termsVersion })
    persist(response)
  }

  function logout() {
    localStorage.removeItem('agro.token')
    localStorage.removeItem('agro.user')
    localStorage.removeItem('agro.propertyId')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{
        user,
        isAuthenticated: !!user,
        isPlatformAdmin: currentMe?.platformAdmin ?? false,
        emailVerified: currentMe ? currentMe.emailVerified : null,
        pendingEmail: currentMe?.pendingEmail ?? null,
        termsAccepted: currentMe ? currentMe.termsAccepted : null,
        refreshMe,
        startSession: persist,
        login,
        register,
        logout,
      }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return context
}
