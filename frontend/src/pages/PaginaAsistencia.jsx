import React, { useState, useRef, useEffect, useCallback } from 'react'
import { Box, Button, Typography, Paper, Container } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import Webcam from 'react-webcam'
import {
  registrarEntrada,
  registrarSalida,
  getEstadoAsistenciaDiario,
} from '../services/asistenciaService'
import { useAuth } from '../context/AuthContext'
import { useNotification } from '../context/NotificationContext'
import Clock from '../components/Clock'

function PaginaAsistencia() {
  const { authData, logout } = useAuth()
  const { showNotification } = useNotification()
  const navigate = useNavigate()
  const webcamRef = useRef(null)
  const [capturing, setCapturing] = useState(false)

  const [nombre, setNombre] = useState('')
  const [area, setArea] = useState('')

  const [entradaRegistrada, setEntradaRegistrada] = useState(false)
  const [salidaRegistrada, setSalidaRegistrada] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const loadEstadoAsistencia = useCallback(async () => {
    if (!authData || !authData.token) return
    setIsLoading(true)
    try {
      const estado = await getEstadoAsistenciaDiario()
      setEntradaRegistrada(estado.entradaRegistrada)
      setSalidaRegistrada(estado.salidaRegistrada)
    } catch (error) {
      console.error('Error al cargar el estado de asistencia:', error)
      showNotification('Error al cargar el estado de asistencia.', 'error')
    } finally {
      setIsLoading(false)
    }
  }, [authData, showNotification])

  useEffect(() => {
    if (authData && authData.user) {
      setNombre(authData.user.nombreCompleto)
      setArea(authData.user.area)
      loadEstadoAsistencia()
    }
  }, [authData, loadEstadoAsistencia])

  const capture = useCallback(() => {
    const imageSrc = webcamRef.current.getScreenshot()
    return imageSrc
  }, [webcamRef])

  const handleRegistro = async (tipo) => {
    if (!authData || !authData.token) {
      showNotification(
        'No estás autenticado. Por favor, identifícate de nuevo.',
        'error',
      )
      return
    }

    setIsLoading(true)
    setCapturing(true)

    try {
      // 1. Obtenemos el string nativo de la cámara (ej. "data:image/jpeg;base64,/9j/4AAQSk...")
      const imageBase64 = capture()

      if (!imageBase64) {
        showNotification(
          'No se pudo capturar la imagen. Asegúrate de que la cámara esté activa.',
          'error',
        )
        return
      }

      // 2. Eliminamos toda la lógica de conversión a Blob.
      // Mandamos directamente el string al servicio.
      let response
      if (tipo === 'entrada') {
        response = await registrarEntrada(imageBase64)
        setEntradaRegistrada(true)
      } else {
        response = await registrarSalida(imageBase64)
        setSalidaRegistrada(true)
      }

      showNotification(
        response.message ||
          `${tipo === 'entrada' ? 'Entrada' : 'Salida'} registrada con éxito.`,
        'success',
      )
      setTimeout(() => {
        logout()
        navigate('/')
      }, 2000)
    } catch (error) {
      console.error(`Error al registrar ${tipo}:`, error)
      const errorMessage =
        error.response?.data?.message ||
        `Error al registrar ${tipo}. Intenta de nuevo.`
      showNotification(errorMessage, 'error')

      setTimeout(() => {
        logout()
        navigate('/')
      }, 3000)

      loadEstadoAsistencia()
    }
  }

  const handleLogout = () => {
    showNotification('Sesión cerrada.', 'info')
    logout()
    navigate('/')
  }

  const getStatusMessage = () => {
    if (isLoading) {
      return 'Cargando estado de asistencia...'
    }
    if (entradaRegistrada && salidaRegistrada) {
      return '¡Has registrado tu entrada y salida para hoy!'
    }
    if (entradaRegistrada) {
      return 'Ya registraste tu entrada. Ahora puedes registrar tu salida.'
    }
    return 'Por favor, registra tu entrada para comenzar el día.'
  }

  return (
    <Container component="main" maxWidth="sm">
      <Paper
        elevation={3}
        sx={{
          padding: 4,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          mt: 4,
        }}>
        <Typography component="h1" variant="h5" sx={{ mb: 2 }}>
          Panel de Asistencia
        </Typography>

        {authData && authData.user && (
          <Box sx={{ mb: 3, textAlign: 'center' }}>
            <Typography variant="h6">¡Hola, {nombre}!</Typography>
            <Typography variant="subtitle1">Área: {area}</Typography>
          </Box>
        )}

        <Clock />

        {/* Contenedor de cámara fluido: ocupa el ancho disponible con un máximo,
            conservando la proporción 4:3 para que se vea bien en cualquier pantalla. */}
        <Box
          sx={{
            width: '100%',
            maxWidth: 360,
            aspectRatio: '4 / 3',
            borderRadius: 2,
            overflow: 'hidden',
            border: '1px solid',
            borderColor: 'divider',
            mb: 3,
            backgroundColor: 'grey.100',
          }}>
          <Webcam
            audio={false}
            ref={webcamRef}
            screenshotFormat="image/jpeg"
            videoConstraints={{ facingMode: 'user' }}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              display: 'block',
            }}
          />
        </Box>

        <Box
          sx={{
            display: 'flex',
            flexDirection: { xs: 'column', sm: 'row' },
            gap: 2,
            mb: 2,
            width: '100%',
          }}>
          <Button
            fullWidth
            size="large"
            variant="contained"
            color="primary"
            onClick={() => handleRegistro('entrada')}
            disabled={isLoading || entradaRegistrada}>
            {capturing && !salidaRegistrada
              ? 'Capturando...'
              : entradaRegistrada
              ? 'Entrada Registrada'
              : 'Registrar Entrada'}
          </Button>
          <Button
            fullWidth
            size="large"
            variant="contained"
            color="secondary"
            onClick={() => handleRegistro('salida')}
            disabled={isLoading || !entradaRegistrada || salidaRegistrada}>
            {capturing && entradaRegistrada
              ? 'Capturando...'
              : salidaRegistrada
              ? 'Salida Registrada'
              : 'Registrar Salida'}
          </Button>
        </Box>
        <Button
          fullWidth
          variant="outlined"
          color="error"
          onClick={handleLogout}
          disabled={isLoading}
          sx={{ maxWidth: { sm: 200 } }}>
          Salir
        </Button>

        <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
          {getStatusMessage()}
        </Typography>
      </Paper>
    </Container>
  )
}

export default PaginaAsistencia
