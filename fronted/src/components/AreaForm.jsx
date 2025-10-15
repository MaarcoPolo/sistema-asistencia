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

const estados = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'Inactivo' },
]

function AreaForm({ open, onClose, onSubmit, initialData }) {
  const [formData, setFormData] = useState({})
  const [areas, setAreas] = useState([])

  useEffect(() => {
    getAreasForSelect().then((response) => {
      setAreas(response.data)
    })
  }, [])

  useEffect(() => {
    setFormData(initialData || { estatus: 'ACTIVE' })
  }, [initialData, open])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit(formData)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {initialData ? 'Editar Área' : 'Crear Nueva Área'}
        </DialogTitle>
        <DialogContent>
          <TextField
            name="clave"
            label="Clave"
            value={formData.clave || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <TextField
            name="nombre"
            label="Nombre del Área"
            value={formData.nombre || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />

          <FormControl fullWidth margin="normal">
            <InputLabel>Área Padre (Opcional)</InputLabel>
            <Select
              name="idAreaPadre"
              value={formData.idAreaPadre || ''}
              label="Área Padre (Opcional)"
              onChange={handleChange}>
              <MenuItem value="">
                <em>Ninguna</em>
              </MenuItem>
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

export default AreaForm
