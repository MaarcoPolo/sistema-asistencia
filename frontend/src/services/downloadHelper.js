import apiClient from './api'

/**
 * Descarga un archivo (blob) desde un endpoint del backend y dispara la
 * descarga en el navegador. Centraliza la lógica que antes se repetía en cada
 * servicio de exportación (crear blob, leer el nombre del Content-Disposition,
 * simular el clic en un enlace y limpiar).
 *
 * @param {string} url           Ruta del endpoint (relativa a la baseURL de axios).
 * @param {object} params        Parámetros de query (los vacíos/null se descartan).
 * @param {string} fileNamePorDefecto  Nombre a usar si el servidor no envía uno.
 */
export const descargarArchivo = async (url, params = {}, fileNamePorDefecto = 'archivo') => {
  const cleanParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== undefined && v !== ''),
  )

  const response = await apiClient.get(url, {
    params: cleanParams,
    responseType: 'blob',
  })

  const blobUrl = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = blobUrl

  let fileName = fileNamePorDefecto
  const contentDisposition = response.headers['content-disposition']
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="(.+)"/)
    if (match && match.length === 2) fileName = match[1]
  }

  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(blobUrl)
}
