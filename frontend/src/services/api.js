import axios from 'axios'

/**
 * Instancia base de axios para todas las llamadas al backend.
 * La URL base se lee de la variable de entorno VITE_API_URL para que
 * funcione tanto en desarrollo (localhost) como en producción.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  // Necesario para que el navegador envíe la cookie HttpOnly de refresh
  // en los requests cross-origin hacia /api/auth/refresh.
  withCredentials: true,
})

apiClient.interceptors.response.use(
  (response) => {
    // Si la respuesta es un ApiResponse (trae .data), devolvemos el contenido real.
    // Si no, devolvemos la respuesta tal cual (ej. para archivos blob).
    if (
      response.data &&
      Object.prototype.hasOwnProperty.call(response.data, 'data')
    ) {
      return { ...response, data: response.data.data }
    }
    return response
  },
  (error) => Promise.reject(error)
)
/**
 * Función de logout registrada desde AuthContext.
 * Se usa dentro del interceptor de respuesta para forzar el cierre de sesión
 * cuando el refresh token también ha expirado.
 */
let _logoutFn = null

/**
 * Registra la función de logout del AuthContext para que el interceptor
 * pueda forzar el cierre de sesión sin depender de hooks de React.
 *
 * @param {Function} fn Función logout del AuthContext.
 */
export const registerLogout = (fn) => {
  _logoutFn = fn
}

// ── Interceptor de REQUEST ──────────────────────────────────────────────────
// localStorage es la ÚNICA fuente de verdad del token: se lee en cada petición,
// justo antes de enviarla. Así nunca importa el orden de los re-renders de React
// ni el estado de apiClient.defaults.headers, que podían quedar desincronizados y
// provocar peticiones sin Authorization (403) o con un token viejo.
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      // Adjunta el token vigente en cada request.
      config.headers['Authorization'] = `Bearer ${token}`
    } else {
      // Sin token: nos aseguramos de NO enviar un Authorization heredado/obsoleto.
      delete config.headers['Authorization']
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ── Interceptor de RESPONSE ─────────────────────────────────────────────────
// Maneja automáticamente los tokens expirados (401) y accesos denegados (403).
// Bandera para evitar bucles infinitos si el propio refresh devuelve 401.
let isRefreshing = false
// Cola de peticiones que esperan el nuevo token tras un refresh exitoso.
let pendingRequests = []

/**
 * Encola una petición fallida para reintentarla después del refresh.
 * Resuelve o rechaza la promesa cuando el refresh termina.
 */
const enqueueRequest = (resolve, reject) => {
  pendingRequests.push({ resolve, reject })
}

/** Reintenta todas las peticiones encoladas con el nuevo access token. */
const retryPendingRequests = (newToken) => {
  pendingRequests.forEach(({ resolve }) => resolve(newToken))
  pendingRequests = []
}

/** Rechaza todas las peticiones encoladas (el refresh también falló). */
const rejectPendingRequests = (error) => {
  pendingRequests.forEach(({ reject }) => reject(error))
  pendingRequests = []
}

apiClient.interceptors.response.use(
  // Respuestas exitosas pasan sin modificación
  (response) => response,

  async (error) => {
    const originalRequest = error.config

    // 403: acceso denegado definitivo, no hay refresh que ayude
    if (error.response?.status === 403) {
      if (_logoutFn) _logoutFn()
      return Promise.reject(error)
    }

    // 401: access token expirado — intentar renovarlo con la cookie de refresh
    if (error.response?.status === 401 && !originalRequest._retried) {
      // Marcar para no reintentar esta misma petición dos veces
      originalRequest._retried = true

      if (isRefreshing) {
        // Ya hay un refresh en curso: encolar esta petición y esperar
        return new Promise((resolve, reject) => {
          enqueueRequest(resolve, reject)
        }).then((newToken) => {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
          return apiClient(originalRequest)
        })
      }

      isRefreshing = true

      try {
        // La cookie HttpOnly se envía automáticamente gracias a withCredentials
        const refreshResponse = await apiClient.post('/auth/refresh')
        const newToken = refreshResponse.data.token

        // localStorage es la fuente de verdad; el interceptor de request leerá
        // este nuevo token automáticamente en los reintentos.
        localStorage.setItem('token', newToken)

        retryPendingRequests(newToken)

        originalRequest.headers['Authorization'] = `Bearer ${newToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        // El refresh también falló: sesión expirada, forzar logout
        rejectPendingRequests(refreshError)
        localStorage.clear()
        if (_logoutFn) _logoutFn()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  },
)

export default apiClient
