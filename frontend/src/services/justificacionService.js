import apiClient from './api'
import { descargarArchivo } from './downloadHelper'

// Endpoints para el Catálogo (CRUD)
export const getJustificaciones = (params) => {
  return apiClient.get('/core/justificacion', { params })
}

// Exportar el catálogo de justificaciones a Excel
export const exportarJustificacionesExcel = () => {
  return descargarArchivo('/core/justificacion/exportar/excel', {}, 'justificaciones.xlsx')
}

export const getJustificacionesSelect = async () => {
  const response = await apiClient.get('/core/justificacion/select-list')
  return response.data
}

export const createJustificacion = (data) => {
  return apiClient.post('/core/justificacion', data)
}

export const updateJustificacion = (id, data) => {
  return apiClient.put(`/core/justificacion/${id}`, data)
}

export const deleteJustificacion = (id) => {
  return apiClient.delete(`/core/justificacion/${id}`)
}
