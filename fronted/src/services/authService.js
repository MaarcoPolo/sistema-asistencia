import apiClient from './api'
import { jwtDecode } from 'jwt-decode'

export const loginAdmin = async (matricula, password) => {
  const response = await apiClient.post('/auth/login', {
    matricula,
    password,
  })
  const token = response.data.token

  const decodedToken = jwtDecode(token)
  const user = {
    role: decodedToken.roles.replace('ROLE_', ''),
    nombreCompleto: decodedToken.sub,
  }

  return { token, user } 
}

export const identificarUsuario = async (matricula) => {
  const response = await apiClient.post('/auth/identificar', { matricula })
  const token = response.data.token 

  const user = {
    nombreCompleto: response.data.nombreCompleto, 
    area: response.data.area, 
    role: 'USER', 
  }

  return { token, user }
}
