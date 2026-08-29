import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import JobListingCard from '../components/ui/JobListingCard'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getMyMatches } from '../api/jobListings'

// A 409 here means "profile too empty to match yet", not a real failure - it
// gets its own friendly empty state rather than the generic error banner.
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
    navigate(`/jobs/${listing.id}`, { state: { listing, from: '/matches' } })
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
              <JobListingCard
                key={match.listing.id}
                listing={match.listing}
                matchScore={match.similarityScore}
                onClick={() => openListing(match.listing)}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default CandidateMatches
