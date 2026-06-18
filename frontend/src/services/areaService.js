import apiClient from './api'
import { descargarArchivo } from './downloadHelper'

// Obtener la lista de áreas para un selector (esta ya la teníamos)
export const getAreasForSelect = () => {
  return apiClient.get('/core/area/select-list')
}

// Exportar todas las áreas activas a Excel
export const exportarAreasExcel = () => {
  return descargarArchivo('/core/area/exportar/excel', {}, 'areas.xlsx')
}

// Obtener la lista paginada de áreas
export const getAreas = (params) => {
  return apiClient.get('/core/area', { params })
}

// Crear una nueva área
export const createArea = (areaData) => {
  return apiClient.post('/core/area', areaData)
}

// Actualizar un área existente
export const updateArea = (id, areaData) => {
  return apiClient.put(`/core/area/${id}`, areaData)
}

// Eliminar (soft delete) un área
export const deleteArea = (id) => {
  return apiClient.delete(`/core/area/${id}`)
}