import apiClient from './api'
import { jwtDecode } from 'jwt-decode' // Necesitamos esto aquí para decodificar

export const loginAdmin = async (matricula, password) => {
  const response = await apiClient.post('/auth/login', {
    matricula,
    password,
  })
  const token = response.data.token

  // --- CAMBIO: Decodificar el token para obtener el rol y el subject (matrícula) ---
  const decodedToken = jwtDecode(token)
  const user = {
    role: decodedToken.roles.replace('ROLE_', ''),
    nombreCompleto: decodedToken.sub, // En este caso, 'sub' es la matrícula del admin
    // Puedes añadir más campos si los necesitas del token o de la respuesta.data
  }
  // --- FIN CAMBIO ---

  return { token, user } // Devolvemos el token Y el objeto user
}

export const identificarUsuario = async (matricula) => {
  const response = await apiClient.post('/auth/identificar', { matricula })
  const token = response.data.token // El backend devuelve el token

  // --- CAMBIO: Construir el objeto user a partir de la respuesta del backend ---
  const user = {
    nombreCompleto: response.data.nombreCompleto, // Ya viene de la respuesta del backend
    area: response.data.area, // Ya viene de la respuesta del backend
    role: 'USER', // Rol fijo para identificación de usuario
  }
  // --- FIN CAMBIO ---

  return { token, user } // Devolvemos el token Y el objeto user
}
