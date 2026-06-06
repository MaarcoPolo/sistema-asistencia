import { useState, useEffect } from 'react'
import { Box, Typography, Card, CardContent, Container, Chip, Tooltip, IconButton, Button, Alert, AlertTitle } from '@mui/material'
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import LogoutIcon from '@mui/icons-material/Logout'

import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import { getMisAsistencias } from '../services/asistenciaService'
import { getMiPerfil } from '../services/usuarioService'
import DynamicTable from '../components/DynamicTable'
import JustificarModal from '../components/JustificarModal'
import CambiarContrasenaModal from '../components/CambiarContrasenaModal'

function MisAsistencias() {
  const { authData, logout } = useAuth()
  const navigate = useNavigate()
  const [tableKey, setTableKey] = useState(0)
  
  const [perfil, setPerfil] = useState(null)
  const [justificarModalOpen, setJustificarModalOpen] = useState(false)
  const [recordToJustify, setRecordToJustify] = useState(null)
  const [passwordModalOpen, setPasswordModalOpen] = useState(false)

  useEffect(() => {
    cargarPerfil()
  }, [])

  const cargarPerfil = async () => {
    try {
      const data = await getMiPerfil()
      setPerfil(data)
    } catch (error) {
      console.error("Error al cargar el perfil del usuario", error)
    }
  }

  const columns = [
    { id: 'fecha', label: 'Fecha' },
    {
      id: 'horaEntrada',
      label: 'Entrada',
      render: (row) => row.horaEntrada ? new Date(row.horaEntrada).toLocaleTimeString() : '---',
    },
    {
      id: 'horaSalida',
      label: 'Salida',
      render: (row) => row.horaSalida ? new Date(row.horaSalida).toLocaleTimeString() : '---',
    },
    {
      id: 'semaforo',
      label: 'Estado',
      render: (row) => {
        const isOk = row.estatusIncidencia === 0
        return (
          <Chip 
            icon={isOk ? <CheckCircleIcon /> : <CancelIcon />}
            label={isOk ? "Correcto" : "Incidencia"} 
            color={isOk ? "success" : "error"} 
            variant="filled"
          />
        )
      }
    },
    {
      id: 'detalle',
      label: 'Detalle / Justificación',
      render: (row) => {
        if (row.estatusIncidencia === 0) return <Typography variant="body2" color="textSecondary">Sin novedad</Typography>
        
        if (row.motivoJustificacion) {
          return <Chip label={`Justificado: ${row.motivoJustificacion}`} color="primary" variant="outlined" size="small" />
        }
        
        let mensaje = ""
        if (row.estatusIncidencia === 1) mensaje = "Retardo"
        else if (row.estatusIncidencia === 2) mensaje = "Falta Total"
        else if (row.estatusIncidencia === 3) mensaje = "Falta checar entrada"
        else if (row.estatusIncidencia === 4) mensaje = "Falta checar salida"

        return <Typography variant="body2" color="error" fontWeight="bold">{mensaje}</Typography>
      }
    }
  ]

  const handleOpenJustificar = (record) => {
    setRecordToJustify(record)
    setJustificarModalOpen(true)
  }

  const handleLogout = () => {
    logout()
    navigate('/login-admin')
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1" fontWeight="bold">
          Mi Historial de Asistencias
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" color="primary" onClick={() => setPasswordModalOpen(true)}>
            Cambiar Contraseña
          </Button>
          <Button variant="outlined" color="error" startIcon={<LogoutIcon />} onClick={handleLogout}>
            Cerrar Sesión
          </Button>
        </Box>
      </Box>

      {/* ALERTA DE SEGURIDAD (Solo se muestra si requiereCambioPassword es true) */}
      {perfil?.requiereCambioPassword && (
        <Alert severity="warning" sx={{ mb: 3, borderRadius: 2 }}>
          <AlertTitle><strong>¡Atención, {perfil.nombre}!</strong></AlertTitle>
          Estás utilizando la contraseña por defecto del sistema. Por seguridad, es obligatorio que la cambies para proteger tu información.
          <Button color="inherit" size="small" sx={{ ml: 2, textDecoration: 'underline' }} onClick={() => setPasswordModalOpen(true)}>
            Cambiar ahora
          </Button>
        </Alert>
      )}

      <Card elevation={3}>
        <CardContent>
          <DynamicTable
            key={tableKey}
            columns={columns}
            fetchDataFunction={getMisAsistencias}
            renderActions={(row) => (
              <>
                {row.estatusIncidencia !== 0 && !row.motivoJustificacion && (
                  <Tooltip title="Justificar esta incidencia">
                    <IconButton color="primary" onClick={() => handleOpenJustificar(row)}>
                      <VerifiedUserIcon fontSize="large" />
                    </IconButton>
                  </Tooltip>
                )}
              </>
            )}
          />
        </CardContent>
      </Card>

      <JustificarModal
        open={justificarModalOpen}
        onClose={() => setJustificarModalOpen(false)}
        record={recordToJustify}
        onSuccess={() => setTableKey((prev) => prev + 1)}
      />

      <CambiarContrasenaModal
        open={passwordModalOpen}
        onClose={() => setPasswordModalOpen(false)}
        onSuccess={cargarPerfil} // Quita la alerta roja automáticamente
      />
    </Container>
  )
}

export default MisAsistencias