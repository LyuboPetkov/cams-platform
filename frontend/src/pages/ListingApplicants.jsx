import { useState, useEffect, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import Avatar from '../components/ui/Avatar'
import { getListing, getCandidaciesForListing } from '../api/jobListings'
import { acceptCandidacy, rejectCandidacy, offerCandidacy } from '../api/candidacies'

const CANDIDACY_STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  OFFERED: '#8b5cf6',
  REJECTED: '#ef4444',
}

function ListingApplicants() {
  const { id } = useParams()

  const [listing, setListing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [candidacies, setCandidacies] = useState([])
  const [candidaciesLoading, setCandidaciesLoading] = useState(true)
  const [candidaciesError, setCandidaciesError] = useState(null)
  const [decidingId, setDecidingId] = useState(null)

  const loadListing = useCallback(() => {
    setLoading(true)
    getListing(id)
      .then((response) => setListing(response.data))
      .catch(() => setError('Failed to load this listing.'))
      .finally(() => setLoading(false))
  }, [id])

  const loadCandidacies = useCallback(() => {
    setCandidaciesLoading(true)
    getCandidaciesForListing(id)
      .then((response) => setCandidacies(response.data))
      .catch(() => setCandidaciesError('Failed to load applicants.'))
      .finally(() => setCandidaciesLoading(false))
  }, [id])

  useEffect(() => {
    loadListing()
    loadCandidacies()
  }, [loadListing, loadCandidacies])

  async function handleDecide(candidacyId, decide) {
    setDecidingId(candidacyId)
    try {
      const response = await decide(candidacyId)
      setCandidacies((prev) =>
        prev.map((c) =>
          c.id === candidacyId ? { ...c, status: response.data.status } : c
        )
      )
    } catch {
      setCandidaciesError('Failed to record your decision. Try again.')
    } finally {
      setDecidingId(null)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <LoadingSpinner />
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="p-6 text-red-500">{error}</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">
        <div>
          <Link to={`/listings/${id}`} className="text-sm text-gray-500 hover:text-gray-800">
            ← Back to {listing.title}
          </Link>
          <h1 className="text-2xl font-bold text-gray-800 mt-2">Applicants</h1>
        </div>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Applicants</h2>

          {candidaciesLoading && <LoadingSpinner />}
          {!candidaciesLoading && candidaciesError && (
            <div className="text-red-500 text-sm">{candidaciesError}</div>
          )}

          {!candidaciesLoading && !candidaciesError && candidacies.length === 0 && (
            <EmptyState title="No applicants yet" description="Check back once candidates apply." />
          )}

          {!candidaciesLoading && !candidaciesError && candidacies.length > 0 && (
            <div className="space-y-3">
              {candidacies.map((candidacy) => (
                <div
                  key={candidacy.id}
                  className="border border-gray-200 rounded-lg p-4"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Avatar
                        src={candidacy.candidate.pictureUrl}
                        name={candidacy.candidate.name}
                        size="sm"
                      />
                      <div>
                        <Link
                          to={`/listings/${id}/applicants/${candidacy.id}`}
                          state={{ candidacy }}
                          className="text-sm font-semibold text-gray-800 hover:text-blue-600 hover:underline"
                        >
                          {candidacy.candidate.name}
                        </Link>
                        <p className="text-sm text-gray-500">{candidacy.candidate.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {candidacy.matchScore != null && (
                        <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">
                          {Math.round(candidacy.matchScore * 100)}% match
                        </span>
                      )}
                      {candidacy.locationMismatch && (
                        <span className="text-xs font-medium text-amber-700 bg-amber-50 px-2.5 py-1 rounded-full">
                          Location may not match
                        </span>
                      )}
                      <Badge status={candidacy.status} colors={CANDIDACY_STATUS_COLORS} />
                    </div>
                  </div>

                  {candidacy.openToRemoteMismatch && (
                    <p className="text-xs text-gray-500 mt-2">
                      Candidate hasn't indicated they're open to remote work
                    </p>
                  )}

                  {candidacy.candidate.headline && (
                    <p className="text-sm text-gray-600 mt-2">{candidacy.candidate.headline}</p>
                  )}

                  {candidacy.candidate.skills?.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {candidacy.candidate.skills.map((skill) => (
                        <span
                          key={skill.id}
                          className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                        >
                          {skill.name}
                        </span>
                      ))}
                    </div>
                  )}

                  <div className="flex items-center gap-3 mt-3">
                    {candidacy.status === 'SUBMITTED' && (
                      <>
                        <Button
                          variant="primary"
                          disabled={decidingId === candidacy.id}
                          onClick={() => handleDecide(candidacy.id, acceptCandidacy)}
                        >
                          Accept
                        </Button>
                        <Button
                          variant="danger"
                          disabled={decidingId === candidacy.id}
                          onClick={() => handleDecide(candidacy.id, rejectCandidacy)}
                        >
                          Reject
                        </Button>
                      </>
                    )}
                    {candidacy.status === 'ACCEPTED' && (
                      <>
                        <Button
                          variant="primary"
                          disabled={decidingId === candidacy.id}
                          onClick={() => handleDecide(candidacy.id, offerCandidacy)}
                        >
                          Mark Offered
                        </Button>
                        <Button
                          variant="danger"
                          disabled={decidingId === candidacy.id}
                          onClick={() => handleDecide(candidacy.id, rejectCandidacy)}
                        >
                          Reject
                        </Button>
                      </>
                    )}
                    <Link
                      to={`/listings/${id}/applicants/${candidacy.id}`}
                      state={{ candidacy }}
                      className="text-sm text-blue-600 hover:underline"
                    >
                      View profile
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}

export default ListingApplicants
