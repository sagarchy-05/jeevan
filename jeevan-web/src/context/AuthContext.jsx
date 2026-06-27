import { createContext, useCallback, useContext, useState } from 'react'
import { ApiError, apiRequest } from '../api/client'

const AuthContext = createContext(null)

const TOKEN_KEY = 'jeevan_token'
const USER_KEY = 'jeevan_user'

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  })

  const login = useCallback((newToken, newUser) => {
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
    setToken(newToken)
    setUser(newUser)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
  }, [])

  /** Authenticated request that clears the session on 401 / expired token. */
  const authedRequest = useCallback(
    async (path, options = {}) => {
      try {
        return await apiRequest(path, { ...options, token: localStorage.getItem(TOKEN_KEY) })
      } catch (err) {
        if (err instanceof ApiError && (err.status === 401 || err.code === 'TOKEN_EXPIRED')) {
          logout()
        }
        throw err
      }
    },
    [logout],
  )

  return (
    <AuthContext.Provider
      value={{ token, user, isAuthenticated: !!token, login, logout, authedRequest }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
