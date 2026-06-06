import apiClient from './api'

/**
 * Autentica a un administrador con número de control y contraseña.
 * El backend retorna el access token en el body y emite el refresh token
 * como cookie HttpOnly (no accesible por JavaScript).
 *
 * @param {string} numeroControl Identificador del usuario.
 * @param {string} password Contraseña en texto plano (viaja por HTTPS).
 * @returns {{ token: string, user: object }} Token y datos del usuario.
 */
export const loginAdmin = async (numeroControl, password) => {
  const response = await apiClient.post('/auth/login', { numeroControl, password })
  // El backend ya construye el objeto user con todos los campos necesarios
  return { token: response.data.token, user: response.data.user }
}

/**
 * Identifica a un empleado en el kiosco solo con número de control.
 * No requiere contraseña; la seguridad depende de la restricción por IP del área.
 *
 * @param {string} numeroControl Número de control del empleado.
 * @returns {{ token: string, user: object }} Token de sesión temporal y datos básicos.
 */
export const identificarUsuario = async (numeroControl) => {
  const response = await apiClient.post('/auth/identificar', { numeroControl })
  const user = {
    nombreCompleto: response.data.nombreCompleto,
    area: response.data.area,
    role: 'USER',
  }
  return { token: response.data.token, user }
}

/**
 * Cierra la sesión en el servidor invalidando la cookie de refresh token.
 * El frontend debe limpiar localStorage de forma independiente.
 */
export const logoutRemoto = async () => {
  try {
    await apiClient.post('/auth/logout')
  } catch {
    // Si el servidor falla, el logout local sigue siendo válido
  }
}
