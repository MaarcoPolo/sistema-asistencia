import { useState } from 'react'
import {
  createUsuario,
  updateUsuario,
  deleteUsuario,
  getUsuarios,
} from '../services/usuarioService'
import UsuarioForm from '../components/UsuarioForm'
import ConfirmationDialog from '../components/ConfirmationDialog'
import { useNotification } from '../context/NotificationContext'
import DynamicTable from '../components/DynamicTable' // <-- Importamos nuestra tabla
import { Box, Button, Typography, IconButton } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'

function AdminUsuarios() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)
  // 'key' para forzar la recarga de la tabla
  const [tableKey, setTableKey] = useState(0)
  const { showNotification } = useNotification()

  const columns = [
    { id: 'matricula', label: 'Matrícula' },
    { id: 'nombreCompleto', label: 'Nombre Completo', sortable: false },
    { id: 'rol', label: 'Rol' },
    {
      id: 'nombreAreaPrincipal',
      sortId: 'areaPrincipal.nombre',
      label: 'Área Principal',
    },
    { id: 'estatus', label: 'Estatus' },
  ]

  const handleOpenModal = (user = null) => {
    setEditingUser(user)
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    setModalOpen(false)
    setEditingUser(null)
  }

  const handleSubmit = (formData) => {
    const action = editingUser ? 'update' : 'create'
    const title =
      action === 'update' ? 'Confirmar Actualización' : 'Confirmar Creación'
    const message =
      action === 'update'
        ? `¿Estás seguro de que quieres guardar los cambios para el usuario ${formData.nombre}?`
        : `¿Estás seguro de que quieres crear al usuario ${formData.nombre}?`

    setConfirmAction(() => () => executeSave(formData, editingUser))
    setConfirmData({ title, message })
    setConfirmOpen(true)
  }
  const executeSave = async (formData, userToEdit) => {
    try {
      if (userToEdit) {
        await updateUsuario(userToEdit.id, { ...formData, id: userToEdit.id })
        showNotification('Usuario actualizado con éxito', 'success')
      } else {
        await createUsuario(formData)
        showNotification('Usuario creado con éxito', 'success')
      }
      setTableKey((prev) => prev + 1) // Forzamos la recarga de la tabla
    } catch (error) {
      showNotification('Error al guardar el usuario', 'error')
    } finally {
      handleCloseModal()
      setConfirmOpen(false)
    }
  }
  const handleDeleteClick = (user) => {
    setConfirmAction(() => () => executeDelete(user.id))
    setConfirmData({
      title: 'Confirmar Eliminación',
      message: `¿Estás seguro de que quieres eliminar al usuario ${user.nombreCompleto}? Esta acción es irreversible.`,
    })
    setConfirmOpen(true)
  }
  const executeDelete = async (id) => {
    try {
      await deleteUsuario(id)
      showNotification('Usuario eliminado con éxito', 'success')
      setTableKey((prev) => prev + 1) // Forzamos la recarga
    } catch (error) {
      showNotification('Error al eliminar el usuario', 'error')
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
          Gestión de Usuarios
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenModal()}>
          Crear Usuario
        </Button>
      </Box>

      <DynamicTable
        key={tableKey} // La key fuerza el re-montaje del componente
        columns={columns}
        fetchDataFunction={getUsuarios}
        initialSort={{ field: 'matricula', direction: 'asc' }}
        renderActions={(user) => (
          <>
            <IconButton color="primary" onClick={() => handleOpenModal(user)}>
              <EditIcon />
            </IconButton>
            <IconButton color="error" onClick={() => handleDeleteClick(user)}>
              <DeleteIcon />
            </IconButton>
          </>
        )}
      />

      <UsuarioForm
        open={modalOpen}
        onClose={handleCloseModal}
        onSubmit={handleSubmit}
        initialData={editingUser}
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
export default AdminUsuarios
