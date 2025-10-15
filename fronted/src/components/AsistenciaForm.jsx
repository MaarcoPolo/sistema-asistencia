import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Autocomplete,
  Checkbox,
  FormControlLabel,
  CircularProgress,
} from '@mui/material'
import { getUsuarios } from '../services/usuarioService'

function AsistenciaForm({ open, onClose, onSubmit, initialData }) {
  const [formData, setFormData] = useState({})
  const [userOptions, setUserOptions] = useState([])
  const [userLoading, setUserLoading] = useState(false)
  const [userInputValue, setUserInputValue] = useState('')

  // para buscar usuarios dinámicamente
  useEffect(() => {
    if (!open) return
    setUserLoading(true)
    const delayDebounceFn = setTimeout(() => {
      getUsuarios({ key: userInputValue, size: 10 }).then((res) => {
        setUserOptions(res.data.content)
        setUserLoading(false)
      })
    }, 500)
    return () => clearTimeout(delayDebounceFn)
  }, [userInputValue, open])

  // para inicializar el formulario
  useEffect(() => {
    if (open) {
      if (initialData) {
        // Modo Edición
        const initialUser = {
          id: initialData.usuarioId,
          nombreCompleto: initialData.usuarioNombreCompleto,
        }
        setUserOptions([initialUser])
        setFormData({
          ...initialData,
          usuarioId: initialData.usuarioId,
          // Formatear fechas para los inputs datetime-local
          horaEntrada: initialData.horaEntrada
            ? new Date(initialData.horaEntrada).toISOString().slice(0, 16)
            : '',
          horaSalida: initialData.horaSalida
            ? new Date(initialData.horaSalida).toISOString().slice(0, 16)
            : '',
        })
      } else {
        // Modo Creación
        setFormData({
          esRetardo: false,
          fecha: new Date().toISOString().split('T')[0],
        })
        setUserOptions([])
      }
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

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {initialData ? 'Editar Asistencia' : 'Crear Asistencia Manual'}
        </DialogTitle>
        <DialogContent>
          <Autocomplete
            sx={{ mt: 2 }}
            options={userOptions}
            getOptionLabel={(option) => option.nombreCompleto || ''}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            loading={userLoading}
            value={userOptions.find((u) => u.id === formData.usuarioId) || null}
            onInputChange={(event, newInputValue) => {
              setUserInputValue(newInputValue)
            }}
            onChange={(event, newValue) => {
              setFormData((prev) => ({
                ...prev,
                usuarioId: newValue ? newValue.id : '',
              }))
            }}
            renderInput={(
              params
            ) => (
              <TextField
                {...params}
                label="Usuario"
                required
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
          <TextField
            name="fecha"
            label="Fecha"
            type="date"
            value={formData.fecha || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            name="horaEntrada"
            label="Hora Entrada"
            type="datetime-local"
            value={formData.horaEntrada || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            name="horaSalida"
            label="Hora Salida"
            type="datetime-local"
            value={formData.horaSalida || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            InputLabelProps={{ shrink: true }}
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={formData.esRetardo || false}
                onChange={(e) =>
                  setFormData((p) => ({ ...p, esRetardo: e.target.checked }))
                }
              />
            }
            label="Es Retardo"
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
export default AsistenciaForm
