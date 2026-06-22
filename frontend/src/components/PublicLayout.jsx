import { AppBar, Box, Container, Toolbar, Typography } from '@mui/material'
import Footer from './Footer'

function PublicLayout({ children }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        // Fondo blanco coherente con la imagen de marca del DIF.
        backgroundColor: 'background.default',
      }}>
      {/* ========== HEADER ========== */}
      <AppBar position="static" elevation={0}>
        <Toolbar sx={{ justifyContent: 'center', gap: 2 }}>
          <picture>
            <source srcSet="/assets/familias-dif-rosa.webp" type="image/webp" />
            <img
              src="/assets/familias-dif-rosa.png"
              alt="Logo Sistema Estatal DIF"
              style={{ height: '42px', marginRight: '16px', display: 'block' }}
            />
          </picture>
          <Typography
            variant="h6"
            component="div"
            sx={{ fontWeight: 700, letterSpacing: 0.5, color: 'text.primary' }}>
            SISTEMA ESTATAL DIF
          </Typography>
        </Toolbar>
      </AppBar>

      {/* ========== CONTENIDO PRINCIPAL ========== */}
      <Container
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          py: { xs: 3, sm: 5 },
        }}>
        {children}
      </Container>

      {/* ========== FOOTER ========== */}
      <Footer />
    </Box>
  )
}

export default PublicLayout
