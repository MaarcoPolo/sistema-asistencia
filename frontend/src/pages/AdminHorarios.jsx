import { useState } from 'react'
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
import { Box, Button, Typography, IconButton, Chip, Stack } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'

// Diccionario para convertir el número del día a texto corto
const mapaDias = {
  1: 'Lun',
  2: 'Mar',
  3: 'Mié',
  4: 'Jue',
  5: 'Vie',
  6: 'Sáb',
  7: 'Dom',
}

function AdminHorarios() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingHorario, setEditingHorario] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)
  const [tableKey, setTableKey] = useState(0)
  const { showNotification } = useNotification()

  // Definición de las columnas de la tabla
  const columns = [
    { id: 'nombre', label: 'Nombre del Horario' },
    {
      id: 'horaEntrada',
      label: 'Entrada',
      sortable: false,
      render: (row) => {
        // Tomamos la hora del primer día configurado
        const primerDetalle =
          row.detalles && row.detalles.length > 0 ? row.detalles[0] : null
        return primerDetalle && primerDetalle.horaEntrada
          ? primerDetalle.horaEntrada.substring(0, 5)
          : '---'
      },
    },
    {
      id: 'horaSalida',
      label: 'Salida',
      sortable: false,
      render: (row) => {
        const primerDetalle =
          row.detalles && row.detalles.length > 0 ? row.detalles[0] : null
        return primerDetalle && primerDetalle.horaSalida
          ? primerDetalle.horaSalida.substring(0, 5)
          : '---'
      },
    },
    {
      id: 'diasAsignados',
      label: 'Días Asignados',
      sortable: false,
      render: (row) => {
        if (!row.detalles || row.detalles.length === 0)
          return (
            <Typography variant="body2" color="error">
              Sin días
            </Typography>
          )

        // Ordenamos los días del 1 al 7 para que no salgan revueltos
        const diasOrdenados = [...row.detalles].sort((a, b) => a.dia - b.dia)

        return (
          <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
            {diasOrdenados.map((detalle) => (
              <Chip
                key={detalle.id || detalle.dia}
                label={mapaDias[detalle.dia]}
                size="small"
                color="primary"
                variant="outlined"
              />
            ))}
          </Stack>
        )
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
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
    } catch (error) {
      console.error('Error al guardar el horario:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al guardar el horario'
      showNotification(errorMessage, 'error')
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
      setTimeout(() => setTableKey((prev) => prev + 1), 300)
    } catch (error) {
      console.error('Error al eliminar el horario:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al eliminar el horario'
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

export default AdminHorarios
