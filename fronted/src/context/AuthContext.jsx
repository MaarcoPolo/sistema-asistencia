import { createContext, useState, useContext, useEffect } from 'react'
import { jwtDecode } from 'jwt-decode'
import apiClient from '../services/api'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [authData, setAuthData] = useState(null)
  const [isAuthLoading, setAuthLoading] = useState(true) // Inicia en 'true'

  useEffect(() => {
    const token = localStorage.getItem('token')
    const user = localStorage.getItem('user')
    try {
      if (token && user) {
        const decodedToken = jwtDecode(token)
        if (decodedToken.exp * 1000 > Date.now()) {
          apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
          setAuthData({ token, user: JSON.parse(user) })
        } else {
          localStorage.clear() // Limpia si el token ha expirado
        }
      }
    } catch (e) {
      console.error('Token en localStorage inválido, limpiando sesión:', e)
      localStorage.clear()
    } finally {
      setAuthLoading(false) // La verificación SIEMPRE termina
    }
  }, [])

  const login = (data) => {
    if (!data || !data.token || !data.user) {
      console.error('Intento de login con datos inválidos:', data)
      return
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${data.token}`
    setAuthData(data)
  }

  const logout = () => {
    localStorage.clear()
    delete apiClient.defaults.headers.common['Authorization']
    setAuthData(null)
  }

  const value = { authData, login, logout, isAuthLoading }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  return useContext(AuthContext)
}
