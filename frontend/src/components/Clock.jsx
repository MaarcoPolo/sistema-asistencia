import { useState, useEffect } from 'react'
import { Typography } from '@mui/material'

function Clock() {
  const [time, setTime] = useState(new Date())

  useEffect(() => {
    // Establecemos un intervalo que actualiza la hora cada segundo
    const timerId = setInterval(() => {
      setTime(new Date())
    }, 1000)

    // Limpiamos el intervalo cuando el componente se desmonta para evitar fugas de memoria
    return () => {
      clearInterval(timerId)
    }
  }, []) // El array vacío asegura que esto solo se ejecute una vez

  return (
    <Typography variant="h4" component="p" sx={{ my: 2 }}>
      {time.toLocaleTimeString('es-MX', { hour12: false })}
    </Typography>
  )
}

export default Clock
