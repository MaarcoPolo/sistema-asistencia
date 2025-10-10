import apiClient from './api'

// Obtener la lista paginada de usuarios
export const getUsuarios = (params) => {
  return apiClient.get('/core/usuario', { params })
}

// Crear un nuevo usuario
export const createUsuario = (usuarioData) => {
  return apiClient.post('/core/usuario', usuarioData)
}

// Actualizar un usuario existente
export const updateUsuario = (id, usuarioData) => {
  return apiClient.put(`/core/usuario/${id}`, usuarioData)
}

// Eliminar un usuario
export const deleteUsuario = (id) => {
  return apiClient.delete(`/core/usuario/${id}`)
}
