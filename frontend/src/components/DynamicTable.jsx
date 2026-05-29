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
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import TableSkeleton from './TableSkeleton'

function DynamicTable({
  columns,
  fetchDataFunction,
  renderActions,
  initialSort,
}) {
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
      }

      if (!params.key) delete params.key

      const response = await fetchDataFunction(params)
      const pageData = response.data
      setData(pageData.content || [])
      setTotalElements(pageData.totalElements || 0)
    } catch (error) {
      console.error('Error al cargar datos para la tabla:', error)
      setData([])
      setTotalElements(0)
    } finally {
      setLoading(false)
    }
  }, [page, rowsPerPage, sort, debouncedSearchTerm, fetchDataFunction, columns])

  useEffect(() => {
    loadData()
  }, [loadData])

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

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
        <TextField
          variant="outlined"
          size="small"
          placeholder="Buscar..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
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
        <TableContainer component={Paper}>
          <Table>
            <TableHead sx={{ backgroundColor: 'primary.light' }}>
              <TableRow>
                {columns.map((col) => (
                  <TableCell
                    key={col.id}
                    sortDirection={
                      sort.field === col.id ? sort.direction : false
                    }>
                    {col.sortable !== false ? (
                      <TableSortLabel
                        active={sort.field === col.id}
                        direction={
                          sort.field === col.id ? sort.direction : 'asc'
                        }
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
          <TablePagination
            rowsPerPageOptions={[5, 10, 25]}
            component="div"
            count={totalElements}
            rowsPerPage={rowsPerPage}
            page={page}
            onPageChange={handleChangePage}
            onRowsPerPageChange={handleChangeRowsPerPage}
          />
        </TableContainer>
      )}
    </Box>
  )
}

export default DynamicTable