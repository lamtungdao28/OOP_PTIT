import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authService } from '@/services'
import { TOKEN_KEY } from '@/services/apiClient'
import type { CurrentUser, LoginResponse, Role } from '@/types/api'

interface AuthContextValue {
  user: CurrentUser | null
  loading: boolean
  login: (username: string, password: string) => Promise<LoginResponse>
  logout: () => void
  hasRole: (role: Role) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(() => Boolean(sessionStorage.getItem(TOKEN_KEY)))

  useEffect(() => {
    if (!sessionStorage.getItem(TOKEN_KEY)) return
    authService.me().then(setUser).catch(() => sessionStorage.removeItem(TOKEN_KEY)).finally(() => setLoading(false))
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    login: async (username, password) => {
      const response = await authService.login({ username, password })
      sessionStorage.setItem(TOKEN_KEY, response.accessToken)
      setUser(response.user)
      return response
    },
    logout: () => { sessionStorage.removeItem(TOKEN_KEY); setUser(null); window.location.assign('/login') },
    hasRole: (role) => Boolean(user?.roles.includes(role)),
  }), [user, loading])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
