import { useAuth } from '../context/AuthContext'
import { Navigate } from 'react-router-dom'

function ProtectedRoute({ children, adminOnly = false }) {
  const { authData, isAuthLoading } = useAuth()

  if (isAuthLoading) {
    return <div>Cargando sesión...</div>
  }

  if (!authData) {
    return <Navigate to={adminOnly ? '/login-admin' : '/identificacion'} />
  }

  if (
    adminOnly &&
    authData.user.role !== 'SUPERADMIN' &&
    authData.user.role !== 'ADMIN'
  ) {
    return <Navigate to="/asistencia" />
  }

  return children
}

export default ProtectedRoute
