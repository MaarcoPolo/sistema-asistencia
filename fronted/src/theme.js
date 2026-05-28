import { createTheme } from '@mui/material/styles'

const theme = createTheme({
    palette: {
        primary: {
            main: '#ff4081', // --rosa-500: Color principal de marca
            light: 'rgb(255,165,197)', // --color-principal: Rosa suave
            dark: '#ff2d6f', // --rosa-accent: Acento vibrante para hover y botones
        },
        secondary: {
            main: '#707070', // --color-secundario: Gris secundario
        },
        text: {
            primary: '#353535', // Casi negro para buena legibilidad
            secondary: '#707070', // El gris secundario para textos descriptivos
        },
        background: {
            default: '#f8f9fa', // Un fondo un poco más limpio y claro
        },
    },
    typography: {
        fontFamily: 'Roboto, Arial, sans-serif',
    },
    components: {
        // Redondeamos un poco los botones para un look más amable
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: '8px',
                    textTransform: 'none',
                    fontWeight: 'bold',
                },
            },
        },
        // Hacemos que los bordes de los inputs brillen con el color principal al hacer hover
        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                        borderColor: '#ff4081',
                    },
                },
            },
        },
    },
})

export default theme