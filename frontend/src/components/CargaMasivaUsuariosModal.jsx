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
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
} from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import ErrorIcon from '@mui/icons-material/Error'
import RemoveCircleIcon from '@mui/icons-material/RemoveCircleOutline'
import { subirUsuariosMasivo } from '../services/usuarioService'
import { useNotification } from '../context/NotificationContext'

/**
 * Modal de carga masiva de usuarios desde Excel. Soporta dos modos:
 *  - CREAR: da de alta usuarios nuevos; reporta duplicados.
 *  - ACTUALIZAR_HORARIO: solo actualiza el horario de usuarios ya existentes.
 *
 * Muestra un reporte fila por fila (creados / actualizados / sin cambios /
 * errores) con el número de fila y el número de control.
 *
 * @param {boolean}  open       Visibilidad del modal.
 * @param {function} onClose    Cierra el modal.
 * @param {function} onSuccess  Se llama tras una carga con cambios, para refrescar la tabla.
 */
function CargaMasivaUsuariosModal({ open, onClose, onSuccess }) {
  const [archivo, setArchivo] = useState(null)
  const [modo, setModo] = useState('CREAR')
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

  const handleCambiarModo = (e) => {
    setModo(e.target.value)
    setResultado(null) // el reporte anterior ya no aplica al nuevo modo
  }

  const handleProcesar = async () => {
    if (!archivo) {
      showNotification('Selecciona un archivo Excel primero', 'warning')
      return
    }
    setCargando(true)
    setResultado(null)
    try {
      const data = await subirUsuariosMasivo(archivo, modo)
      setResultado(data)
      const accion = modo === 'ACTUALIZAR_HORARIO' ? 'actualizados' : 'registrados'
      if (data.procesados > 0) {
        showNotification(
          `Carga finalizada: ${data.procesados} ${accion}, ${data.errores} con error.`,
          data.errores > 0 ? 'warning' : 'success',
        )
        onSuccess?.()
      } else {
        showNotification(
          `No hubo cambios. ${data.errores} fila(s) con error.`,
          data.errores > 0 ? 'error' : 'info',
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

  // Colorea cada línea del detalle según su contenido para lectura rápida.
  const colorLinea = (texto) => {
    const t = texto.toLowerCase()
    if (t.includes('creado') || t.includes('actualizado')) return 'success.main'
    if (t.includes('sin cambios')) return 'text.secondary'
    return 'error.main'
  }

  const esModoHorario = modo === 'ACTUALIZAR_HORARIO'

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Carga Masiva de Usuarios</DialogTitle>
      <DialogContent>
        {/* Selector de modo */}
        <FormControl sx={{ mb: 2 }}>
          <FormLabel>¿Qué deseas hacer?</FormLabel>
          <RadioGroup value={modo} onChange={handleCambiarModo}>
            <FormControlLabel
              value="CREAR"
              control={<Radio />}
              label="Crear usuarios nuevos"
            />
            <FormControlLabel
              value="ACTUALIZAR_HORARIO"
              control={<Radio />}
              label="Actualizar el horario de usuarios existentes"
            />
          </RadioGroup>
        </FormControl>

        {/* Aviso dinámico según el modo */}
        {esModoHorario ? (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Solo se actualizará la columna <b>horario_id</b> de los usuarios que
            <b> ya existen</b> (por su número de control). No se crean usuarios ni
            se modifican otros datos. Las filas sin <b>horario_id</b> se reportan
            sin cambios.
          </Alert>
        ) : (
          <Alert severity="info" sx={{ mb: 2 }}>
            Columnas esperadas (en orden): <b>numero_control</b>, <b>nombre</b>,{' '}
            <b>apellido_paterno</b>, <b>apellido_materno</b>, <b>area_id</b>,{' '}
            <b>rol</b>, <b>horario_id</b> (opcional). Los usuarios se registran como{' '}
            <b>activos</b> con rol <b>USER</b> y contraseña inicial automática
            (número de control + "-DIF"). Los números de control ya existentes se
            reportan como duplicados.
          </Alert>
        )}

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1, flexWrap: 'wrap' }}>
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
            <Box sx={{ display: 'flex', gap: 3, mb: 1, flexWrap: 'wrap' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <CheckCircleIcon color="success" fontSize="small" />
                <Typography variant="body2">
                  {esModoHorario ? 'Actualizados' : 'Registrados'}:{' '}
                  <b>{resultado.procesados}</b>
                </Typography>
              </Box>
              {typeof resultado.sinCambios === 'number' && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  <RemoveCircleIcon color="disabled" fontSize="small" />
                  <Typography variant="body2">
                    Sin cambios: <b>{resultado.sinCambios}</b>
                  </Typography>
                </Box>
              )}
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
                  Detalle por fila:
                </Typography>
                <List
                  dense
                  sx={{
                    maxHeight: 260,
                    overflow: 'auto',
                    bgcolor: 'action.hover',
                    borderRadius: 1,
                  }}>
                  {resultado.detalleErrores.map((linea, idx) => (
                    <ListItem key={idx} divider>
                      <ListItemText
                        primary={linea}
                        primaryTypographyProps={{
                          variant: 'body2',
                          color: colorLinea(linea),
                        }}
                      />
                    </ListItem>
                  ))}
                </List>
              </>
            )}

            {resultado.errores === 0 && resultado.procesados > 0 && (
              <Alert severity="success" sx={{ mt: 1 }}>
                Proceso completado sin errores.
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
