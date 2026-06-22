import { useState } from 'react'
import {
  Box,
  Typography,
  Button,
  IconButton,
  Tooltip,
  Chip,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControlLabel,
  Switch,
  Stack,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import FileDownloadIcon from '@mui/icons-material/FileDownload'

import DynamicTable from '../components/DynamicTable'
import ConfirmationDialog from '../components/ConfirmationDialog'
import { useNotification } from '../context/NotificationContext'
import {
  getJustificaciones,
  createJustificacion,
  updateJustificacion,
  deleteJustificacion,
  exportarJustificacionesExcel,
} from '../services/justificacionService'

function AdminJustificaciones() {
  const [tableKey, setTableKey] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)
  const [formData, setFormData] = useState({
    clave: '',
    nombre: '',
    requiereObservacion: false,
  })
  const [loading, setLoading] = useState(false)

  // Estados para eliminar
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)

  const [exportando, setExportando] = useState(false)

  const { showNotification } = useNotification()

  const handleExportar = async () => {
    setExportando(true)
    try {
      await exportarJustificacionesExcel()
    } catch (error) {
      console.error('Error al exportar justificaciones:', error)
      showNotification('No se pudo exportar el archivo', 'error')
    } finally {
      setExportando(false)
    }
  }

  const columns = [
    { id: 'clave', label: 'Clave' },
    { id: 'nombre', label: 'Nombre / Motivo' },
    {
      id: 'requiereObservacion',
      label: '¿Requiere Observación?',
      render: (row) =>
        row.requiereObservacion ? (
          <Chip label="Obligatorio" color="warning" size="small" />
        ) : (
          <Chip label="Opcional" color="default" size="small" />
        ),
    },
  ]

  // --- MANEJO DEL FORMULARIO (CREAR / EDITAR) ---
  const handleOpenModal = (record = null) => {
    if (record) {
      setEditingRecord(record)
      setFormData({
        clave: record.clave,
        nombre: record.nombre,
        requiereObservacion: record.requiereObservacion,
      })
    } else {
      setEditingRecord(null)
      setFormData({ clave: '', nombre: '', requiereObservacion: false })
    }
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    setModalOpen(false)
    setEditingRecord(null)
  }

  const handleChange = (e) => {
    const { name, value, checked, type } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      if (editingRecord) {
        await updateJustificacion(editingRecord.id, formData)
        showNotification('Motivo actualizado correctamente', 'success')
      } else {
        await createJustificacion(formData)
        showNotification('Motivo creado correctamente', 'success')
      }
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
      handleCloseModal()
    } catch (error) {
      showNotification(
        error.response?.data?.message || 'Error al guardar el motivo',
        'error',
      )
    } finally {
      setLoading(false)
    }
  }

  // --- MANEJO DE ELIMINACIÓN ---
  const handleDeleteClick = (record) => {
    setConfirmAction(() => () => executeDelete(record.id))
    setConfirmData({
      title: 'Confirmar Eliminación',
      message: `¿Estás seguro de que deseas eliminar el motivo "${record.nombre}"? Esta acción no se puede deshacer y podría afectar el historial si ya se usó.`,
    })
    setConfirmOpen(true)
  }

  const executeDelete = async (id) => {
    try {
      await deleteJustificacion(id)
      showNotification('Motivo eliminado correctamente', 'success')
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
    } catch (error) {
      showNotification(
        error.response?.data?.message || 'Error al eliminar el motivo',
        'error',
      )
    } finally {
      setConfirmOpen(false)
    }
  }

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          justifyContent: 'space-between',
          alignItems: { xs: 'stretch', sm: 'center' },
          gap: 2,
          mb: 3,
        }}>
        <Typography variant="h4" component="h1">
          Catálogo de Justificaciones
        </Typography>
        <Stack
          direction="row"
          spacing={1}
          sx={{ flexWrap: 'wrap', gap: 1, justifyContent: { xs: 'flex-start', sm: 'flex-end' } }}>
          <Button
            variant="outlined"
            startIcon={<FileDownloadIcon />}
            onClick={handleExportar}
            disabled={exportando}>
            {exportando ? 'Exportando...' : 'Exportar Excel'}
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenModal()}>
            Nuevo Motivo
          </Button>
        </Stack>
      </Box>

      <DynamicTable
        key={tableKey}
        columns={columns}
        fetchDataFunction={getJustificaciones}
        initialSort={{ field: 'clave', direction: 'asc' }}
        renderActions={(row) => (
          <>
            <Tooltip title="Editar">
              <IconButton color="primary" onClick={() => handleOpenModal(row)}>
                <EditIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Eliminar">
              <IconButton color="error" onClick={() => handleDeleteClick(row)}>
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          </>
        )}
      />

      {/* MODAL DE FORMULARIO */}
      <Dialog
        open={modalOpen}
        onClose={handleCloseModal}
        maxWidth="sm"
        fullWidth>
        <DialogTitle>
          {editingRecord ? 'Editar Motivo' : 'Nuevo Motivo de Justificación'}
        </DialogTitle>
        <form onSubmit={handleSave}>
          <DialogContent dividers>
            <TextField
              autoFocus
              fullWidth
              label="Clave (Ej. PERM-ECO)"
              name="clave"
              value={formData.clave}
              onChange={handleChange}
              required
              margin="normal"
            />
            <TextField
              fullWidth
              label="Nombre Oficial (Ej. Permiso Económico)"
              name="nombre"
              value={formData.nombre}
              onChange={handleChange}
              required
              margin="normal"
            />
            <Box sx={{ mt: 2 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.requiereObservacion}
                    onChange={handleChange}
                    name="requiereObservacion"
                    color="warning"
                  />
                }
                label="Exigir que Recursos Humanos escriba una observación al seleccionar este motivo"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={handleCloseModal}
              color="inherit"
              disabled={loading}>
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              disabled={loading}>
              {loading ? 'Guardando...' : 'Guardar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* MODAL DE CONFIRMACIÓN DE ELIMINACIÓN */}
      <ConfirmationDialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={confirmAction}
        title={confirmData?.title}
        message={confirmData?.message}
      />
    </Box>
  )
}

export default AdminJustificaciones