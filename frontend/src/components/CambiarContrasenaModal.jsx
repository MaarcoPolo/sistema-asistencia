import { useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, CircularProgress } from '@mui/material'
import { cambiarMiContrasena } from '../services/usuarioService'
import { useNotification } from '../context/NotificationContext'

function CambiarContrasenaModal({ open, onClose, onSuccess, esPrimerAcceso = false }) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { showNotification } = useNotification()

  const handleSubmit = async (e) => {
    e.preventDefault()

    // En primer acceso el backend no exige la contraseña actual; en cualquier
    // otro caso es obligatoria para evitar el secuestro de cuentas.
    if (!esPrimerAcceso && !currentPassword) {
      showNotification('Debes ingresar tu contraseña actual', 'warning')
      return
    }
    if (password !== confirmPassword) {
      showNotification('Las contraseñas no coinciden', 'warning')
      return
    }
    if (password.length < 8) {
      showNotification('La contraseña debe tener al menos 8 caracteres', 'warning')
      return
    }

    setLoading(true)
    try {
      await cambiarMiContrasena(password, currentPassword)
      showNotification('Contraseña actualizada correctamente', 'success')
      setCurrentPassword('')
      setPassword('')
      setConfirmPassword('')
      onSuccess() // Recarga el perfil para ocultar la alerta roja
      onClose()
    } catch (error) {
      showNotification(error.response?.data?.message || 'Error al actualizar contraseña', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Cambiar Mi Contraseña</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          {!esPrimerAcceso && (
            <TextField
              fullWidth
              type="password"
              label="Contraseña Actual"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
              margin="normal"
              autoComplete="current-password"
            />
          )}
          <TextField
            fullWidth
            type="password"
            label="Nueva Contraseña"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            margin="normal"
            autoComplete="new-password"
          />
          <TextField
            fullWidth
            type="password"
            label="Confirmar Nueva Contraseña"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            margin="normal"
            autoComplete="new-password"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} color="inherit" disabled={loading}>Cancelar</Button>
          <Button type="submit" variant="contained" color="primary" disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Actualizar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default CambiarContrasenaModal