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

const ADMIN_LINKS = [
  { to: '/admin', label: 'Employer Requests' },
]

function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isCandidate = user?.role === 'CANDIDATE'
  const isEmployer = user?.role === 'EMPLOYER'
  const isAdmin = user?.role === 'ADMIN'

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav
      className={`px-6 py-4 flex items-center justify-between ${
        isAdmin ? 'bg-slate-900 text-white' : 'bg-white border-b border-gray-200'
      }`}
    >
      <div className="flex items-center gap-6 flex-wrap">
        <Link to={isAdmin ? '/admin' : '/'} className={`text-lg font-bold ${isAdmin ? 'text-white' : 'text-blue-600'}`}>
          {isAdmin ? 'CAMS Admin' : 'CAMS'}
        </Link>
        {isAdmin && (
          ADMIN_LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `text-sm font-medium transition-colors ${
                  isActive ? 'text-white' : 'text-slate-300 hover:text-white'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))
        )}
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
        {!isCandidate && !isEmployer && !isAdmin && (
          <span className="text-sm text-gray-500">
            Your employer account is pending review
          </span>
        )}
      </div>

      <div className="flex items-center gap-4">
        <span className={`text-sm ${isAdmin ? 'text-slate-300' : 'text-gray-600'}`}>
          {user?.fullName}
        </span>
        <button
          style={{ cursor: 'pointer' }}
          onClick={handleLogout}
          className={`text-sm transition-colors cursor-pointer ${
            isAdmin ? 'text-slate-300 hover:text-red-400' : 'text-gray-500 hover:text-red-500'
          }`}
        >
          Logout
        </button>
      </div>
    </nav>
  )
}

export default Navbar
