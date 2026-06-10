import { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  TextField,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  CircularProgress,
  Tooltip,
  IconButton,
} from '@mui/material'
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf'
import SearchIcon from '@mui/icons-material/Search'
import {
  getResumenSanciones,
  exportarSancionesPdf,
  exportarSancionesExcel,
} from '../services/asistenciaService'
import { useNotification } from '../context/NotificationContext'
import TableChartIcon from '@mui/icons-material/TableChart'


function ResumenSancionesModal({ open, onClose }) {
  const [filtros, setFiltros] = useState({
    fechaInicio: '',
    fechaFin: '',
    usuarioId: '',
  })
  const [resultados, setResultados] = useState([])
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const { showNotification } = useNotification()

  const handleFilterChange = (event) => {
    const { name, value } = event.target
    setFiltros((prev) => ({ ...prev, [name]: value }))
  }

  const prepararParametros = () => {
    const params = {
      fechaInicio: filtros.fechaInicio,
      fechaFin: filtros.fechaFin,
    }
    if (filtros.usuarioId) params.usuarioId = filtros.usuarioId
    return params
  }

  const handleCalcular = async () => {
    if (!filtros.fechaInicio || !filtros.fechaFin) {
      showNotification('Por favor selecciona ambas fechas', 'warning')
      return
    }
    setLoading(true)
    try {
      const data = await getResumenSanciones(prepararParametros())
      setResultados(data)
      if (data.length === 0)
        showNotification('No se encontraron sanciones en este periodo', 'info')
    } catch {
      showNotification('Error al calcular las sanciones', 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleDescargarPdf = async () => {
    if (!filtros.fechaInicio || !filtros.fechaFin) {
      showNotification(
        'Por favor selecciona ambas fechas para exportar',
        'warning',
      )
      return
    }
    setDownloading(true)
    try {
      await exportarSancionesPdf(prepararParametros())
      showNotification('PDF generado con éxito', 'success')
    } catch {
      showNotification('Error al descargar el PDF', 'error')
    } finally {
      setDownloading(false)
    }
  }

  const handleDescargarExcel = async () => {
    if (!filtros.fechaInicio || !filtros.fechaFin) {
      showNotification(
        'Por favor selecciona ambas fechas para exportar',
        'warning',
      )
      return
    }
    setDownloading(true)
    try {
      await exportarSancionesExcel(prepararParametros())
      showNotification('Excel generado con éxito', 'success')
    } catch {
      showNotification('Error al descargar el Excel', 'error')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>Cálculo de Sanciones (Auditoría)</DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
          Selecciona las fechas para el corte. Puedes ingresar el ID del usuario
          si deseas generar el reporte de una sola persona.
        </Typography>

        <Box
          sx={{
            display: 'flex',
            gap: 2,
            mb: 3,
            alignItems: 'center',
            flexWrap: 'wrap',
          }}>
          <TextField
            name="fechaInicio"
            type="date"
            label="Fecha Inicio"
            InputLabelProps={{ shrink: true }}
            size="small"
            value={filtros.fechaInicio}
            onChange={handleFilterChange}
          />
          <TextField
            name="fechaFin"
            type="date"
            label="Fecha Fin"
            InputLabelProps={{ shrink: true }}
            size="small"
            value={filtros.fechaFin}
            onChange={handleFilterChange}
          />
          <TextField
            name="usuarioId"
            type="number"
            label="ID Usuario (Opcional)"
            placeholder="Ej: 15"
            size="small"
            value={filtros.usuarioId}
            onChange={handleFilterChange}
          />

          <Button
            variant="contained"
            onClick={handleCalcular}
            disabled={loading}
            startIcon={<SearchIcon />}>
            {loading ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              'Calcular'
            )}
          </Button>

          <Button
            variant="outlined"
            color="success"
            onClick={handleDescargarExcel}
            disabled={downloading}
            startIcon={<TableChartIcon />}>
            {downloading ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              'Exportar Excel'
            )}
          </Button>

          <Button
            variant="outlined"
            color="error"
            onClick={handleDescargarPdf}
            disabled={downloading}
            startIcon={<PictureAsPdfIcon />}>
            {downloading ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              'Exportar PDF'
            )}
          </Button>
        </Box>

        {resultados.length > 0 && (
          <TableContainer
            component={Paper}
            variant="outlined"
            sx={{ maxHeight: 400 }}>
            <Table stickyHeader size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ bgcolor: '#f5f5f5' }}>
                    <strong>No. Control</strong>
                  </TableCell>
                  <TableCell sx={{ bgcolor: '#f5f5f5' }}>
                    <strong>Nombre</strong>
                  </TableCell>
                  <TableCell sx={{ bgcolor: '#f5f5f5' }} align="center">
                    <strong>Retardos (Días)</strong>
                  </TableCell>
                  <TableCell sx={{ bgcolor: '#f5f5f5' }} align="center">
                    <strong>Faltas/Omisiones (Días)</strong>
                  </TableCell>
                  <TableCell sx={{ bgcolor: '#ffebee' }} align="center">
                    <strong>Total Descuento</strong>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {resultados.map((row) => (
                  <TableRow key={row.numeroControl}>
                    <TableCell>{row.numeroControl}</TableCell>
                    <TableCell>{row.nombreCompleto}</TableCell>
                    <TableCell align="center">
                      <Tooltip
                        title={`Fechas: ${row.fechasRetardos.join(', ') || 'Ninguna'}`}>
                        <Typography variant="body2" sx={{ cursor: 'help' }}>
                          {row.totalRetardos}{' '}
                          <Typography
                            component="span"
                            color="error"
                            variant="caption">
                            ({row.diasDescuentoRetardos})
                          </Typography>
                        </Typography>
                      </Tooltip>
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip
                        title={`Fechas: ${row.fechasFaltasYOmisiones.join(', ') || 'Ninguna'}`}>
                        <Typography variant="body2" sx={{ cursor: 'help' }}>
                          {row.totalFaltasYOmisiones}{' '}
                          <Typography
                            component="span"
                            color="error"
                            variant="caption">
                            ({row.diasDescuentoFaltas})
                          </Typography>
                        </Typography>
                      </Tooltip>
                    </TableCell>
                    <TableCell align="center">
                      <Typography color="error" fontWeight="bold">
                        {row.totalDiasDescontar}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="inherit">
          Cerrar
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default ResumenSancionesModal
