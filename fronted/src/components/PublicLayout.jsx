import { AppBar, Box, Container, Toolbar, Typography } from '@mui/material'
import Footer from './Footer'

function PublicLayout({ children }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        // --- Configuración de la Imagen de Fondo ---
        backgroundImage: 'url(/assets/background.jpg)', // Ruta a tu imagen
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }}>
      {/* ========== HEADER ========== */}
      <AppBar position="static" color="primary">
        <Toolbar sx={{ justifyContent: 'center', gap: 2 }}>
          <img
            src="/assets/logo_gris.png"
            alt="Logo"
            style={{ height: '40px', marginRight: '16px' }}
          />
          <Typography variant="h6" component="div">
            PODER JUDICIAL DEL ESTADO DE PUEBLA
          </Typography>
        </Toolbar>
      </AppBar>

      {/* ========== CONTENIDO PRINCIPAL (EL FORMULARIO) ========== */}
      <Container component="main" sx={{ mt: 4, mb: 4 }}>
        {children}
      </Container>

      {/* ========== FOOTER ========== */}
      <Footer />
    </Box>
  )
}

export default PublicLayout
