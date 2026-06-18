import { useState, useEffect, useMemo } from 'react'
import { createUsuario, updateUsuario, deleteUsuario, getUsuarios, resetPasswordUsuario } from '../services/usuarioService'
import UsuarioForm from '../components/UsuarioForm'
import ConfirmationDialog from '../components/ConfirmationDialog'
import ExportarUsuariosModal from '../components/ExportarUsuariosModal'
import CargaMasivaUsuariosModal from '../components/CargaMasivaUsuariosModal'
import { useNotification } from '../context/NotificationContext'
import DynamicTable from '../components/DynamicTable'
import { Box, Button, Typography, IconButton, Tooltip, TextField, Autocomplete, Grid, Paper, Stack } from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import EditIcon from '@mui/icons-material/Edit'
import DeleteIcon from '@mui/icons-material/Delete'
import VpnKeyIcon from '@mui/icons-material/VpnKey'
import FileDownloadIcon from '@mui/icons-material/FileDownload'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import { getAreasForSelect } from '../services/areaService'

function AdminUsuarios() {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [confirmAction, setConfirmAction] = useState(null)
  const [confirmData, setConfirmData] = useState(null)
  const [tableKey, setTableKey] = useState(0)
  const [exportModalOpen, setExportModalOpen] = useState(false)
  const [cargaMasivaOpen, setCargaMasivaOpen] = useState(false)
  const { showNotification } = useNotification()
  const [areas, setAreas] = useState([])
  // `filtros` es lo que escribe el usuario; `filtrosAplicados` es lo que
  // realmente se envía a la tabla (con debounce sobre el número de control
  // para no disparar una petición por cada tecla).
  const [filtros, setFiltros] = useState({ numeroControl: '', areaId: '' })
  const [filtrosAplicados, setFiltrosAplicados] = useState({ numeroControl: '', areaId: '' })

  useEffect(() => {
    const timerId = setTimeout(() => {
      setFiltrosAplicados(filtros)
    }, 500)
    return () => clearTimeout(timerId)
  }, [filtros])

  // Memoizado para conservar la misma referencia entre renders: evita que
  // DynamicTable re-ejecute su carga de datos en cada render del padre (PERF-013).
  const columns = useMemo(() => [
    { id: 'numeroControl', label: 'No. Control' },
    { id: 'nombreCompleto', label: 'Nombre Completo', sortable: false },
    { id: 'rol', label: 'Rol' },
    {
      id: 'nombreAreaPrincipal',
      sortId: 'areaPrincipal.nombre',
      label: 'Área Principal',
    },
    { id: 'estatus', label: 'Estatus' },
  ], [])

  useEffect(() => {
    getAreasForSelect().then(res => {
      const data = res.data?.data || res.data || []
      setAreas(Array.isArray(data) ? data : [])
    })
  }, [])

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
      setTableKey((prev) => prev + 1)
    } catch (error) {
      console.error('Error al guardar el usuario:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al guardar el usuario'
      showNotification(errorMessage, 'error')
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
      setTimeout(() => {
        setTableKey((prev) => prev + 1)
      }, 300)
    } catch (error) {
      console.error('Error al eliminar el usuario:', error)
      const errorMessage =
        error.response?.data?.message || 'Error al eliminar el usuario'
      showNotification(errorMessage, 'error')
    } finally {
      setConfirmOpen(false)
    }
  }

  const handleResetPassword = (user) => {
    setConfirmAction(() => () => executeResetPassword(user.id))
    setConfirmData({
      title: 'Restablecer Contraseña',
      message: `¿Seguro que deseas restablecer la contraseña de ${user.nombreCompleto}? Volverá a ser su Número de Control + "-DIF".`,
    })
    setConfirmOpen(true)
  }

  const executeResetPassword = async (id) => {
    try {
      await resetPasswordUsuario(id)
      showNotification('Contraseña restablecida con éxito', 'success')
    } catch (error) {
      showNotification(error.response?.data?.message || 'Error al restablecer contraseña', 'error')
    } finally {
      setConfirmOpen(false)
    }
  }

  const handleFiltroChange = (campo, valor) => {
    setFiltros(prev => ({ ...prev, [campo]: valor }))
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
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            startIcon={<UploadFileIcon />}
            onClick={() => setCargaMasivaOpen(true)}>
            Carga Masiva
          </Button>
          <Button
            variant="outlined"
            startIcon={<FileDownloadIcon />}
            onClick={() => setExportModalOpen(true)}>
            Exportar Excel
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenModal()}>
            Crear Usuario
          </Button>
        </Stack>
      </Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          size="small"
          label="Filtrar por No. Control"
          value={filtros.numeroControl}
          onChange={(e) => handleFiltroChange('numeroControl', e.target.value)}
          sx={{ width: { xs: '100%', sm: '250px' } }}
        />
        <Autocomplete
          size="small"
          options={areas}
          getOptionLabel={(option) => option.nombre}
          onChange={(e, newValue) => handleFiltroChange('areaId', newValue ? newValue.id : null)}
          sx={{ width: { xs: '100%', sm: '400px' } }}
          renderInput={(params) => (
            <TextField {...params} label="Filtrar por Departamento / Área" />
          )}
        />
      </Box>
      <DynamicTable
        key={tableKey}
        columns={columns}
        fetchDataFunction={getUsuarios}
        extraFilters={filtrosAplicados}
        initialSort={{ field: 'numeroControl', direction: 'asc' }}
        renderActions={(user) => (
          <>
            <IconButton color="primary" onClick={() => handleOpenModal(user)}>
              <EditIcon />
            </IconButton>
            <IconButton color="error" onClick={() => handleDeleteClick(user)}>
              <DeleteIcon />
            </IconButton>
            <Tooltip title="Restablecer Contraseña">
              <IconButton color="warning" onClick={() => handleResetPassword(user)}>
                <VpnKeyIcon />
              </IconButton>
            </Tooltip>
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
      <ExportarUsuariosModal
        open={exportModalOpen}
        onClose={() => setExportModalOpen(false)}
        areas={areas}
      />
      <CargaMasivaUsuariosModal
        open={cargaMasivaOpen}
        onClose={() => setCargaMasivaOpen(false)}
        onSuccess={() => setTableKey((prev) => prev + 1)}
      />
    </Box>
  )
}
export default AdminUsuarios
