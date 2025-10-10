import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material'
import { getAreasForSelect } from '../services/areaService'
import { useAuth } from '../context/AuthContext.jsx'

const allRoles = [
  { value: 'SUPERADMIN', label: 'Superadministrador' },
  { value: 'ADMIN', label: 'Administrador' },
  { value: 'USER', label: 'Usuario' },
]
const estados = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
]

function UsuarioForm({ open, onClose, onSubmit, initialData }) {
  const [formData, setFormData] = useState({})
  const [areas, setAreas] = useState([])
  const { user } = useAuth()

  useEffect(() => {
    getAreasForSelect().then((response) => {
      setAreas(response.data)
    })
  }, [])

  useEffect(() => {
    if (initialData) {
      setFormData(initialData)
    } else {
      setFormData({ rol: 'USER', estatus: 'ACTIVE' })
    }
  }, [initialData, open])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit(formData)
  }

  const availableRoles =
    user?.role === 'SUPERADMIN'
      ? allRoles
      : allRoles.filter((r) => r.value === 'USER')

  const showPassword = formData.rol === 'ADMIN' || formData.rol === 'SUPERADMIN'

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {initialData ? 'Editar Usuario' : 'Crear Nuevo Usuario'}
        </DialogTitle>
        <DialogContent>
          <TextField
            name="matricula"
            label="Matrícula"
            value={formData.matricula || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <TextField
            name="nombre"
            label="Nombre(s)"
            value={formData.nombre || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <TextField
            name="apellidoPaterno"
            label="Apellido Paterno"
            value={formData.apellidoPaterno || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <TextField
            name="apellidoMaterno"
            label="Apellido Materno"
            value={formData.apellidoMaterno || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
          />

          {showPassword && (
            <TextField
              name="password"
              label="Contraseña"
              type="password"
              helperText={
                initialData
                  ? 'Dejar en blanco para no cambiar'
                  : 'Requerida para Admins'
              }
              fullWidth
              margin="normal"
              required={!initialData}
            />
          )}

          <FormControl fullWidth margin="normal">
            <InputLabel>Rol</InputLabel>
            <Select
              name="rol"
              value={formData.rol || 'USER'}
              label="Rol"
              onChange={handleChange}>
              {availableRoles.map((rol) => (
                <MenuItem key={rol.value} value={rol.value}>
                  {rol.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth margin="normal">
            <InputLabel>Área Principal</InputLabel>
            <Select
              name="idAreaPrincipal"
              value={formData.idAreaPrincipal || ''}
              label="Área Principal"
              onChange={handleChange}>
              {areas.map((area) => (
                <MenuItem key={area.id} value={area.id}>
                  {area.nombre}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl fullWidth margin="normal">
            <InputLabel>Estatus</InputLabel>
            <Select
              name="estatus"
              value={formData.estatus || ''}
              label="Estatus"
              onChange={handleChange}>
              {estados.map((est) => (
                <MenuItem key={est.value} value={est.value}>
                  {est.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} color="secondary">
            Cancelar
          </Button>
          <Button type="submit" variant="contained">
            Guardar
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default UsuarioForm
