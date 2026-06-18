import { useState, useRef } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Alert,
  List,
  ListItem,
  ListItemText,
  Divider,
  CircularProgress,
} from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import { subirUsuariosMasivo } from '../services/usuarioService'
import { useNotification } from '../context/NotificationContext'

/**
 * Modal de carga masiva de usuarios desde Excel. Permite seleccionar el archivo,
 * lo envía al backend y muestra el reporte fila por fila (cuántos se procesaron,
 * cuántos fallaron y el detalle de cada error con su número de fila).
 *
 * @param {boolean}  open       Visibilidad del modal.
 * @param {function} onClose    Cierra el modal.
 * @param {function} onSuccess  Se llama tras una carga con al menos un alta, para refrescar la tabla.
 */
function CargaMasivaUsuariosModal({ open, onClose, onSuccess }) {
  const [archivo, setArchivo] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const inputRef = useRef(null)
  const { showNotification } = useNotification()

  const reiniciar = () => {
    setArchivo(null)
    setResultado(null)
    setCargando(false)
    if (inputRef.current) inputRef.current.value = ''
  }

  const handleClose = () => {
    if (cargando) return
    reiniciar()
    onClose()
  }

  const handleSeleccion = (e) => {
    const file = e.target.files[0]
    setArchivo(file || null)
    setResultado(null) // limpiar reporte anterior al elegir otro archivo
  }

  const handleProcesar = async () => {
    if (!archivo) {
      showNotification('Selecciona un archivo Excel primero', 'warning')
      return
    }
    setCargando(true)
    setResultado(null)
    try {
      const data = await subirUsuariosMasivo(archivo)
      setResultado(data)
      if (data.procesados > 0) {
        showNotification(
          `Carga finalizada: ${data.procesados} registrados, ${data.errores} con error.`,
          data.errores > 0 ? 'warning' : 'success',
        )
        onSuccess?.()
      } else {
        showNotification(
          `No se registró ningún usuario. ${data.errores} fila(s) con error.`,
          'error',
        )
      }
    } catch (error) {
      console.error('Error en carga masiva de usuarios:', error)
      showNotification(
        error.response?.data?.message || 'No se pudo procesar el archivo',
        'error',
      )
    } finally {
      setCargando(false)
    }
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Carga Masiva de Usuarios</DialogTitle>
      <DialogContent>
        <Alert severity="info" sx={{ mb: 2 }}>
          El archivo debe tener las columnas: <b>numero_control</b>, <b>nombre</b>,{' '}
          <b>apellido_paterno</b>, <b>apellido_materno</b>, <b>area_id</b>, <b>rol</b>.
          Todos se registran como <b>activos</b> con rol <b>USER</b> y contraseña
          inicial automática (número de control + "-DIF").
        </Alert>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
          <Button
            variant="outlined"
            component="label"
            startIcon={<UploadFileIcon />}
            disabled={cargando}>
            Seleccionar archivo
            <input
              ref={inputRef}
              type="file"
              hidden
              accept=".xlsx, .xls"
              onChange={handleSeleccion}
            />
          </Button>
          <Typography variant="body2" color="text.secondary" noWrap>
            {archivo ? archivo.name : 'Ningún archivo seleccionado'}
          </Typography>
        </Box>

        {/* Reporte de resultados */}
        {resultado && (
          <Box sx={{ mt: 2 }}>
            <Box sx={{ display: 'flex', gap: 3, mb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <CheckCircleIcon color="success" fontSize="small" />
                <Typography variant="body2">
                  Registrados: <b>{resultado.procesados}</b>
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <ErrorIcon color="error" fontSize="small" />
                <Typography variant="body2">
                  Con error: <b>{resultado.errores}</b>
                </Typography>
              </Box>
            </Box>

            {resultado.detalleErrores && resultado.detalleErrores.length > 0 && (
              <>
                <Divider sx={{ my: 1 }} />
                <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                  Detalle de errores:
                </Typography>
                <List
                  dense
                  sx={{
                    maxHeight: 240,
                    overflow: 'auto',
                    bgcolor: 'action.hover',
                    borderRadius: 1,
                  }}>
                  {resultado.detalleErrores.map((err, idx) => (
                    <ListItem key={idx} divider>
                      <ListItemText
                        primary={err}
                        primaryTypographyProps={{ variant: 'body2', color: 'error' }}
                      />
                    </ListItem>
                  ))}
                </List>
              </>
            )}

            {resultado.errores === 0 && resultado.procesados > 0 && (
              <Alert severity="success" sx={{ mt: 1 }}>
                Todos los usuarios se registraron correctamente.
              </Alert>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={cargando}>
          {resultado ? 'Cerrar' : 'Cancelar'}
        </Button>
        <Button
          variant="contained"
          startIcon={cargando ? <CircularProgress size={18} color="inherit" /> : <UploadFileIcon />}
          onClick={handleProcesar}
          disabled={cargando || !archivo}>
          {cargando ? 'Procesando...' : 'Procesar'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default CargaMasivaUsuariosModal
