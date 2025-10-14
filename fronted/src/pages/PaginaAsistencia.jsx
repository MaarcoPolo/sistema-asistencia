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
        'error'
      )
      return
    }

    setIsLoading(true)
    setCapturing(true)

    try {
      const imageBase64 = capture()
      if (!imageBase64) {
        showNotification(
          'No se pudo capturar la imagen. Asegúrate de que la cámara esté activa.',
          'error'
        )
        return
      }

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
        setEntradaRegistrada(true)
      } else {
        response = await registrarSalida(blob)
        setSalidaRegistrada(true)
      }

      // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
      // Accedemos a response.message para obtener solo el texto.
      showNotification(
        response.message ||
          `${tipo === 'entrada' ? 'Entrada' : 'Salida'} registrada con éxito.`,
        'success'
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
      loadEstadoAsistencia()
      setIsLoading(false)
      setCapturing(false)
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

        <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
          <Button
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
          variant="outlined"
          color="error"
          onClick={handleLogout}
          disabled={isLoading}>
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
