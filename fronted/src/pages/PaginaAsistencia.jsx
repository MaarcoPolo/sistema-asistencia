import React, { useState, useRef, useEffect, useCallback } from 'react'
import { Box, Button, Typography, Paper, Container } from '@mui/material'
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
  const { authData } = useAuth()
  const showNotification = useNotification()
  const webcamRef = useRef(null)
  const [capturing, setCapturing] = useState(false)

  const [nombre, setNombre] = useState('')
  const [area, setArea] = useState('')

  const [entradaRegistrada, setEntradaRegistrada] = useState(false)
  const [salidaRegistrada, setSalidaRegistrada] = useState(false)
  const [isLoading, setIsLoading] = useState(false) // Para indicar que una operación está en curso

  // Efecto para cargar datos del usuario y el estado de asistencia inicial
  useEffect(() => {
    if (authData && authData.user) {
      setNombre(authData.user.nombreCompleto)
      setArea(authData.user.area)
      loadEstadoAsistencia() // Cargar el estado al iniciar la página
    }
  }, [authData]) // Dependencia authData

  // Función para cargar el estado de asistencia del día
  const loadEstadoAsistencia = useCallback(async () => {
    if (!authData || !authData.token) return // Asegúrate de tener token para la petición
    setIsLoading(true) // Indicar que se está cargando el estado
    try {
      const estado = await getEstadoAsistenciaDiario()
      setEntradaRegistrada(estado.entradaRegistrada)
      setSalidaRegistrada(estado.salidaRegistrada)
    } catch (error) {
      console.error('Error al cargar el estado de asistencia:', error)
      showNotification('Error al cargar el estado de asistencia.', 'error')
    } finally {
      setIsLoading(false) // Finalizar carga
    }
  }, [authData, showNotification])

  useEffect(() => {
    loadEstadoAsistencia() // Se ejecuta una vez al montar el componente
  }, [loadEstadoAsistencia])

  // Función para capturar la imagen de la webcam
  const capture = useCallback(() => {
    const imageSrc = webcamRef.current.getScreenshot()
    return imageSrc // Devuelve la imagen en base64
  }, [webcamRef])

  // Función para manejar el registro de entrada o salida
  const handleRegistro = async (tipo) => {
    if (!authData || !authData.token) {
      showNotification(
        'No estás autenticado. Por favor, identifícate de nuevo.',
        'error'
      )
      return
    }

    setIsLoading(true) // Bloquear botones
    setCapturing(true) // Indicar que se está capturando

    try {
      const imageBase64 = capture()
      if (!imageBase64) {
        showNotification(
          'No se pudo capturar la imagen. Asegúrate de que la cámara esté activa.',
          'error'
        )
        return
      }

      // Convertir base64 a Blob (para enviar como MultipartFile al backend)
      const byteString = atob(imageBase64.split(',')[1])
      const mimeString = imageBase64.split(',')[0].split(':')[1].split(';')[0]
      const ab = new ArrayBuffer(byteString.length)
      const ia = new Uint8Array(ab)
      for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i)
      }
      const blob = new Blob([ab], { type: mimeString })

      let response
      if (tipo === 'entrada') {
        response = await registrarEntrada(blob)
        setEntradaRegistrada(true) // Actualiza el estado después de una entrada exitosa
      } else {
        response = await registrarSalida(blob)
        setSalidaRegistrada(true) // Actualiza el estado después de una salida exitosa
      }
      showNotification(
        response ||
          `${tipo === 'entrada' ? 'Entrada' : 'Salida'} registrada con éxito.`,
        'success'
      )
    } catch (error) {
      console.error(`Error al registrar ${tipo}:`, error)
      const errorMessage =
        error.response?.data?.message ||
        `Error al registrar ${tipo}. Intenta de nuevo.`
      showNotification(errorMessage, 'error')
      // Vuelve a cargar el estado para asegurar la coherencia si hubo un error del backend
      loadEstadoAsistencia()
    } finally {
      setIsLoading(false) // Desbloquear botones
      setCapturing(false) // Finalizar captura
    }
  }

  // Mensaje a mostrar basado en el estado de asistencia
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

        <Webcam
          audio={false}
          ref={webcamRef}
          screenshotFormat="image/jpeg"
          width={320}
          height={240}
          videoConstraints={{
            facingMode: 'user',
          }}
          style={{
            borderRadius: '8px',
            border: '1px solid #ccc',
            marginBottom: '20px',
          }}
        />

        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button
            variant="contained"
            color="primary"
            onClick={() => handleRegistro('entrada')}
            disabled={isLoading || entradaRegistrada}
          >
            {capturing && !salidaRegistrada
              ? 'Capturando...'
              : entradaRegistrada
              ? 'Entrada Registrada'
              : 'Registrar Entrada'}
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => handleRegistro('salida')}
            disabled={isLoading || !entradaRegistrada || salidaRegistrada} 
          >
            {capturing && entradaRegistrada
              ? 'Capturando...'
              : salidaRegistrada
              ? 'Salida Registrada'
              : 'Registrar Salida'}
          </Button>
        </Box>

        {/* Mensaje de estado informativo */}
        <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
          {getStatusMessage()}
        </Typography>
      </Paper>
    </Container>
  )
}

export default PaginaAsistencia
