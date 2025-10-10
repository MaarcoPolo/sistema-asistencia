import apiClient from './api'

export const getHorarios = (params) => {
  // El backend para esto aún no está hecho, pero lo preparamos
  return apiClient.get('/core/horario', { params })
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
