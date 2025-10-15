import { useState } from 'react'
import {
  getAreas,
  createArea,
  updateArea,
  deleteArea,
} from '../services/areaService'
import AreaForm from '../components/AreaForm'
import ConfirmationDialog from '../components/ConfirmationDialog'
import { useNotification } from '../context/NotificationContext'
import DynamicTable from '../components/DynamicTable'
import { Box, Button, Typography, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'

function AdminAreas() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingArea, setEditingArea] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)
  const [tableKey, setTableKey] = useState(0)
  const { showNotification } = useNotification()

  const columns = [
    { id: 'clave', label: 'Clave' },
    { id: 'nombre', label: 'Nombre' },
    {
      id: 'nombreAreaPadre',
      sortId: 'areaPadre.nombre',
      label: 'Área Padre',
    },
    { id: 'estatus', label: 'Estatus' },
  ]

  const handleOpenModal = (area = null) => {
    setEditingArea(area)
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    setModalOpen(false)
    setEditingArea(null)
  }

  const handleSubmit = (formData) => {
    const action = editingArea ? 'update' : 'create'
    const title =
      action === 'update' ? 'Confirmar Actualización' : 'Confirmar Creación'
    const message = `¿Estás seguro de que quieres guardar el área ${formData.nombre}?`

    setConfirmAction(() => () => executeSave(formData, editingArea))
    setConfirmData({ title, message })
    setConfirmOpen(true)
  }

  const executeSave = async (formData, areaToEdit) => {
    try {
      if (areaToEdit) {
        await updateArea(areaToEdit.id, { ...formData, id: areaToEdit.id })
        showNotification('Área actualizada con éxito', 'success')
      } else {
        await createArea(formData)
        showNotification('Área creada con éxito', 'success')
      }
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
    } catch (error) {
      console.error('Error al guardar el área:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al guardar el área'
      showNotification(errorMessage, 'error')
    } finally {
      handleCloseModal()
      setConfirmOpen(false)
    }
  }

  const handleDeleteClick = (area) => {
    setConfirmAction(() => () => executeDelete(area.id))
    setConfirmData({
      title: 'Confirmar Eliminación',
      message: `¿Estás seguro de que quieres eliminar el área ${area.nombre}?`,
    })
    setConfirmOpen(true)
  }

  const executeDelete = async (id) => {
    try {
      await deleteArea(id)
      showNotification('Área eliminada con éxito', 'success')
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
    } catch (error) {
      console.error('Error al eliminar el área:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al eliminar el área'
      showNotification(errorMessage, 'error')
    } finally {
      setConfirmOpen(false)
    }
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
          Gestión de Áreas
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenModal()}>
          Crear Área
        </Button>
      </Box>

      <DynamicTable
        key={tableKey}
        columns={columns}
        fetchDataFunction={getAreas}
        initialSort={{ field: 'nombre', direction: 'asc' }}
        renderActions={(area) => (
          <>
            <IconButton color="primary" onClick={() => handleOpenModal(area)}>
              <EditIcon />
            </IconButton>
            <IconButton color="error" onClick={() => handleDeleteClick(area)}>
              <DeleteIcon />
            </IconButton>
          </>
        )}
      />

      <AreaForm
        open={modalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSubmit}
        initialData={editingArea}
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

export default AdminAreas
