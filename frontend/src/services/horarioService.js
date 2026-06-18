import apiClient from './api'
import { descargarArchivo } from './downloadHelper'

export const getHorarios = (params) => {
  return apiClient.get('/core/horario', { params })
}

// Exportar todos los horarios a Excel
export const exportarHorariosExcel = () => {
  return descargarArchivo('/core/horario/exportar/excel', {}, 'horarios.xlsx')
}

export const createHorario = (horarioData) => {
  return apiClient.post('/core/horario', horarioData)
}

export const updateHorario = (id, horarioData) => {
  return apiClient.put(`/core/horario/${id}`, horarioData)
}

export const deleteHorario = (id) => {
  return apiClient.delete(`/core/horario/${id}`)
}
