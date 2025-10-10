import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import {
  Button,
  TextField,
  Box,
  Typography,
  Container,
  Paper,
} from '@mui/material'
import { loginAdmin } from '../services/authService.js'
import { useNotification } from '../context/NotificationContext'

function LoginAdmin() {
  const [matricula, setMatricula] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login } = useAuth()
  const { showNotification } = useNotification()

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      setLoading(true)
      setError('')
      const data = await loginAdmin(matricula, password)
      login(data)
      navigate('/admin/dashboard')
    } catch (err) {
      const message = err.response?.data?.message || 'Credenciales inválidas.'
      setError(message)
      showNotification(message, 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container component="main" maxWidth="xs">
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
        <Typography component="h1" variant="h5">
          Acceso de Administrador
        </Typography>
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
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
          <TextField
            margin="normal"
            required
            fullWidth
            name="password"
            label="Contraseña"
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && (
            <Typography color="error" variant="body2">
              {error}
            </Typography>
          )}
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
            disabled={loading}>
            {loading ? 'Ingresando...' : 'Ingresar'}
          </Button>
        </Box>
      </Paper>
    </Container>
  )
}
export default LoginAdmin
