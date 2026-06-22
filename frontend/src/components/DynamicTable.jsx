import { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TableSortLabel,
  TextField,
  InputAdornment,
  TablePagination,
  Card,
  CardContent,
  Stack,
  Typography,
  Divider,
  useMediaQuery,
} from '@mui/material'
import { useTheme } from '@mui/material/styles'
import SearchIcon from '@mui/icons-material/Search'
import TableSkeleton from './TableSkeleton'

function DynamicTable({
  columns,
  fetchDataFunction,
  renderActions,
  initialSort,
  extraFilters = {},
}) {
  // En móvil/tablet la tabla se reemplaza por tarjetas apiladas para evitar el
  // scroll horizontal y mejorar la lectura (responsividad).
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const [data, setData] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)

  // Estados para la paginación, ordenamiento y búsqueda
  const [page, setPage] = useState(0)
  const [rowsPerPage, setRowsPerPage] = useState(10)
  const [sort, setSort] = useState(
    initialSort || { field: 'id', direction: 'asc' }
  )

  const [searchTerm, setSearchTerm] = useState('') // Lo que el usuario escribe
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('') // Lo que se envía
  const extraFiltersStr = JSON.stringify(extraFilters)

  useEffect(() => {
    const timerId = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm)
      setPage(0) // Regresar a la primera página con cada nueva búsqueda
    }, 500)

    return () => {
      clearTimeout(timerId)
    }
  }, [searchTerm])

  // Función para cargar los datos, que se llamará cada vez que algo cambie
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const sortColumn = columns.find((c) => c.id === sort.field)
      const sortField = sortColumn?.sortId || sort.field

      const params = {
        page,
        size: rowsPerPage,
        sort: `${sortField},${sort.direction}`,
        key: debouncedSearchTerm,
        ...JSON.parse(extraFiltersStr)
      }

      if (!params.key) delete params.key

      // Quitar filtros extra vacíos (string vacío, null o undefined)
      // para no ensuciar la query con parámetros sin valor.
      Object.keys(params).forEach((k) => {
        if (params[k] === '' || params[k] === null || params[k] === undefined) {
          delete params[k]
        }
      })

      const response = await fetchDataFunction(params)
      const pageData = response.data?.data ?? response.data
      setData(pageData.content || [])
      setTotalElements(pageData.totalElements || 0)
    } catch (error) {
      console.error('Error al cargar datos para la tabla:', error)
      setData([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [page, rowsPerPage, sort, debouncedSearchTerm, extraFiltersStr, fetchDataFunction, columns])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Al cambiar los filtros externos (No. Control, Área), regresar a la
  // primera página para no quedar en una página inexistente del nuevo resultado.
  useEffect(() => {
    setPage(0)
  }, [extraFiltersStr])

  const handleSort = (field) => {
    const isAsc = sort.field === field && sort.direction === 'asc'
    setSort({ field, direction: isAsc ? 'desc' : 'asc' })
  }

  const handleChangePage = (event, newPage) => {
    setPage(newPage)
  }

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(parseInt(event.target.value, 10))
    setPage(0)
  }

  // --- Vista de tarjetas para móvil/tablet ---
  const renderTarjetas = () => {
    if (data.length === 0) {
      return (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            No hay registros para mostrar.
          </Typography>
        </Paper>
      )
    }

    return (
      <Stack spacing={1.5}>
        {data.map((row) => {
          const id = row.id || row.idAsistencia
          return (
            <Card key={id} variant="outlined">
              <CardContent sx={{ '&:last-child': { pb: 2 } }}>
                <Stack spacing={1}>
                  {columns.map((col) => (
                    <Box
                      key={`${id}-${col.id}`}
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        gap: 2,
                      }}>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontWeight: 600, flexShrink: 0 }}>
                        {col.label}
                      </Typography>
                      <Box sx={{ textAlign: 'right', minWidth: 0 }}>
                        {col.render ? col.render(row) : row[col.id]}
                      </Box>
                    </Box>
                  ))}
                  {renderActions && (
                    <>
                      <Divider sx={{ my: 0.5 }} />
                      <Box
                        sx={{
                          display: 'flex',
                          justifyContent: 'flex-end',
                          flexWrap: 'wrap',
                        }}>
                        {renderActions(row)}
                      </Box>
                    </>
                  )}
                </Stack>
              </CardContent>
            </Card>
          )
        })}
      </Stack>
    )
  }

  // --- Vista de tabla para escritorio ---
  const renderTabla = () => (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            {columns.map((col) => (
              <TableCell
                key={col.id}
                sortDirection={sort.field === col.id ? sort.direction : false}>
                {col.sortable !== false ? (
                  <TableSortLabel
                    active={sort.field === col.id}
                    direction={sort.field === col.id ? sort.direction : 'asc'}
                    onClick={() => handleSort(col.id)}
                    sx={{ fontWeight: 'bold' }}>
                    {col.label}
                  </TableSortLabel>
                ) : (
                  <Box sx={{ fontWeight: 'bold' }}>{col.label}</Box>
                )}
              </TableCell>
            ))}
            {renderActions && (
              <TableCell sx={{ fontWeight: 'bold' }}>Acciones</TableCell>
            )}
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((row) => (
            <TableRow key={row.id || row.idAsistencia} hover>
              {columns.map((col) => (
                <TableCell key={`${row.id || row.idAsistencia}-${col.id}`}>
                  {col.render ? col.render(row) : row[col.id]}
                </TableCell>
              ))}
              {renderActions && <TableCell>{renderActions(row)}</TableCell>}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: { xs: 'stretch', sm: 'flex-end' } }}>
        <TextField
          variant="outlined"
          size="small"
          placeholder="Buscar..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          fullWidth
          sx={{ maxWidth: { sm: 280 } }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
      </Box>
      {loading ? (
        <TableSkeleton columns={columns.length + (renderActions ? 1 : 0)} />
      ) : (
        <>
          {isMobile ? renderTarjetas() : renderTabla()}
          <TablePagination
            rowsPerPageOptions={[5, 10, 25]}
            component="div"
            count={totalElements}
            rowsPerPage={rowsPerPage}
            page={page}
            onPageChange={handleChangePage}
            onRowsPerPageChange={handleChangeRowsPerPage}
            labelRowsPerPage={isMobile ? 'Filas:' : 'Filas por página:'}
            sx={isMobile ? { '.MuiTablePagination-toolbar': { pl: 1 } } : undefined}
          />
        </>
      )}
    </Box>
  )
}

export default DynamicTable