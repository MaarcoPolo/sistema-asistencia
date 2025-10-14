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
} from '@mui/material'
import { identificarUsuario } from '../services/authService'
import { useNotification } from '../context/NotificationContext'
import { getEstadoAsistenciaDiario } from '../services/asistenciaService'


function IdentificacionUsuario() {
  const [matricula, setMatricula] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login, logout } = useAuth()
  const { showNotification } = useNotification()

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      const data = await identificarUsuario(matricula)
      login(data)
      const estado = await getEstadoAsistenciaDiario()

      if (estado.entradaRegistrada && estado.salidaRegistrada) {
        // Si ya completó su jornada, no se le permite el acceso
        showNotification(
          'Ya has registrado tu entrada y salida el día de hoy.',
          'warning'
        )
        logout() // Se cierra la sesión temporal inmediatamente
      } else {
        // Si no ha completado su jornada, se le permite el acceso
        showNotification('Identificación exitosa. Redirigiendo...', 'success')
        navigate('/asistencia')
      }
    } catch (err) {
      console.error('Error en el proceso de identificación:', err)
      const errorMessage =
        err.response?.data?.message ||
        'Error al identificar. Verifique su matrícula.'
      showNotification(errorMessage, 'error')
      logout() // Limpiar cualquier sesión parcial si hubo un error
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container component="main" maxWidth="sm">
      <Paper
        elevation={6}
        sx={{
          mt: 8,
          p: 4,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
        }}>
        <Typography component="h1" variant="h4" sx={{ mb: 2 }}>
          Registro de Asistencia
        </Typography>
        <Typography>Por favor, ingrese su matrícula para continuar.</Typography>
        <Box
          component="form"
          onSubmit={handleSubmit}
          sx={{ mt: 1, width: '100%' }}>
          <TextField
            margin="normal"
            required
            fullWidth
            id="matricula"
            label="Matrícula"
            name="matricula"
            autoFocus
            value={matricula}
            onChange={(e) => setMatricula(e.target.value)}
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
