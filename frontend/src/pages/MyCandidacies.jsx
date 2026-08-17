import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
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

function MyCandidacies() {
  const [candidacies, setCandidacies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    getMyCandidacies()
      .then((response) => setCandidacies(response.data))
      .catch(() => setError('Failed to load your candidacies.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">My Candidacies</h1>

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

        {!loading && !error && candidacies.length > 0 && (
          <div className="space-y-3">
            {candidacies.map((candidacy) => (
              <Card key={candidacy.id}>
                <div className="flex items-center justify-between">
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
