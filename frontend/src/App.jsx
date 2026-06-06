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
import AdminJustificaciones from './pages/AdminJustificaciones'
import MisAsistencias from './pages/MisAsistencias'

function App() {
  const { authData, isAuthLoading } = useAuth()

  if (isAuthLoading) {
    return <div>Cargando...</div>
  }

  return (
    <Routes>
      {/* ---------------------------------------------------- */}
      {/* RUTAS PÚBLICAS DE LOGIN                              */}
      {/* ---------------------------------------------------- */}
      
      {/* Login con Contraseña (Portal Completo) */}
      <Route
        path="/login-admin"
        element={
          !authData ? (
            <PublicLayout>
              <LoginAdmin />
            </PublicLayout>
          ) : (
            <Navigate to={authData.user.role === 'USER' ? '/mis-asistencias' : '/admin/dashboard'} />
          )
        }
      />
      
      {/* Login Rápido / Kiosco (Solo para checar) */}
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

      {/* ---------------------------------------------------- */}
      {/* RUTAS PROTEGIDAS PARA EL EMPLEADO (ROL: USER)        */}
      {/* ---------------------------------------------------- */}
      
      {/* 1. Vista de Cámara (Tomar Asistencia) */}
      <Route
        path="/asistencia"
        element={
          <ProtectedRoute>
            <PaginaAsistencia />
          </ProtectedRoute>
        }
      />

      {/* 2. NUEVA: Vista de Historial, Semáforo y Justificaciones */}
      <Route
        path="/mis-asistencias"
        element={
          <ProtectedRoute>
            <MisAsistencias />
          </ProtectedRoute>
        }
      />

      {/* ---------------------------------------------------- */}
      {/* RUTAS PROTEGIDAS PARA ADMINISTRADORES                */}
      {/* ---------------------------------------------------- */}
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
      <Route
        path="/admin/justificaciones"
        element={
          <ProtectedRoute adminOnly={true}>
            <AdminLayout>
              <AdminJustificaciones />
            </AdminLayout>
          </ProtectedRoute>
        }
      />

      {/* Redirección por defecto en caso de rutas no encontradas */}
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