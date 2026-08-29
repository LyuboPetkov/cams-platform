import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getMyCandidacies } from '../api/candidacies'

const STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  REJECTED: '#ef4444',
}

const TABS = ['ALL', 'SUBMITTED', 'ACCEPTED', 'REJECTED']

function MyCandidacies() {
  const navigate = useNavigate()
  const [candidacies, setCandidacies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeTab, setActiveTab] = useState('ALL')

  useEffect(() => {
    getMyCandidacies()
      .then((response) => setCandidacies(response.data))
      .catch(() => setError('Failed to load your candidacies.'))
      .finally(() => setLoading(false))
  }, [])

  const filteredCandidacies =
    activeTab === 'ALL'
      ? candidacies
      : candidacies.filter((candidacy) => candidacy.status === activeTab)

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">My Candidacies</h1>

        {!loading && !error && candidacies.length > 0 && (
          <div className="flex gap-2 mb-6 border-b border-gray-200">
            {TABS.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-3 py-2 text-sm font-medium border-b-2 transition-colors cursor-pointer ${
                  activeTab === tab
                    ? 'border-blue-600 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-900'
                }`}
              >
                {tab.charAt(0) + tab.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        )}

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && !error && candidacies.length === 0 && (
          <EmptyState
            title="You haven't applied to any listings yet"
            description="Browse open roles and apply to see them here."
            action={
              <Link to="/jobs" className="text-sm text-blue-600 hover:underline">
                Browse Jobs
              </Link>
            }
          />
        )}

        {!loading && !error && candidacies.length > 0 && filteredCandidacies.length === 0 && (
          <EmptyState
            title={`No ${activeTab.toLowerCase()} candidacies`}
            description="Switch tabs to see your other candidacies."
          />
        )}

        {!loading && !error && filteredCandidacies.length > 0 && (
          <div className="space-y-3">
            {filteredCandidacies.map((candidacy) => (
              <Card
                key={candidacy.id}
                className="cursor-pointer hover:border-blue-300 transition-colors"
              >
                <div
                  className="flex items-center justify-between"
                  onClick={() => navigate(`/jobs/${candidacy.jobListingId}`)}
                >
                  <div>
                    <h3 className="text-base font-semibold text-gray-800">
                      {candidacy.jobListingTitle}
                    </h3>
                    <p className="text-sm text-gray-500 mt-1">
                      {candidacy.companyName} · Applied{' '}
                      {new Date(candidacy.appliedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <Badge status={candidacy.status} colors={STATUS_COLORS} />
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default MyCandidacies
