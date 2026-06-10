import { useState, useEffect, useCallback } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  CircularProgress,
  Typography,
  Box,
} from '@mui/material'
import { getJustificacionesSelect } from '../services/justificacionService'
import {
  justificarAsistencia,
  justificarMiAsistencia,
} from '../services/asistenciaService'
import { useNotification } from '../context/NotificationContext'

/**
 * Modal para justificar una incidencia de asistencia.
 *
 * @param {boolean} esEmpleado  Si es true, el empleado justifica su propia incidencia
 *                              y queda PENDIENTE de aprobación. Si es false (admin),
 *                              se aplica el atajo que aprueba directamente.
 */
function JustificarModal({ open, onClose, record, onSuccess, esEmpleado = false }) {
  const [motivos, setMotivos] = useState([])
  const [formData, setFormData] = useState({
    justificacionId: '',
    observacion: '',
  })
  const [loading, setLoading] = useState(false)
  const [fetchingMotivos, setFetchingMotivos] = useState(false)
  const { showNotification } = useNotification()

  // useCallback estabiliza la referencia para poder declararla como
  // dependencia del useEffect sin recrearla en cada render.
  const cargarMotivos = useCallback(async () => {
    setFetchingMotivos(true)
    try {
      const data = await getJustificacionesSelect()
      setMotivos(data)
    } catch {
      showNotification('Error al cargar catálogo de justificaciones', 'error')
    } finally {
      setFetchingMotivos(false)
    }
  }, [showNotification])

  useEffect(() => {
    if (open) {
      cargarMotivos()
      setFormData({ justificacionId: '', observacion: '' })
    }
  }, [open, cargarMotivos])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Validación de la regla: ¿El motivo requiere observación?
    // Se compara con Number() porque el value del Select llega como string
    // y los ids del catálogo son numéricos (antes el === fallaba siempre).
    const motivoSeleccionado = motivos.find(
      (m) => m.id === Number(formData.justificacionId),
    )
    if (
      motivoSeleccionado?.requiereObservacion &&
      !formData.observacion.trim()
    ) {
      showNotification(
        'Este motivo requiere una observación obligatoria',
        'warning',
      )
      return
    }

    setLoading(true)
    try {
      const payload = {
        justificacionId: formData.justificacionId,
        observacion: formData.observacion,
      }

      // El empleado deja la justificación PENDIENTE; el admin la aplica/aprueba directo.
      if (esEmpleado) {
        await justificarMiAsistencia(record.idAsistencia, payload)
        showNotification(
          'Justificación enviada. Queda pendiente de aprobación',
          'success',
        )
      } else {
        await justificarAsistencia(record.idAsistencia, payload)
        showNotification('Justificación aplicada correctamente', 'success')
      }

      onSuccess() // Recarga la tabla del dashboard
      onClose()
    } catch (error) {
      showNotification(
        error.response?.data?.message || 'Error al aplicar justificación',
        'error',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Justificar Inasistencia / Retardo</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          {record && (
            <Box sx={{ mb: 3, p: 2, bgcolor: '#f5f5f5', borderRadius: 1 }}>
              <Typography variant="subtitle2" color="textSecondary">
                Empleado:
              </Typography>
              <Typography variant="body1" fontWeight="bold">
                {record.usuarioNumeroControl} - {record.usuarioNombreCompleto}
              </Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Fecha: {record.fecha} | Estatus actual:{' '}
                {record.estatusIncidencia === 1
                  ? 'Retardo'
                  : record.estatusIncidencia === 2
                    ? 'Falta Total'
                    : 'Omisión'}
              </Typography>
            </Box>
          )}

          {fetchingMotivos ? (
            <CircularProgress size={24} />
          ) : (
            <TextField
              select
              fullWidth
              label="Motivo de Justificación"
              name="justificacionId"
              value={formData.justificacionId}
              onChange={handleChange}
              required
              margin="normal">
              <MenuItem value="" disabled>
                Seleccione un motivo
              </MenuItem>
              {motivos.map((motivo) => (
                <MenuItem key={motivo.id} value={motivo.id}>
                  {motivo.nombre}
                </MenuItem>
              ))}
            </TextField>
          )}

          <TextField
            fullWidth
            label="Observaciones (Opcional/Obligatorio según motivo)"
            name="observacion"
            value={formData.observacion}
            onChange={handleChange}
            multiline
            rows={3}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} color="inherit" disabled={loading}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={loading || !formData.justificacionId}>
            {loading ? <CircularProgress size={24} /> : 'Aplicar'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default JustificarModal
