import { useEffect, useState, useMemo } from 'react'
import {
  Modal,
  Box,
  Typography,
  TextField,
  FormControlLabel,
  Checkbox,
  Autocomplete,
  Button,
  CircularProgress,
} from '@mui/material'
import { getUsuarios } from '../services/usuarioService'
import { getAreasForSelect } from '../services/areaService'
import debounce from 'lodash.debounce'

const modalStyle = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 700,
  height: 600,
  bgcolor: 'background.paper',
  boxShadow: 24,
  p: 4,
  borderRadius: 2,
}

function ReporteModal({ open, onClose, onGenerate }) {
  const [filters, setFilters] = useState({
    fechaInicio: '',
    fechaFin: '',
    usuarioId: null,
    areaId: null,
    soloRetardos: false,
  })

  const [usuarioOptions, setUsuarioOptions] = useState([])
  const [loadingUsuarios, setLoadingUsuarios] = useState(false)

  const [areaOptions, setAreaOptions] = useState([])
  const [loadingAreas, setLoadingAreas] = useState(false)

  const [loading, setLoading] = useState(false)

  // Cargar la lista de áreas (filtrada por rol en el backend)
  useEffect(() => {
    if (open) {
      setLoadingAreas(true)
      getAreasForSelect()
        .then((response) => setAreaOptions(response.data))
        .catch((error) => console.error('Error al cargar áreas:', error))
        .finally(() => setLoadingAreas(false))
    }
  }, [open])

  // Función para buscar usuarios dinámicamente con debounce
  const fetchUsuarios = useMemo(
    () =>
      debounce(async (searchTerm, callback) => {
        if (searchTerm) {
          try {
            const response = await getUsuarios({
              key: searchTerm,
              page: 0,
              size: 20,
            })
            callback(response.data.content)
          } catch (error) {
            console.error('Error al buscar usuarios:', error)
            callback([])
          }
        } else {
          callback([])
        }
        setLoadingUsuarios(false)
      }, 500),
    []
  )

  const handleFilterChange = (name, value) => {
    setFilters((prev) => ({ ...prev, [name]: value }))
  }

  const handleGenerateClick = async (format) => {
    setLoading(true)
    await onGenerate(filters, format)
    setLoading(false)
    onClose()
  }

  return (
    <Modal open={open} onClose={onClose}>
      <Box sx={modalStyle}>
        <Typography
          variant="h5"
          component="h2"
          sx={{ mb: 3, textAlign: 'center' }}>
          Generar Reporte de Asistencias
        </Typography>

        {/* --- ESTRUCTURA DE FILTROS REESCRITA CON BOX --- */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {/* Renglón 1: Fechas (centradas) */}
          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2 }}>
            <TextField
              name="fechaInicio"
              label="Fecha Inicio"
              type="date"
              InputLabelProps={{ shrink: true }}
              onChange={(e) =>
                handleFilterChange('fechaInicio', e.target.value)
              }
            />
            <TextField
              name="fechaFin"
              label="Fecha Fin"
              type="date"
              InputLabelProps={{ shrink: true }}
              onChange={(e) => handleFilterChange('fechaFin', e.target.value)}
            />
          </Box>

          {/* Renglón 2: Filtro de Usuario */}
          <Box>
            <Autocomplete
              options={usuarioOptions}
              getOptionLabel={(option) => option.nombreCompleto || ''}
              filterOptions={(x) => x}
              onInputChange={(event, newInputValue) => {
                setLoadingUsuarios(true)
                fetchUsuarios(newInputValue, setUsuarioOptions)
              }}
              onChange={(event, value) =>
                handleFilterChange('usuarioId', value ? value.id : null)
              }
              loading={loadingUsuarios}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Buscar y seleccionar un Usuario (opcional)"
                  InputProps={{
                    ...params.InputProps,
                    endAdornment: (
                      <>
                        {loadingUsuarios ? (
                          <CircularProgress color="inherit" size={20} />
                        ) : null}
                        {params.InputProps.endAdornment}
                      </>
                    ),
                  }}
                />
              )}
            />
          </Box>

          {/* Renglón 3: Filtro de Área */}
          <Box>
            <Autocomplete
              options={areaOptions}
              getOptionLabel={(option) => option.nombre || ''}
              onChange={(event, value) =>
                handleFilterChange('areaId', value ? value.id : null)
              }
              loading={loadingAreas}
              renderInput={(params) => (
                <TextField {...params} label="Filtrar por Área (opcional)" />
              )}
            />
          </Box>

          {/* Renglón 4: Checkbox de Retardos */}
          <Box>
            <FormControlLabel
              control={
                <Checkbox
                  checked={filters.soloRetardos}
                  onChange={(e) =>
                    handleFilterChange('soloRetardos', e.target.checked)
                  }
                />
              }
              label="Mostrar solo registros con retardo"
            />
          </Box>
        </Box>

        <Box
          sx={{ mt: 4, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
          <Button variant="text" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="contained"
            color="primary"
            disabled={loading}
            onClick={() => handleGenerateClick('excel')}>
            {loading ? <CircularProgress size={24} /> : 'Generar Excel'}
          </Button>
          <Button
            variant="contained"
            color="secondary"
            disabled={loading}
            onClick={() => handleGenerateClick('pdf')}>
            {loading ? <CircularProgress size={24} /> : 'Generar PDF'}
          </Button>
        </Box>
      </Box>
    </Modal>
  )
}

export default ReporteModal
