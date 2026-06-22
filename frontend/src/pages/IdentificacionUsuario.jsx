import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  Button,
  TextField,
  Box,
  Typography,
  Container,
  Paper,
  CircularProgress,
  Avatar,
} from '@mui/material'
import HowToRegIcon from '@mui/icons-material/HowToReg'
import { identificarUsuario } from '../services/authService'
import { useNotification } from '../context/NotificationContext'
import { getEstadoAsistenciaDiario } from '../services/asistenciaService'

function IdentificacionUsuario() {
  const [numeroControl, setNumeroControl] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login, logout } = useAuth()
  const { showNotification } = useNotification()

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      setLoading(true)
      const data = await identificarUsuario(numeroControl)
      login(data)
      const estado = await getEstadoAsistenciaDiario()

      if (estado.entradaRegistrada && estado.salidaRegistrada) {
        showNotification(
          'Ya has registrado tu entrada y salida el día de hoy.',
          'warning',
        )
        logout()
      } else {
        showNotification('Identificación exitosa. Redirigiendo...', 'success')
        navigate('/asistencia')
      }
    } catch (err) {
      console.error('Error en el proceso de identificación:', err)
      const errorMessage =
        err.response?.data?.message ||
        'Error al identificar. Verifique su número de control.'
      showNotification(errorMessage, 'error')
      logout()
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container component="main" maxWidth="sm">
      <Paper
        elevation={6}
        sx={{
          mt: { xs: 6, sm: 10 },
          p: { xs: 3, sm: 4 },
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          backgroundColor: 'rgba(255, 255, 255, 0.96)',
          backdropFilter: 'blur(4px)',
        }}>
        <Avatar sx={{ bgcolor: 'primary.main', mb: 1.5, width: 56, height: 56 }}>
          <HowToRegIcon />
        </Avatar>
        <Typography component="h1" variant="h4" sx={{ mb: 1, textAlign: 'center' }}>
          Registro de Asistencia
        </Typography>
        <Typography color="text.secondary" sx={{ textAlign: 'center' }}>
          Por favor, ingrese su número de control para continuar.
        </Typography>
        <Box
          component="form"
          onSubmit={handleSubmit}
          sx={{ mt: 1, width: '100%' }}>
          <TextField
            margin="normal"
            required
            fullWidth
            id="numeroControl"
            label="Número de Control"
            name="numeroControl"
            autoFocus
            value={numeroControl}
            onChange={(e) => setNumeroControl(e.target.value)}
            disabled={loading}
          />
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
            disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Identificarse'}
          </Button>
        </Box>
      </Paper>
    </Container>
  )
}

export default IdentificacionUsuario
