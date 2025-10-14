import {
  Modal,
  Box,
  Typography,
  Card,
  CardContent,
  CardMedia,
  Grid,
  Button,
  Divider,
} from '@mui/material'

// Un componente simple para mostrar una imagen o un placeholder
const PhotoDisplay = ({ photo, label }) => (
  <Box sx={{ textAlign: 'center' }}>
    <Typography variant="overline">{label}</Typography>
    <CardMedia
      component="img"
      sx={{
        width: 150,
        height: 150,
        objectFit: 'cover',
        borderRadius: '4px',
        border: '1px solid #ddd',
        backgroundColor: '#f5f5f5',
        margin: 'auto',
      }}
      image={photo || 'https://via.placeholder.com/150?text=Sin+Foto'}
      alt={`Foto de ${label.toLowerCase()}`}
    />
  </Box>
)

function AsistenciaViewModal({ open, onClose, record }) {
  if (!record) {
    return null
  }

  // Función para formatear la hora
  const formatTime = (dateTimeString) => {
    if (!dateTimeString) return '---'
    return new Date(dateTimeString).toLocaleTimeString('es-MX', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }

  return (
    <Modal open={open} onClose={onClose}>
      <Box
        sx={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 500, // Ancho del modal
          bgcolor: 'background.paper',
          boxShadow: 24,
          p: 0, // Quitamos el padding para que la Card ocupe todo
        }}>
        <Card>
          <CardContent sx={{ textAlign: 'center', p: 3 }}>
            <Typography variant="h5" component="div" gutterBottom>
              {record.usuarioNombreCompleto}
            </Typography>
            <Typography sx={{ mb: 2 }} color="text.secondary">
              {record.areaNombre}
            </Typography>
            <Typography variant="body1" sx={{ mb: 3 }}>
              Fecha: <strong>{record.fecha}</strong>
            </Typography>

            <Grid container spacing={2} justifyContent="center">
              <Grid item xs={6}>
                <PhotoDisplay photo={record.fotoEntrada} label="Entrada" />
                <Typography variant="h6" sx={{ mt: 1 }}>
                  {formatTime(record.horaEntrada)}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <PhotoDisplay photo={record.fotoSalida} label="Salida" />
                <Typography variant="h6" sx={{ mt: 1 }}>
                  {formatTime(record.horaSalida)}
                </Typography>
              </Grid>
            </Grid>

            <Divider sx={{ my: 3 }} />

            <Button variant="outlined" onClick={onClose}>
              Cerrar
            </Button>
          </CardContent>
        </Card>
      </Box>
    </Modal>
  )
}

export default AsistenciaViewModal
