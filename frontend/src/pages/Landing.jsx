import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function Landing() {
  const { user, initializing } = useAuth()

  if (initializing) {
    return null
  }

  if (user) {
    // /dashboard has no role gate, so it's the one target that can never bounce
    // back here — anything other than a confirmed EMPLOYER or still-pending
    // employer lands there rather than risking a redirect loop with
    // /employer-pending's own role gate.
    const goToEmployerPending = user.role === 'EMPLOYER' || user.hasPendingEmployerRequest
    return <Navigate to={goToEmployerPending ? '/employer-pending' : '/dashboard'} replace />
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
      <div className="max-w-xl text-center">
        <h1 className="text-3xl font-bold text-blue-600 mb-4">CAMS</h1>
        <p className="text-lg text-gray-700 mb-2">
          Career Activity Management System
        </p>
        <p className="text-sm text-gray-500 mb-8">
          Track your job search, browse open roles, and get AI-matched to listings
          that fit your profile — all in one place.
        </p>
        <div className="flex items-center justify-center gap-3">
          <Link
            to="/register"
            className="bg-blue-600 text-white px-5 py-2 rounded font-medium text-sm hover:bg-blue-700 transition-colors cursor-pointer"
          >
            Get started
          </Link>
          <Link
            to="/login"
            className="bg-white text-gray-700 border border-gray-300 px-5 py-2 rounded font-medium text-sm hover:bg-gray-50 transition-colors cursor-pointer"
          >
            Sign in
          </Link>
        </div>
      </div>
    </div>
  )
}

export default Landing
