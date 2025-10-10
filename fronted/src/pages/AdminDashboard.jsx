import { useState, useCallback } from 'react'
import {
  getReporteAsistencias,
  createAsistencia,
  updateAsistencia,
  deleteAsistencia,
} from '../services/asistenciaService'
import DynamicTable from '../components/DynamicTable'
import AsistenciaForm from '../components/AsistenciaForm'
import ConfirmationDialog from '../components/ConfirmationDialog'
import { useNotification } from '../context/NotificationContext'
import { Box, Button, Typography, IconButton, TextField } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'

function AdminDashboard() {
  // Estados para manejar los modales y diálogos de confirmación
  const [modalOpen, setModalOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)

  // Estado para forzar la recarga de la tabla después de una acción
  const [tableKey, setTableKey] = useState(0)

  // Estado para los filtros de fecha
  const [filters, setFilters] = useState({ fechaInicio: '', fechaFin: '' })

  const { showNotification } = useNotification()

  // Definición de las columnas para la DynamicTable
  const columns = [
    { id: 'usuarioMatricula', label: 'Matrícula', sortId: 'usuario.matricula' },
    { id: 'usuarioNombreCompleto', label: 'Nombre', sortable: false },
    { id: 'areaNombre', label: 'Área', sortId: 'usuario.areaPrincipal.nombre' },
    { id: 'fecha', label: 'Fecha' },
    {
      id: 'horaEntrada',
      label: 'Entrada',
      render: (row) =>
        row.horaEntrada
          ? new Date(row.horaEntrada).toLocaleTimeString()
          : '---',
    },
    {
      id: 'horaSalida',
      label: 'Salida',
      render: (row) =>
        row.horaSalida ? new Date(row.horaSalida).toLocaleTimeString() : '---',
    },
    {
      id: 'esRetardo',
      label: 'Retardo',
      render: (row) => (row.esRetardo ? 'Sí' : 'No'),
    },
  ]

  // Función que combina los parámetros de la tabla con nuestros filtros y llama a la API
    const fetchDataWithFilters = async (params) => {
    const allParams = { ...params, ...filters }
    if (!allParams.fechaInicio) delete allParams.fechaInicio
    if (!allParams.fechaFin) delete allParams.fechaFin

    // El 'await' espera a que la llamada a la API termine y obtiene la respuesta
    const response = await getReporteAsistencias(allParams)
    return response // Devuelve el objeto de respuesta completo
    }

  // --- Lógica para abrir/cerrar modales ---
  const handleOpenModal = (record = null) => {
    setEditingRecord(record)
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    setModalOpen(false)
    setEditingRecord(null)
  }

  const handleSubmit = (formData) => {
    const action = editingRecord ? 'update' : 'create'
    const title =
      action === 'update'
        ? 'Confirmar Actualización'
        : 'Confirmar Creación Manual'
    const message = `¿Estás seguro de que quieres guardar este registro de asistencia?`

    setConfirmAction(() => () => executeSave(formData, editingRecord))
    setConfirmData({ title, message })
    setConfirmOpen(true)
  }

  const executeSave = async (formData, recordToEdit) => {
    try {
      if (recordToEdit) {
        await updateAsistencia(recordToEdit.idAsistencia, formData)
        showNotification('Asistencia actualizada con éxito', 'success')
      } else {
        await createAsistencia(formData)
        showNotification('Asistencia creada con éxito', 'success')
      }
      setTableKey((prev) => prev + 1) // Forzar recarga de la tabla
    } catch (error) {
      showNotification('Error al guardar el registro', 'error')
    } finally {
      handleCloseModal()
      setConfirmOpen(false)
    }
  }

  const handleDeleteClick = (record) => {
    setConfirmAction(() => () => executeDelete(record.idAsistencia))
    setConfirmData({
      title: 'Confirmar Eliminación',
      message: `¿Estás seguro de que quieres eliminar este registro de asistencia?`,
    })
    setConfirmOpen(true)
  }

  const executeDelete = async (id) => {
    try {
      await deleteAsistencia(id)
      showNotification('Registro eliminado con éxito', 'success')
      setTableKey((prev) => prev + 1)
    } catch (error) {
      showNotification('Error al eliminar el registro', 'error')
    } finally {
      setConfirmOpen(false)
    }
  }

  // --- Handler para actualizar los filtros de fecha ---
  const handleFilterChange = (event) => {
    const { name, value } = event.target
    setFilters((prev) => ({ ...prev, [name]: value }))
  }

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 2,
        }}>
        <Typography variant="h4" component="h1">
          Dashboard de Asistencias
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenModal()}>
          Crear Registro Manual
        </Button>
      </Box>

      {/* Controles de Filtro */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <TextField
          name="fechaInicio"
          type="date"
          label="Fecha Inicio"
          InputLabelProps={{ shrink: true }}
          onChange={handleFilterChange}
        />
        <TextField
          name="fechaFin"
          type="date"
          label="Fecha Fin"
          InputLabelProps={{ shrink: true }}
          onChange={handleFilterChange}
        />
      </Box>

      <DynamicTable
        key={tableKey}
        columns={columns}
        fetchDataFunction={fetchDataWithFilters}
        renderActions={(row) => (
          <>
            <IconButton color="primary" onClick={() => handleOpenModal(row)}>
              <EditIcon />
            </IconButton>
            <IconButton color="error" onClick={() => handleDeleteClick(row)}>
              <DeleteIcon />
            </IconButton>
          </>
        )}
      />

      <AsistenciaForm
        open={modalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSubmit}
        initialData={editingRecord}
      />
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

export default AdminDashboard
