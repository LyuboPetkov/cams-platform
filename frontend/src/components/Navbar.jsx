import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const CANDIDATE_LINKS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/applications', label: 'My Applications' },
  { to: '/jobs', label: 'Browse Jobs' },
  { to: '/candidacies', label: 'My Candidacies' },
  { to: '/matches', label: 'Matches' },
  { to: '/profile', label: 'Profile' },
]

const EMPLOYER_LINKS = [
  { to: '/company', label: 'Company' },
  { to: '/listings', label: 'My Listings' },
]

function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isCandidate = user?.role === 'CANDIDATE'
  const isEmployer = user?.role === 'EMPLOYER'

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-6 flex-wrap">
        <Link to="/" className="text-lg font-bold text-blue-600">
          CAMS
        </Link>
        {isCandidate && (
          CANDIDATE_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `text-sm font-medium transition-colors ${
                  isActive ? 'text-blue-600' : 'text-gray-500 hover:text-gray-900'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))
        )}
        {isEmployer && (
          EMPLOYER_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `text-sm font-medium transition-colors ${
                  isActive ? 'text-blue-600' : 'text-gray-500 hover:text-gray-900'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))
        )}
        {!isCandidate && !isEmployer && (
          <span className="text-sm text-gray-500">
            Your employer account is pending review
          </span>
        )}
      </div>

      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-600">
          {user?.fullName}
        </span>
        <button
          style={{ cursor: 'pointer' }}
          onClick={handleLogout}
          className="text-sm text-gray-500 hover:text-red-500 transition-colors cursor-pointer"
        >
          Logout
        </button>
      </div>
    </nav>
  )
}

export default Navbar
