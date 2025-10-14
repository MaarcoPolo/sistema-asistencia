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
// Función para exportar a EXCEL
export const exportarAsistenciasExcel = async (params) => {
  // Limpiamos los filtros que no tengan valor
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== '')
  )

  try {
    const response = await apiClient.get('/asistencia/exportar/excel', {
      params: cleanParams,
      responseType: 'blob', // MUY IMPORTANTE: Le decimos a Axios que esperamos un archivo
    })

    // Lógica para descargar el archivo en el navegador
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    // El backend nos dará el nombre del archivo en las cabeceras
    const contentDisposition = response.headers['content-disposition']
    let fileName = 'reporte-asistencias.xlsx' // Nombre por defecto
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename="(.+)"/)
      if (fileNameMatch.length === 2) fileName = fileNameMatch[1]
    }
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Error al exportar a Excel:', error)
    // Lanzamos el error para que el componente que lo llama pueda manejarlo (ej. mostrar notificación)
    throw error
  }
}

// Función para exportar a PDF
export const exportarAsistenciasPdf = async (params) => {
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== '')
  )
  try {
    const response = await apiClient.get('/asistencia/exportar/pdf', {
      params: cleanParams,
      responseType: 'blob',
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    const contentDisposition = response.headers['content-disposition']
    let fileName = 'reporte-asistencias.pdf'
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename="(.+)"/)
      if (fileNameMatch.length === 2) fileName = fileNameMatch[1]
    }
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Error al exportar a PDF:', error)
    throw error
  }
}