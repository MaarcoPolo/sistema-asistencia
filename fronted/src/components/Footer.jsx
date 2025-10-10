import { Box, Typography } from '@mui/material'

function Footer() {
    return (
        <Box
            component="footer"
            sx={{
            py: 2,
            px: 2,
            mt: 'auto',
            orderTop: '1px solid',
            borderColor: 'divider',
            textAlign: 'center',
            }}>
            <Typography variant="body2" color="text.secondary">
            © {new Date().getFullYear()} Poder Judicial del Estado de Puebla
            </Typography>
        </Box>
    )
}

export default Footer
