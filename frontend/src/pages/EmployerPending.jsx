import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Static confirmation state only. Real employer dashboard, company setup and
// /employer-requests/me polling are Phase 20's job — see CAMS_Phase19 brief 3.1.
function EmployerPending() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
      <div className="bg-white p-8 rounded-lg border border-gray-200 max-w-md text-center">
        <h1 className="text-xl font-bold text-gray-800 mb-2">
          Your employer account is pending review
        </h1>
        <p className="text-sm text-gray-500 mb-6">
          An admin needs to approve your company before you can post job listings.
          There's nothing else to do right now — check back later.
        </p>
        <button
          onClick={handleLogout}
          className="text-sm text-gray-500 hover:text-red-500 transition-colors cursor-pointer"
        >
          Logout
        </button>
      </div>
    </div>
  )
}

export default EmployerPending
