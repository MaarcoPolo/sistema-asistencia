import apiClient from './api'

/**
 * Obtiene el reporte de asistencias paginado y con filtros.
 * @param {object} params - Objeto con los filtros: fechaInicio, fechaFin, usuarioId, areaId, page, size.
 * @returns {Promise<object>} La respuesta paginada del backend.
 */
export const getReporteAsistencias = (params) => {
  // Devuelve la promesa completa, igual que getUsuarios, getAreas, etc.
  return apiClient.get('/asistencia/reporte', { params })
}

export const createAsistencia = (data) => {
  return apiClient.post('/asistencia/manual', data)
}

export const updateAsistencia = (id, data) => {
  return apiClient.put(`/asistencia/manual/${id}`, data)
}

export const deleteAsistencia = (id) => {
  return apiClient.delete(`/asistencia/${id}`)
}

export const registrarEntrada = async (foto) => {
  const formData = new FormData();
  formData.append('file', foto);
  const response = await apiClient.post('/asistencia/registrar-entrada', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const registrarSalida = async (foto) => {
  const formData = new FormData()
  formData.append('file', foto)
  const response = await apiClient.post(
    '/asistencia/registrar-salida',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  )
  return response.data
}
export const getEstadoAsistenciaDiario = async () => {
  const response = await apiClient.get('/asistencia/estado-diario')
  return response.data
}