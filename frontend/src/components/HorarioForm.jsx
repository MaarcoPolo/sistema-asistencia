import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Typography,
  Box,
} from '@mui/material'

const diasSemana = [
  { value: 1, label: 'Lunes' },
  { value: 2, label: 'Martes' },
  { value: 3, label: 'Miércoles' },
  { value: 4, label: 'Jueves' },
  { value: 5, label: 'Viernes' },
  { value: 6, label: 'Sábado' },
  { value: 7, label: 'Domingo' },
]

function HorarioForm({ open, onClose, onSubmit, initialData }) {
  const [formData, setFormData] = useState({
    nombre: '',
    detalles: [], // Ahora guardaremos un objeto completo por cada día
  })

  useEffect(() => {
    if (open) {
      if (initialData) {
        // Modo Edición: Cargamos los días con sus horas específicas
        setFormData({
          nombre: initialData.nombre || '',
          detalles: initialData.detalles
            ? initialData.detalles.map((d) => ({
                dia: d.dia,
                horaEntrada: d.horaEntrada.substring(0, 5),
                horaSalida: d.horaSalida.substring(0, 5),
              }))
            : [],
        })
      } else {
        // Modo Creación: Pre-cargamos Lunes a Viernes con horario estándar
        const defaultDetalles = [1, 2, 3, 4, 5].map((dia) => ({
          dia: dia,
          horaEntrada: '09:00',
          horaSalida: '18:00',
        }))
        setFormData({
          nombre: '',
          detalles: defaultDetalles,
        })
      }
    }
  }, [initialData, open])

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  // Activa o desactiva un día en el arreglo
  const handleDiaToggle = (diaValue) => {
    setFormData((prev) => {
      const existe = prev.detalles.some((d) => d.dia === diaValue)
      if (existe) {
        // Si ya estaba, lo quitamos
        return {
          ...prev,
          detalles: prev.detalles.filter((d) => d.dia !== diaValue),
        }
      } else {
        // Si no estaba, lo agregamos con un horario estándar por defecto
        return {
          ...prev,
          detalles: [
            ...prev.detalles,
            { dia: diaValue, horaEntrada: '09:00', horaSalida: '18:00' },
          ],
        }
      }
    })
  }

  // Actualiza la hora específica de un día específico
  const handleTimeChange = (diaValue, campo, valor) => {
    setFormData((prev) => ({
      ...prev,
      detalles: prev.detalles.map((d) =>
        d.dia === diaValue ? { ...d, [campo]: valor } : d
      ),
    }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    if (formData.detalles.length === 0) {
      alert('Debes configurar al menos un día para este horario.')
      return
    }

    // Evaluamos si ALGÚN día cruza la medianoche
    const tieneCruceMedianoche = formData.detalles.some(
      (d) => d.horaSalida < d.horaEntrada
    )

    const recordParaBackend = {
      nombre: formData.nombre,
      cruceMedianoche: tieneCruceMedianoche,
      tipoCiclo: 1, // Semanal
      detalles: formData.detalles.map((d) => ({
        dia: d.dia,
        horaEntrada: d.horaEntrada.length === 5 ? d.horaEntrada + ':00' : d.horaEntrada,
        horaSalida: d.horaSalida.length === 5 ? d.horaSalida + ':00' : d.horaSalida,
      })),
    }

    if (initialData && initialData.id) {
      recordParaBackend.id = initialData.id
    }

    onSubmit(recordParaBackend)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {initialData ? 'Editar Horario' : 'Crear Nuevo Horario'}
        </DialogTitle>
        <DialogContent dividers>
          <TextField
            name="nombre"
            label="Nombre del Horario (Ej: Operativo Matutino y Sábados)"
            value={formData.nombre || ''}
            onChange={handleChange}
            fullWidth
            margin="normal"
            required
            sx={{ mb: 3 }}
          />

          <Typography variant="subtitle1" color="primary" fontWeight="bold" gutterBottom>
            Configuración por Día
          </Typography>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
            Selecciona los días que labora el empleado y ajusta el horario de cada uno.
          </Typography>

          <FormGroup>
            {diasSemana.map((dia) => {
              const detalle = formData.detalles.find((d) => d.dia === dia.value)
              const isChecked = !!detalle

              return (
                <Box
                  key={dia.value}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    mb: 1.5,
                    p: 1,
                    borderRadius: 1,
                    bgcolor: isChecked ? '#f5f5f5' : 'transparent',
                    transition: 'background-color 0.3s',
                  }}
                >
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={isChecked}
                        onChange={() => handleDiaToggle(dia.value)}
                        color="primary"
                      />
                    }
                    label={dia.label}
                    sx={{ width: 130, margin: 0 }}
                  />
                  
                  {isChecked && (
                    <Box sx={{ display: 'flex', gap: 2, flexGrow: 1 }}>
                      <TextField
                        type="time"
                        label="Entrada"
                        size="small"
                        value={detalle.horaEntrada}
                        onChange={(e) => handleTimeChange(dia.value, 'horaEntrada', e.target.value)}
                        required
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                      />
                      <TextField
                        type="time"
                        label="Salida"
                        size="small"
                        value={detalle.horaSalida}
                        onChange={(e) => handleTimeChange(dia.value, 'horaSalida', e.target.value)}
                        required
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                      />
                    </Box>
                  )}
                </Box>
              )
            })}
          </FormGroup>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={onClose} color="inherit">
            Cancelar
          </Button>
          <Button type="submit" variant="contained" color="primary">
            Guardar Horario
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default HorarioForm