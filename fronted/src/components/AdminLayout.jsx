import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import {
    AppBar,
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
    Toolbar,
    Typography,
} from '@mui/material'

import MenuIcon from '@mui/icons-material/Menu'
import DashboardIcon from '@mui/icons-material/Dashboard'
import PeopleIcon from '@mui/icons-material/People'
import LogoutIcon from '@mui/icons-material/Logout'
import AccountCircle from '@mui/icons-material/AccountCircle'
import ApartmentIcon from '@mui/icons-material/Apartment'
import ScheduleIcon from '@mui/icons-material/Schedule'

import Footer from './Footer' 

const drawerWidth = 240

function AdminLayout({ children }) {
    const [mobileOpen, setMobileOpen] = useState(false)
    const { logout } = useAuth()
    const navigate = useNavigate()

    const handleDrawerToggle = () => {
        setMobileOpen(!mobileOpen)
    }

    const handleLogout = () => {
        logout()
        navigate('/login-admin')
    }

    // Definimos las opciones del menú
    const menuItems = [
      { text: 'Asistencias', icon: <DashboardIcon />, path: '/admin/dashboard' },
      { text: 'Usuarios', icon: <PeopleIcon />, path: '/admin/usuarios' },
      { text: 'Áreas', icon: <ApartmentIcon />, path: '/admin/areas' },
      { text: 'Horarios', icon: <ScheduleIcon />, path: '/admin/horarios' },
    ]

    const drawerContent = (
        <div>
        <Toolbar
            sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            px: [1],
            }}>
            <img
            src="/assets/logo_gris.png"
            alt="Logo"
            style={{
                height: '50px',
                margin: 'auto',
            }}
            />
        </Toolbar>
        <List>
            {menuItems.map((item) => (
            <ListItem key={item.text} disablePadding>
                <ListItemButton onClick={() => navigate(item.path)}>
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.text} />
                </ListItemButton>
            </ListItem>
            ))}
        </List>
        </div>
    )

    return (
        <Box
            sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
            <CssBaseline />
            <AppBar
            position="fixed"
            sx={{
                width: { sm: `calc(100% - ${drawerWidth}px)` },
                ml: { sm: `${drawerWidth}px` },
            }}>
            <Toolbar>
                <IconButton
                color="inherit"
                aria-label="open drawer"
                edge="start"
                onClick={handleDrawerToggle}
                sx={{ mr: 2, display: { sm: 'none' } }}>
                <MenuIcon />
                </IconButton>
                <Typography
                variant="h6"
                noWrap
                component="div"
                sx={{ flexGrow: 1 }}>
                Sistema de Asistencia
                </Typography>
                <Button
                color="inherit"
                startIcon={<LogoutIcon />}
                onClick={handleLogout}>
                Cerrar Sesión
                </Button>
            </Toolbar>
            </AppBar>
            <Box
            component="nav"
            sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
            <Drawer
                variant="temporary"
                open={mobileOpen}
                onClose={handleDrawerToggle}
                ModalProps={{ keepMounted: true }}
                sx={{
                display: { xs: 'block', sm: 'none' },
                '& .MuiDrawer-paper': {
                    boxSizing: 'border-box',
                    width: drawerWidth,
                },
                }}>
                {drawerContent}
            </Drawer>
            <Drawer
                variant="permanent"
                sx={{
                display: { xs: 'none', sm: 'block' },
                '& .MuiDrawer-paper': {
                    boxSizing: 'border-box',
                    width: drawerWidth,
                },
                }}
                open>
                {drawerContent}
            </Drawer>
            </Box>

            <Box
            component="main"
            sx={{
                flexGrow: 1,
                p: 3,
                width: { sm: `calc(100% - ${drawerWidth}px)` },
                ml: { sm: `${drawerWidth}px` },
            }}>
            <Toolbar />{' '}
            {children}{' '}
            </Box>
            <Footer />
        </Box>
    )
}

export default AdminLayout
