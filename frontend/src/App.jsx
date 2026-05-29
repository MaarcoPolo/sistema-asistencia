import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import AdminLayout from './components/AdminLayout'
import ProtectedRoute from './components/ProtectedRoute'

import LoginAdmin from './pages/LoginAdmin'
import AdminDashboard from './pages/AdminDashboard'
import AdminUsuarios from './pages/AdminUsuarios'
import AdminAreas from './pages/AdminAreas'
import AdminHorarios from './pages/AdminHorarios'
import IdentificacionUsuario from './pages/IdentificacionUsuario'
import PaginaAsistencia from './pages/PaginaAsistencia'
import PublicLayout from './components/PublicLayout'

function App() {
  const { authData, isAuthLoading } = useAuth()

  if (isAuthLoading) {
    return <div>Cargando...</div>
  }

  return (
    <Routes>
      {/* Rutas Públicas de Login */}
      <Route
        path="/login-admin"
        element={
          !authData ? (
            <PublicLayout>
              <LoginAdmin />
            </PublicLayout>
          ) : (
            <Navigate to="/admin/dashboard" />
          )
        }
      />
      <Route
        path="/identificacion"
        element={
          !authData ? (
            <PublicLayout>
              <IdentificacionUsuario />
            </PublicLayout>
          ) : (
            <Navigate
              to={
                authData.user.role === 'USER'
                  ? '/asistencia'
                  : '/admin/dashboard'
              }
            />
          )
        }
      />

      {/* Ruta de Asistencia para el rol USER */}
      <Route
        path="/asistencia"
        element={
          <ProtectedRoute>
            <PaginaAsistencia />
          </ProtectedRoute>
        }
      />

      {/* Rutas Protegidas para Administradores */}
      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminLayout>
              <AdminDashboard />
            </AdminLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/usuarios"
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminLayout>
              <AdminUsuarios />
            </AdminLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/areas"
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminLayout>
              <AdminAreas />
            </AdminLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/horarios"
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminLayout>
              <AdminHorarios />
            </AdminLayout>
          </ProtectedRoute>
        }
      />

      {/* Redirección por defecto */}
      <Route
        path="*"
        element={
          <Navigate
            to={
              !authData
                ? '/identificacion'
                : authData.user.role === 'USER'
                ? '/asistencia'
                : '/admin/dashboard'
            }
          />
        }
      />
    </Routes>
  )
}

export default App
