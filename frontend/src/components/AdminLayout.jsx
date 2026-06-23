import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  Box,
  CssBaseline,
  Drawer,
  Button,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import LogoutIcon from '@mui/icons-material/Logout'
import ApartmentIcon from '@mui/icons-material/Apartment'
import ScheduleIcon from '@mui/icons-material/Schedule'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import Footer from './Footer'

const drawerWidth = 180

function AdminLayout({ children }) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const handleDrawerToggle = () => setMobileOpen(!mobileOpen)

  // Navega y, en móvil, cierra el drawer tras elegir una opción.
  const handleNavigate = (path) => {
    navigate(path)
    setMobileOpen(false)
  }

  const handleLogout = () => {
    logout()
    navigate('/login-admin')
  }

  const menuItems = [
    { text: 'Asistencias', icon: <DashboardIcon />, path: '/admin/dashboard' },
    { text: 'Usuarios', icon: <PeopleIcon />, path: '/admin/usuarios' },
    { text: 'Áreas', icon: <ApartmentIcon />, path: '/admin/areas' },
    { text: 'Horarios', icon: <ScheduleIcon />, path: '/admin/horarios' },
    { text: 'Justificaciones', icon: <FactCheckIcon />, path: '/admin/justificaciones' },
  ]

  // Contenido del menú lateral: sidebar BLANCO, tipografía negra, activo en rosa.
  const drawerContent = (
    <Box
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: 'background.paper',
        borderRight: '1px solid',
        borderColor: 'divider',
      }}>
      {/* Cabecera con el logo DIF (sobre blanco, se lee perfecto). */}
      <Box sx={{ px: 2.5, py: 2.5 }}>
        <img
          src="/assets/corazon.png"
          alt="Logo DIF"
          style={{ width: '52px', height: '52px', objectFit: 'contain', display: 'block', margin: '0 auto' }}
        />
      </Box>

      <List sx={{ flexGrow: 1, py: 0.5 }}>
        {menuItems.map((item) => {
          const selected = location.pathname === item.path
          return (
            <ListItem key={item.text} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                selected={selected}
                onClick={() => handleNavigate(item.path)}
                sx={{
                  color: selected ? 'primary.main' : 'text.primary',
                  backgroundColor: selected ? 'rgba(255,64,129,0.10)' : 'transparent',
                  '&:hover': {
                    backgroundColor: selected
                      ? 'rgba(255,64,129,0.16)'
                      : 'rgba(0,0,0,0.04)',
                  },
                  '&.Mui-selected, &.Mui-selected:hover': {
                    backgroundColor: 'rgba(255,64,129,0.12)',
                  },
                  // Indicador rosa a la izquierda del ítem activo.
                  borderLeft: '3px solid',
                  borderColor: selected ? 'primary.main' : 'transparent',
                  transition: 'background-color .15s ease',
                }}>
                <ListItemIcon
                  sx={{ color: selected ? 'primary.main' : 'text.secondary', minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.text}
                  primaryTypographyProps={{ fontWeight: selected ? 700 : 500 }}
                />
              </ListItemButton>
            </ListItem>
          )
        })}
      </List>

      <Divider />
      <Box sx={{ p: 2 }}>
        <Button
          fullWidth
          startIcon={<LogoutIcon />}
          onClick={handleLogout}
          color="primary"
          variant="outlined"
          sx={{ justifyContent: 'flex-start' }}>
          Cerrar Sesión
        </Button>
      </Box>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: 'background.default' }}>
      <CssBaseline />

      {/* Botón hamburguesa flotante: SOLO en móvil (en desktop el sidebar es fijo
          y no hay header). Permite abrir el menú sin barra superior. */}
      <IconButton
        aria-label="abrir menú"
        onClick={handleDrawerToggle}
        sx={{
          position: 'fixed',
          top: 12,
          left: 12,
          zIndex: (theme) => theme.zIndex.drawer + 2,
          display: { xs: 'inline-flex', sm: 'none' },
          backgroundColor: 'background.paper',
          boxShadow: 2,
          '&:hover': { backgroundColor: 'background.paper' },
        }}>
        <MenuIcon />
      </IconButton>

      <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}>
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open>
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
          width: { sm: `calc(100% - ${drawerWidth}px)` },
        }}>
        {/* Sin header: el contenido empieza arriba. En móvil dejamos un respiro
            para que el botón flotante no tape el título de la página. */}
        <Box
          component="main"
          sx={{ flexGrow: 1, p: { xs: 2, sm: 3, md: 4 }, pt: { xs: 8, sm: 3, md: 4 } }}>
          {children}
        </Box>
        <Footer />
      </Box>
    </Box>
  )
}

export default AdminLayout
