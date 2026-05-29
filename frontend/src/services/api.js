import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Esta función se ejecutará ANTES de cada petición que se envíe.
apiClient.interceptors.request.use(
  (config) => {
    // Obtenemos el token desde el localStorage en el momento de la petición
    const token = localStorage.getItem('token')
    if (token) {
      // Si existe, lo añadimos a la cabecera de autorización
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config // Devolvemos la configuración para que la petición continúe
  },
  (error) => {
    // Manejamos errores en la configuración de la petición
    return Promise.reject(error)
  }
)

export default apiClient
