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
  Autocomplete,
  Chip,
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
  const { authData } = useAuth()
  const user = authData?.user

  useEffect(() => {
    getAreasForSelect().then((response) => {
      setAreas(response.data)
    })
  }, [])

  useEffect(() => {
    if (initialData) {
      setFormData({
        ...initialData,
        idsAreasGestionadas: initialData.idsAreasGestionadas || [],
      })
    } else {
      setFormData({ rol: 'USER', estatus: 'ACTIVE', idsAreasGestionadas: [] })
    }
  }, [initialData, open])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleAreasGestionadasChange = (event, newValue) => {
    const ids = newValue.map((area) => area.id)
    setFormData((prev) => ({ ...prev, idsAreasGestionadas: ids }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit(formData)
  }

  const availableRoles =
    user?.rol === 'SUPERADMIN'
      ? allRoles
      : allRoles.filter((r) => r.value === 'USER' || r.value === 'ADMIN') // Un ADMIN puede crear otros ADMINs en sus áreas

  const showPassword = formData.rol === 'ADMIN' || formData.rol === 'SUPERADMIN'

  const showAreasGestionadas = formData.rol === 'ADMIN'

  const selectedAreasValue = formData.idsAreasGestionadas
    ? areas.filter((area) => formData.idsAreasGestionadas.includes(area.id))
    : []
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
              onChange={handleChange}
              helperText={
                initialData
                  ? 'Dejar en blanco para no cambiar'
                  : 'Requerida para Admins'
              }
              fullWidth
              margin="normal"
              required={
                !initialData &&
                (formData.rol === 'ADMIN' || formData.rol === 'SUPERADMIN')
              }
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

          {showAreasGestionadas && (
            <Autocomplete
              multiple
              options={areas}
              value={selectedAreasValue}
              getOptionLabel={(option) => option.nombre}
              onChange={handleAreasGestionadasChange}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => (
                  <Chip
                    variant="outlined"
                    label={option.nombre}
                    {...getTagProps({ index })}
                  />
                ))
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  variant="outlined"
                  label="Áreas Gestionadas (opcional)"
                  placeholder="Seleccionar áreas"
                />
              )}
              sx={{ mt: 2 }}
            />
          )}

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
