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
  Avatar,
  Divider,
  Link,
} from '@mui/material'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import { loginAdmin, restablecerContrasena } from '../services/authService.js'
import { useNotification } from '../context/NotificationContext'

function LoginAdmin() {
  const [numeroControl, setNumeroControl] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Estado del flujo "olvidé mi contraseña": se identifica reescribiendo el
  // número de control para tener certeza de a quién se le restablece.
  const [mostrarReset, setMostrarReset] = useState(false)
  const [resetNumeroControl, setResetNumeroControl] = useState('')
  const [resetLoading, setResetLoading] = useState(false)

  const navigate = useNavigate()
  const { login } = useAuth()
  const { showNotification } = useNotification()

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      setLoading(true)
      setError('')
      const data = await loginAdmin(numeroControl, password)
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

  const handleReset = async (event) => {
    event.preventDefault()
    try {
      setResetLoading(true)
      await restablecerContrasena(resetNumeroControl)
      showNotification('Su contraseña se ha restablecido a la contraseña inicial', 'success')
      setMostrarReset(false)
      setResetNumeroControl('')
    } catch (err) {
      const message =
        err.response?.data?.message || 'Número de control incorrecto, favor de verificar'
      showNotification(message, 'error')
    } finally {
      setResetLoading(false)
    }
  }

  return (
    <Container component="main" maxWidth="xs">
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
          <LockOutlinedIcon />
        </Avatar>
        <Typography component="h1" variant="h5">
          Acceso
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Ingresa tus credenciales para continuar
        </Typography>
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2, width: '100%' }}>
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

        {/* ── Olvidé mi contraseña: restablece a la inicial del sistema ── */}
        {!mostrarReset ? (
          <Link
            component="button"
            type="button"
            variant="body2"
            underline="hover"
            onClick={() => {
              setMostrarReset(true)
              setResetNumeroControl('')
            }}>
            ¿Olvidó su contraseña? Restablecer
          </Link>
        ) : (
          <Box component="form" onSubmit={handleReset} sx={{ width: '100%' }}>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Escriba su número de control para restablecer su contraseña a la inicial.
            </Typography>
            <TextField
              margin="normal"
              required
              fullWidth
              id="resetNumeroControl"
              label="Número de Control"
              name="resetNumeroControl"
              value={resetNumeroControl}
              onChange={(e) => setResetNumeroControl(e.target.value)}
            />
            <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={() => {
                  setMostrarReset(false)
                  setResetNumeroControl('')
                }}
                disabled={resetLoading}>
                Cancelar
              </Button>
              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={resetLoading}>
                {resetLoading ? 'Restableciendo...' : 'Restablecer contraseña'}
              </Button>
            </Box>
          </Box>
        )}
      </Paper>
    </Container>
  )
}
export default LoginAdmin
