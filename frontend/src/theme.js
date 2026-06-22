import { createTheme } from '@mui/material/styles'

// ── Sistema visual DIF ────────────────────────────────────────────────────────
// Identidad rosa de marca + neutros carbón para un look cohesivo y moderno.
// Todo lo transversal vive aquí para que TODAS las vistas hereden el mismo
// estilo sin tener que tocarlas una por una.

const ROSA = '#ff4081'
const ROSA_OSCURO = '#c2185b'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: ROSA,
      light: '#ff79b0',
      dark: ROSA_OSCURO,
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#5c6bc0', // Índigo de apoyo
      contrastText: '#ffffff',
    },
    success: { main: '#2e9e6b' },
    warning: { main: '#e8911a' },
    error: { main: '#e04848' },
    info: { main: '#3a8dde' },
    text: {
      primary: '#1f232b', // Negro suave para tipografía principal
      secondary: '#6b7280',
    },
    background: {
      default: '#ffffff', // Lienzo blanco para combinar con la imagen del DIF
      paper: '#ffffff',
    },
    divider: 'rgba(31, 37, 51, 0.10)',
  },

  shape: { borderRadius: 12 },

  typography: {
    fontFamily: 'Roboto, "Segoe UI", Arial, sans-serif',
    h4: { fontWeight: 700, letterSpacing: '-0.5px', fontSize: '1.7rem' },
    h5: { fontWeight: 700, letterSpacing: '-0.3px' },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 500 },
    subtitle2: { fontWeight: 600 },
    button: { fontWeight: 600 },
  },

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: { backgroundColor: '#ffffff' },
      },
    },

    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 10,
          textTransform: 'none',
          fontWeight: 600,
          paddingInline: 18,
          paddingBlock: 8,
        },
        containedPrimary: {
          boxShadow: '0 4px 12px rgba(255, 64, 129, 0.28)',
          '&:hover': { boxShadow: '0 6px 16px rgba(255, 64, 129, 0.36)' },
        },
      },
    },

    // Tarjetas: superficie principal del sistema. Sobre lienzo blanco se apoyan
    // en un borde claro + sombra muy suave para distinguirse sin "flotar".
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: '1px solid rgba(31,37,51,0.10)',
          boxShadow: '0 1px 2px rgba(16,24,40,0.04), 0 6px 18px rgba(16,24,40,0.05)',
        },
      },
    },

    MuiPaper: {
      styleOverrides: {
        rounded: { borderRadius: 16 },
        outlined: { borderColor: 'rgba(31,37,51,0.08)' },
      },
    },

    // Header blanco con borde inferior sutil (no rectángulo plano de color).
    MuiAppBar: {
      defaultProps: { elevation: 0, color: 'default' },
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          color: '#262b36',
          borderBottom: '1px solid rgba(31,37,51,0.08)',
          boxShadow: '0 1px 12px rgba(16,24,40,0.04)',
        },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: { border: 'none' },
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: '#ffffff',
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: ROSA },
        },
      },
    },

    // Tabla más estilizada: encabezado tenue, filas con hover y separadores suaves.
    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: '1px solid rgba(31,37,51,0.06)',
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 700,
          color: '#262b36',
          backgroundColor: '#fdeef4', // rosa muy tenue, integrado con la marca
          borderBottom: '1px solid rgba(31,37,51,0.10)',
        },
        body: { borderBottom: '1px solid rgba(31,37,51,0.05)' },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&:hover': { backgroundColor: 'rgba(255,64,129,0.035)' } },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          marginInline: 10,
        },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 8 },
      },
    },

    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 18 },
      },
    },

    MuiTooltip: {
      styleOverrides: {
        tooltip: { borderRadius: 8, fontSize: '0.75rem' },
      },
    },
  },
})

export default theme
