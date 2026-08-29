import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { resubmitEmployerRequest } from '../api/employerRequests'

// Unauthenticated - a REJECTED account can't log in, so this re-identifies the
// applicant with email/password. Reachable from Login.jsx's rejected-account state.
function ResubmitEmployerRequest() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [companyDescription, setCompanyDescription] = useState('')
  const [companyWebsite, setCompanyWebsite] = useState('')
  const [companyLocation, setCompanyLocation] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      await resubmitEmployerRequest({
        email,
        password,
        companyName,
        companyDescription,
        companyWebsite,
        companyLocation,
      })
      navigate('/employer-pending')
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Invalid email or password.')
      } else if (err.response?.status === 404) {
        setError('No account found with this email.')
      } else if (err.response?.status === 409) {
        setError(err.response.data?.message || 'This request cannot be resubmitted.')
      } else if (err.response?.status === 400) {
        setError(err.response.data?.message || 'Please check your inputs and try again.')
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-10">
      <div className="bg-white p-8 rounded-lg shadow w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Resubmit your employer application</h1>
        <p className="text-sm text-gray-500 mb-6">
          Sign in with your rejected account's email and password to submit a new request.
        </p>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded mb-4 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="space-y-4 border-t border-gray-200 pt-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Company Name</label>
              <input
                type="text"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Company Description</label>
              <textarea
                value={companyDescription}
                onChange={(e) => setCompanyDescription(e.target.value)}
                rows={3}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Company Website</label>
              <input
                type="url"
                value={companyWebsite}
                onChange={(e) => setCompanyWebsite(e.target.value)}
                placeholder="https://"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Company Location</label>
              <input
                type="text"
                value={companyLocation}
                onChange={(e) => setCompanyLocation(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors cursor-pointer"
          >
            {loading ? 'Submitting...' : 'Resubmit application'}
          </button>
        </form>

        <p className="text-sm text-gray-500 mt-6 text-center">
          <Link to="/login" className="text-blue-600 hover:underline cursor-pointer">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  )
}

export default ResubmitEmployerRequest
