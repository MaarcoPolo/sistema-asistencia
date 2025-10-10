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
} from '@mui/material'
import { identificarUsuario } from '../services/authService'
import { useNotification } from '../context/NotificationContext'

function IdentificacionUsuario() {
  const [matricula, setMatricula] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const { login } = useAuth()
  const { showNotification } = useNotification()

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      setLoading(true)
      setError('')
      const data = await identificarUsuario(matricula)
      login(data)
      showNotification('Identificación exitosa. Redirigiendo...', 'success')
      navigate('/asistencia')
    } catch (err) {
      console.error('Error al identificar usuario:', err)
      setError(
        err.response?.data?.message ||
          'Error al identificar. Matrícula o token inválido.'
      )
      showNotification(
        err.response?.data?.message || 'Error de identificación',
        'error'
      )
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
          />
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}>
            Identificarse
          </Button>
        </Box>
      </Paper>
    </Container>
  )
}

export default IdentificacionUsuario
