import { useState, useEffect } from 'react'
import {
  getHorarios,
  createHorario,
  updateHorario,
  deleteHorario,
} from '../services/horarioService'
import HorarioForm from '../components/HorarioForm'
import ConfirmationDialog from '../components/ConfirmationDialog'
import { useNotification } from '../context/NotificationContext'
import DynamicTable from '../components/DynamicTable'
import { Box, Button, Typography, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'

function AdminHorarios() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingHorario, setEditingHorario] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)
  const [tableKey, setTableKey] = useState(0)
  const { showNotification } = useNotification()

  const columns = [
    { id: 'nombre', label: 'Nombre del Horario' },
    { id: 'horaEntrada', label: 'Entrada' },
    { id: 'horaSalida', label: 'Salida' },
    {
      id: 'aplicaA',
      label: 'Aplica a',
      sortable: false,
      // Usamos nuestra nueva función 'render' para mostrar el texto personalizado
      render: (row) => {
        if (row.nombreUsuario) return `Usuario: ${row.nombreUsuario}`
        if (row.nombreArea) return `Área: ${row.nombreArea}`
        return 'Global'
      },
    },
  ]

  const handleOpenModal = (horario = null) => {
    setEditingHorario(horario)
    setModalOpen(true)
  }
  const handleCloseModal = () => {
    setModalOpen(false)
    setEditingHorario(null)
  }

  const handleSubmit = (formData) => {
    const action = editingHorario ? 'update' : 'create'
    const title =
      action === 'update' ? 'Confirmar Actualización' : 'Confirmar Creación'
    const message = `¿Estás seguro de que quieres guardar el horario ${formData.nombre}?`

    setConfirmAction(() => () => executeSave(formData, editingHorario))
    setConfirmData({ title, message })
    setConfirmOpen(true)
  }

  const executeSave = async (formData, horarioToEdit) => {
    try {
      if (horarioToEdit) {
        await updateHorario(horarioToEdit.id, {
          ...formData,
          id: horarioToEdit.id,
        })
        showNotification('Horario actualizado con éxito', 'success')
      } else {
        await createHorario(formData)
        showNotification('Horario creado con éxito', 'success')
      }
    } catch (error) {
      showNotification('Error al guardar el horario', 'error')
    } finally {
      handleCloseModal()
      setConfirmOpen(false)
    }
  }

  const handleDeleteClick = (horario) => {
    setConfirmAction(() => () => executeDelete(horario.id))
    setConfirmData({
      title: 'Confirmar Eliminación',
      message: `¿Estás seguro de que quieres eliminar el horario ${horario.nombre}?`,
    })
    setConfirmOpen(true)
  }

  const executeDelete = async (id) => {
    try {
      await deleteHorario(id)
      showNotification('Horario eliminado con éxito', 'success')
    } catch (error) {
      showNotification('Error al eliminar el horario', 'error')
    } finally {
      setConfirmOpen(false)
    }
  }

  const getAplicaA = (horario) => {
    if (horario.idUsuario) return `Usuario: ${horario.nombreUsuario}`
    if (horario.idArea) return `Área: ${horario.nombreArea}`
    return 'Global'
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
          Gestión de Horarios
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenModal()}>
          Crear Horario
        </Button>
      </Box>

      <DynamicTable
        key={tableKey}
        columns={columns}
        fetchDataFunction={getHorarios}
        initialSort={{ field: 'nombre', direction: 'asc' }}
        renderActions={(horario) => (
          <>
            <IconButton
              color="primary"
              onClick={() => handleOpenModal(horario)}>
              <EditIcon />
            </IconButton>
            <IconButton
              color="error"
              onClick={() => handleDeleteClick(horario)}>
              <DeleteIcon />
            </IconButton>
          </>
        )}
      />

      <HorarioForm
        open={modalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSubmit}
        initialData={editingHorario}
      />
      {/* ... (ConfirmationDialog no cambia) */}
    </Box>
  )
}

export default AdminHorarios
