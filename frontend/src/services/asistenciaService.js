import apiClient from './api'

export const getReporteAsistencias = (params) => {
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

export const registrarEntrada = async (fotoBase64) => {
  // Enviamos directamente un objeto que coincida con tu Record de Java
  const response = await apiClient.post('/asistencia/registrar-entrada', {
    fotoBase64: fotoBase64,
  })
  return response.data
}

export const registrarSalida = async (fotoBase64) => {
  const response = await apiClient.post('/asistencia/registrar-salida', {
    fotoBase64: fotoBase64,
  })
  return response.data
}
export const getEstadoAsistenciaDiario = async () => {
  const response = await apiClient.get('/asistencia/estado-diario')
  // El interceptor de api.js ya desenvuelve el ApiResponse: response.data es el payload.
  return response.data
}
// Función para exportar a EXCEL
export const exportarAsistenciasExcel = async (params) => {
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== ''),
  )

  try {
    const response = await apiClient.get('/asistencia/exportar/excel', {
      params: cleanParams,
      responseType: 'blob',
    })

    // Lógica para descargar el archivo en el navegador
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    const contentDisposition = response.headers['content-disposition']
    let fileName = 'reporte-asistencias.xlsx'
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
    throw error
  }
}

// Función para exportar a PDF
export const exportarAsistenciasPdf = async (params) => {
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== ''),
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

export const subirExcelMasivo = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/asistencia/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  // El interceptor ya desenvolvió el ApiResponse.
  return response.data
}

export const getResumenRetardos = async (fechaInicio, fechaFin) => {
  const response = await apiClient.get('/asistencia/resumen-retardos', {
    params: { fechaInicio, fechaFin },
  })
  return response.data
}

export const justificarAsistencia = async (id, data) => {
  const response = await apiClient.post(`/asistencia/${id}/justificar`, data)
  return response.data
}

// Agrega esto al final de asistenciaService.js

export const getResumenSanciones = async (params) => {
  const response = await apiClient.get('/asistencia/resumen-sanciones', {
    params,
  })
  // El interceptor ya desenvolvió el ApiResponse.
  return response.data
}

export const exportarSancionesPdf = async (params) => {
  const response = await apiClient.get('/asistencia/exportar/sanciones/pdf', {
    params,
    responseType: 'blob', // Importantísimo para recibir archivos
  })

  // Lógica para forzar la descarga del archivo en el navegador
  const url = window.URL.createObjectURL(
    new Blob([response.data], { type: 'application/pdf' }),
  )
  const link = document.createElement('a')
  link.href = url
  link.setAttribute(
    'download',
    `Reporte_Sanciones_${params.fechaInicio || 'General'}.pdf`,
  )
  document.body.appendChild(link)
  link.click()
  link.remove()
}

export const exportarSancionesExcel = async (params) => {
  const response = await apiClient.get('/asistencia/exportar/sanciones/excel', {
    params,
    responseType: 'blob',
  })

  const url = window.URL.createObjectURL(
    new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
  )
  const link = document.createElement('a')
  link.href = url
  link.setAttribute(
    'download',
    `Sanciones_Nómina_${params.fechaInicio || 'General'}.xlsx`,
  )
  document.body.appendChild(link)
  link.click()
  link.remove()
}

export const getMisAsistencias = (params) => {
  return apiClient.get('/asistencia/mis-asistencias', { params })
}

export const justificarMiAsistencia = async (id, data) => {
  const response = await apiClient.post(`/asistencia/${id}/mi-justificacion`, data)
  return response.data
}

// Aprobar una justificación pendiente
export const aprobarJustificacion = async (id) => {
  const response = await apiClient.post(`/asistencia/${id}/justificacion/aprobar`)
  return response.data
}

// Rechazar una justificación pendiente
export const rechazarJustificacion = async (id) => {
  const response = await apiClient.post(`/asistencia/${id}/justificacion/rechazar`)
  return response.data
}

// Obtiene las fotos (entrada/salida) de una asistencia bajo demanda.
// Las fotos ya no viajan en los listados para no transferir megabytes por página.
export const getFotosAsistencia = async (id) => {
  const response = await apiClient.get(`/asistencia/${id}/fotos`)
  return response.data
}