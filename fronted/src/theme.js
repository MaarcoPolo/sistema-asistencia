import { createTheme } from '@mui/material/styles'

const theme = createTheme({
    palette: {
        // El color principal de la aplicación (headers, botones primarios, etc.)
        primary: {
        main: '#8c92bc', // Azul/Morado principal de tu paleta
        light: '#b1bced', // Tono más claro para fondos o hovers [cite: 14]
        dark: '#6a73a0', // Tono más oscuro para hovers o bordes [cite: 26]
        },
        // El color de acento, para resaltar acciones o elementos importantes
        secondary: {
        main: '#c4f45d', // Verde lima principal
        },
        // Colores para el texto
        text: {
        primary: '#353535', // El color principal para el texto, casi negro
        secondary: '#7a7b7f', // Un gris para texto secundario o descriptivo
        },
        // Color de fondo principal
        background: {
        default: '#ffffff', // Fondo completamente blanco
        },
    },
    typography: {
        // Opcional: define una fuente para toda la aplicación si quieres
        fontFamily: 'Roboto, Arial, sans-serif',
    },
})

export default theme
