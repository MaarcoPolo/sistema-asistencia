import { AppBar, Box, Container, Toolbar, Typography } from '@mui/material'
import Footer from './Footer'

function PublicLayout({ children }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundImage: 'url(/assets/background.jpg)', // Puedes cambiar esta imagen después por algo más ad hoc
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }}>
      {/* ========== HEADER ========== */}
      <AppBar
        position="static"
        sx={{ backgroundColor: 'white', color: '#707070' }}>
        <Toolbar sx={{ justifyContent: 'center', gap: 2 }}>
          <img
            src="/assets/familias-dif-rosa.png"
            alt="Logo Sistema Estatal DIF"
            style={{ height: '45px', marginRight: '16px' }}
          />
          <Typography variant="h6" component="div" sx={{ fontWeight: 'bold' }}>
            SISTEMA ESTATAL DIF
          </Typography>
        </Toolbar>
      </AppBar>

      {/* ========== CONTENIDO PRINCIPAL ========== */}
      <Container component="main" sx={{ mt: 4, mb: 4 }}>
        {children}
      </Container>

      {/* ========== FOOTER ========== */}
      <Footer />
    </Box>
  )
}

export default PublicLayout
