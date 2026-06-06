import { createContext, useState, useContext, useEffect } from 'react'
import { jwtDecode } from 'jwt-decode'
import apiClient, { registerLogout } from '../services/api'
import { logoutRemoto } from '../services/authService'

const AuthContext = createContext(null)

/**
 * Proveedor de contexto de autenticación.
 *
 * Responsabilidades:
 * - Persistir y restaurar la sesión desde localStorage al recargar la página.
 * - Exponer las funciones login/logout para los componentes hijos.
 * - Registrar el callback de logout en el interceptor de axios (api.js) para
 *   que pueda forzar el cierre de sesión cuando el refresh token expira.
 */
export const AuthProvider = ({ children }) => {
  const [authData, setAuthData] = useState(null)
  const [isAuthLoading, setAuthLoading] = useState(true)

  // Restaura la sesión desde localStorage al montar el proveedor
  useEffect(() => {
    const token = localStorage.getItem('token')
    const userRaw = localStorage.getItem('user')
    try {
      if (token && userRaw) {
        const decoded = jwtDecode(token)
        if (decoded.exp * 1000 > Date.now()) {
          apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
          setAuthData({ token, user: JSON.parse(userRaw) })
        } else {
          // Token expirado localmente; limpiar sin esperar al servidor
          localStorage.clear()
        }
      }
    } catch (e) {
      console.error('Token en localStorage inválido, limpiando sesión:', e)
      localStorage.clear()
    } finally {
      setAuthLoading(false)
    }
  }, [])

  /**
   * Almacena el token y los datos del usuario tras un login exitoso.
   * Configura el header por defecto de axios para peticiones subsecuentes.
   *
   * @param {{ token: string, user: object }} data Respuesta del endpoint de login.
   */
  const login = (data) => {
    if (!data?.token || !data?.user) {
      console.error('Intento de login con datos inválidos:', data)
      return
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${data.token}`
    setAuthData(data)
  }

  /**
   * Cierra la sesión: limpia localStorage, elimina el header de axios,
   * invalida la cookie de refresh token en el servidor y limpia el estado.
   */
  const logout = () => {
    logoutRemoto()
    localStorage.clear()
    delete apiClient.defaults.headers.common['Authorization']
    setAuthData(null)
  }

  // Registra la función logout en el interceptor de axios para que pueda
  // forzar el cierre de sesión cuando el refresh token también expira.
  useEffect(() => {
    registerLogout(logout)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const value = { authData, login, logout, isAuthLoading }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/**
 * Hook para acceder al contexto de autenticación desde cualquier componente.
 *
 * @returns {{ authData, login, logout, isAuthLoading }}
 */
export const useAuth = () => {
  return useContext(AuthContext)
}
