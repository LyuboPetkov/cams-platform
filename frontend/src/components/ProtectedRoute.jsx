import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function ProtectedRoute({ children, role }) {
  const { user, initializing } = useAuth()

  if (initializing) {
    return null
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (role && user.role !== role) {
    return <Navigate to="/" replace />
  }

  return children
}
export default ProtectedRoute
