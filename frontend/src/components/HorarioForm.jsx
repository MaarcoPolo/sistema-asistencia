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
  FormHelperText,
  Autocomplete,
  CircularProgress
} from '@mui/material'
import { getAreasForSelect } from '../services/areaService'
import { getUsuarios } from '../services/usuarioService'

function HorarioForm({ open, onClose, onSubmit, initialData }) {
  const [formData, setFormData] = useState({})
  const [areas, setAreas] = useState([])
  const [userOptions, setUserOptions] = useState([])
  const [userLoading, setUserLoading] = useState(false)
  const [userInputValue, setUserInputValue] = useState('')

  useEffect(() => {
    if (open) {
            getAreasForSelect().then(response => setAreas(response.data));
    }
  }, [open])

  useEffect(() => {
    if (!open) return

    setUserLoading(true)
    // Solo busca después de que el usuario deja de escribir por 500ms.
    const delayDebounceFn = setTimeout(() => {
      getUsuarios({ key: userInputValue, page: 0, size: 10 }).then(
        (response) => {
          setUserOptions(response.data.content)
          setUserLoading(false)
        }
      )
    }, 500)

    return () => clearTimeout(delayDebounceFn)
  }, [userInputValue, open])

  useEffect(() => {
    setFormData(initialData || {})
  }, [initialData, open])


  const handleChange = (event) => {
    const { name, value } = event.target
    if (name === 'idArea') {
      setFormData((prev) => ({ ...prev, idArea: value, idUsuario: '' }))
    } else if (name === 'idUsuario') {
      setFormData((prev) => ({ ...prev, idUsuario: value, idArea: '' }))
    } else {
      setFormData((prev) => ({ ...prev, [name]: value }))
    }
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onSubmit(formData)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {initialData ? 'Editar Horario' : 'Crear Nuevo Horario'}
        </DialogTitle>
        <DialogContent>
          <TextField
            name="nombre"
            label="Nombre del Horario"
            value={formData.nombre || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <TextField
            name="horaEntrada"
            label="Hora de Entrada"
            type="time"
            value={formData.horaEntrada || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            name="horaSalida"
            label="Hora de Salida"
            type="time"
            value={formData.horaSalida || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            name="toleranciaMinutos"
            label="Tolerancia (minutos)"
            type="number"
            value={formData.toleranciaMinutos || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
          />
          <FormControl fullWidth margin="normal">
            <InputLabel>Aplicar a Área (Opcional)</InputLabel>
            <Select
              name="idArea"
              value={formData.idArea || ''}
              label="Aplicar a Área (Opcional)"
              onChange={handleChange}
              disabled={!!formData.idUsuario}>
              <MenuItem value="">
                <em>Global / Ninguna</em>
              </MenuItem>
              {areas.map((area) => (
                <MenuItem key={area.id} value={area.id}>
                  {area.nombre}
                </MenuItem>
              ))}
            </Select>
            <FormHelperText>
              Si no selecciona Área ni Usuario, el horario será Global.
            </FormHelperText>
          </FormControl>
          <Autocomplete
            options={userOptions}
            getOptionLabel={(option) => option.nombreCompleto || ''}
            loading={userLoading}
            onInputChange={(event, newInputValue) => {
              setUserInputValue(newInputValue)
            }}
            onChange={(event, newValue) => {
              setFormData((prev) => ({
                ...prev,
                idUsuario: newValue ? newValue.id : '',
                idArea: '',
              }))
            }}
            disabled={!!formData.idArea}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Aplicar a Usuario (Opcional)"
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <>
                      {userLoading ? (
                        <CircularProgress color="inherit" size={20} />
                      ) : null}
                      {params.InputProps.endAdornment}
                    </>
                  ),
                }}
              />
            )}
          />
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
export default HorarioForm
