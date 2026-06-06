import { useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField, CircularProgress } from '@mui/material'
import { cambiarMiContrasena } from '../services/usuarioService'
import { useNotification } from '../context/NotificationContext'

function CambiarContrasenaModal({ open, onClose, onSuccess }) {
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { showNotification } = useNotification()

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (password !== confirmPassword) {
      showNotification('Las contraseñas no coinciden', 'warning')
      return
    }
    if (password.length < 6) {
      showNotification('La contraseña debe tener al menos 6 caracteres', 'warning')
      return
    }

    setLoading(true)
    try {
      await cambiarMiContrasena(password)
      showNotification('Contraseña actualizada correctamente', 'success')
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
          <TextField
            fullWidth
            type="password"
            label="Nueva Contraseña"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            margin="normal"
          />
          <TextField
            fullWidth
            type="password"
            label="Confirmar Nueva Contraseña"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            margin="normal"
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