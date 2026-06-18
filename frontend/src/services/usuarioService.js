import apiClient from './api'
import { descargarArchivo } from './downloadHelper'

// Obtener la lista paginada de usuarios
export const getUsuarios = (params) => {
  return apiClient.get('/core/usuario', { params })
}

// Exportar usuarios activos a Excel, aplicando filtros opcionales
// (numeroControl y/o areaId). Sin filtros exporta todos los activos visibles.
export const exportarUsuariosExcel = (params = {}) => {
  return descargarArchivo('/core/usuario/exportar/excel', params, 'usuarios.xlsx')
}

// Carga masiva de usuarios desde un archivo Excel. Devuelve el resumen
// { procesados, errores, detalleErrores }.
export const subirUsuariosMasivo = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/core/usuario/carga-masiva', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  // El interceptor ya desenvolvió el ApiResponse, así que .data es el resumen.
  return response.data
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

export const getMiPerfil = async () => {
  const response = await apiClient.get('/core/usuario/mi-perfil')
  // El interceptor ya desenvolvió el ApiResponse.
  return response.data
}

export const resetPasswordUsuario = async (id) => {
  const response = await apiClient.post(`/core/usuario/${id}/reset-password`)
  return response.data
}

export const cambiarMiContrasena = async (nuevaContrasena, contrasenaActual) => {
  const response = await apiClient.post(`/core/usuario/mi-contrasena`, {
    nuevaContrasena,
    contrasenaActual,
  })
  return response.data
}