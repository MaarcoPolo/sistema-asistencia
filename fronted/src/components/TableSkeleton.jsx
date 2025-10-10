import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Skeleton,
  Paper,
} from '@mui/material'

function TableSkeleton({ columns = 6, rows = 5 }) {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead sx={{ backgroundColor: 'grey.200' }}>
          <TableRow>
            {/* Crea las columnas del encabezado */}
            {Array.from({ length: columns }).map((_, index) => (
              <TableCell key={index}>
                <Skeleton variant="text" sx={{ fontSize: '1rem' }} />
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {/* Crea las filas del cuerpo de la tabla */}
          {Array.from({ length: rows }).map((_, rowIndex) => (
            <TableRow key={rowIndex}>
              {Array.from({ length: columns }).map((_, colIndex) => (
                <TableCell key={colIndex}>
                  <Skeleton variant="text" />
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

export default TableSkeleton
