import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import axiosInstance from '../api/axiosInstance'

function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [showResubmitLink, setShowResubmitLink] = useState(false)
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setShowResubmitLink(false)
    setLoading(true)

    try {
      const response = await axiosInstance.post('/api/auth/login', {
        email,
        password,
      })
      login(response.data)
      // A successful login is always ACTIVE, so role is an approved EMPLOYER,
      // a CANDIDATE, or ADMIN.
      if (response.data.role === 'ADMIN') {
        navigate('/admin')
      } else if (response.data.role === 'EMPLOYER') {
        navigate('/listings')
      } else {
        navigate('/jobs')
      }
    } catch (err) {
      if (err.response?.status === 403) {
        // PENDING_APPROVAL/REJECTED accounts - show the backend's own message;
        // only a REJECTED account can resubmit, and its message says so.
        const message = err.response.data?.message || 'This account cannot sign in right now.'
        setError(message)
        setShowResubmitLink(message.toLowerCase().includes('resubmit'))
      } else {
        setError('Invalid email or password.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Sign in to CAMS</h1>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded mb-4 text-sm">
            <p>{error}</p>
            {showResubmitLink && (
              <Link to="/resubmit-employer-request" className="text-blue-600 hover:underline cursor-pointer">
                Resubmit your employer application
              </Link>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors cursor-pointer"
          >
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p className="text-sm text-gray-500 mt-6 text-center">
          Don't have an account?{' '}
          <Link to="/register" className="text-blue-600 hover:underline cursor-pointer">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}

export default Login