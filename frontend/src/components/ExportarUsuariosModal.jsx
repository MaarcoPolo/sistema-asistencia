import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  TextField,
  Autocomplete,
  RadioGroup,
  FormControlLabel,
  Radio,
  FormControl,
  FormLabel,
} from '@mui/material'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import { exportarUsuariosExcel } from '../services/usuarioService'
import { useNotification } from '../context/NotificationContext'

/**
 * Modal para exportar usuarios a Excel con tres modos:
 *  - "todos": todos los usuarios activos visibles.
 *  - "area": usuarios activos de un área específica.
 *  - "usuario": un usuario por su número de control.
 *
 * La exportación reutiliza el endpoint filtrable del backend.
 *
 * @param {boolean}  open      Si el modal está visible.
 * @param {function} onClose   Cierra el modal.
 * @param {Array}    areas     Lista de áreas para el selector (ya cargada por el padre).
 */
function ExportarUsuariosModal({ open, onClose, areas = [] }) {
  const [modo, setModo] = useState('todos')
  const [areaSeleccionada, setAreaSeleccionada] = useState(null)
  const [numeroControl, setNumeroControl] = useState('')
  const [exportando, setExportando] = useState(false)
  const { showNotification } = useNotification()

  const handleClose = () => {
    if (exportando) return
    onClose()
  }

  const handleExportar = async () => {
    // Validaciones según el modo elegido
    if (modo === 'area' && !areaSeleccionada) {
      showNotification('Selecciona un área para exportar', 'warning')
      return
    }
    if (modo === 'usuario' && !numeroControl.trim()) {
      showNotification('Ingresa el número de control del usuario', 'warning')
      return
    }

    const params = {}
    if (modo === 'area') params.areaId = areaSeleccionada.id
    if (modo === 'usuario') params.numeroControl = numeroControl.trim()

    setExportando(true)
    try {
      await exportarUsuariosExcel(params)
      onClose()
    } catch (error) {
      console.error('Error al exportar usuarios:', error)
      showNotification('No se pudo exportar el archivo', 'error')
    } finally {
      setExportando(false)
    }
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Exportar Usuarios a Excel</DialogTitle>
      <DialogContent>
        <FormControl sx={{ mt: 1 }}>
          <FormLabel>¿Qué información deseas exportar?</FormLabel>
          <RadioGroup
            value={modo}
            onChange={(e) => setModo(e.target.value)}>
            <FormControlLabel
              value="todos"
              control={<Radio />}
              label="Todos los usuarios activos"
            />
            <FormControlLabel
              value="area"
              control={<Radio />}
              label="Usuarios de un área específica"
            />
            <FormControlLabel
              value="usuario"
              control={<Radio />}
              label="Un usuario en específico"
            />
          </RadioGroup>
        </FormControl>

        {modo === 'area' && (
          <Box sx={{ mt: 1 }}>
            <Autocomplete
              options={areas}
              getOptionLabel={(option) => option.nombre}
              value={areaSeleccionada}
              onChange={(e, newValue) => setAreaSeleccionada(newValue)}
              renderInput={(params) => (
                <TextField {...params} label="Departamento / Área" autoFocus />
              )}
            />
          </Box>
        )}

        {modo === 'usuario' && (
          <Box sx={{ mt: 1 }}>
            <TextField
              fullWidth
              label="Número de control"
              value={numeroControl}
              onChange={(e) => setNumeroControl(e.target.value)}
              autoFocus
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={exportando}>
          Cancelar
        </Button>
        <Button
          variant="contained"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportar}
          disabled={exportando}>
          {exportando ? 'Exportando...' : 'Exportar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default ExportarUsuariosModal
