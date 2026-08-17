import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getMyMatches } from '../api/jobListings'

// The 409 NoEmbeddingAvailableException means "profile too empty to match yet",
// not a real failure — this is the first place in the app the embedding-based
// matching the thesis is about becomes visible, so it needs its own friendly
// empty state rather than falling into the generic error banner.
function CandidateMatches() {
  const navigate = useNavigate()
  const [matches, setMatches] = useState(null)
  const [incompleteProfile, setIncompleteProfile] = useState(false)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getMyMatches()
      .then((response) => setMatches(response.data))
      .catch((err) => {
        if (err.response?.status === 409) {
          setIncompleteProfile(true)
        } else {
          setError('Failed to load your matches.')
        }
      })
      .finally(() => setLoading(false))
  }, [])

  function openListing(listing) {
    navigate(`/jobs/${listing.id}`, { state: { listing } })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">Matches</h1>
        <p className="text-sm text-gray-500 mb-6">
          Listings ranked by how closely they match your profile.
        </p>

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && incompleteProfile && (
          <EmptyState
            title="Complete your profile to see matches"
            description="Add a headline, description, or a few skills so we can match you to listings."
            action={
              <Link to="/profile" className="text-sm text-blue-600 hover:underline">
                Complete your profile
              </Link>
            }
          />
        )}

        {!loading && !error && !incompleteProfile && matches?.length === 0 && (
          <EmptyState
            title="No matches yet"
            description="Check back once more listings are posted."
          />
        )}

        {!loading && !error && matches?.length > 0 && (
          <div className="space-y-3">
            {matches.map((match) => (
              <Card
                key={match.listing.id}
                className="cursor-pointer hover:border-blue-300 transition-colors"
              >
                <div onClick={() => openListing(match.listing)}>
                  <div className="flex items-center justify-between">
                    <h3 className="text-base font-semibold text-gray-800">
                      {match.listing.title}
                    </h3>
                    <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">
                      {Math.round(match.similarityScore * 100)}% match
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    {match.listing.company?.name} · {match.listing.location || 'No location listed'}
                    {match.listing.remote ? ' · Remote' : ''}
                  </p>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default CandidateMatches
