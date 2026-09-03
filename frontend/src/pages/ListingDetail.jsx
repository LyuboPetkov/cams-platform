import { useState, useEffect } from 'react'
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { applyToListing } from '../api/candidacies'
import { getListingForCandidate, getMyMatchScore } from '../api/jobListings'
import { formatRelativeTime } from '../utils/formatRelativeTime'
import { formatEnumLabel } from '../utils/formatEnumLabel'

const CANDIDACY_STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  REJECTED: '#ef4444',
}

// Reached with the listing in router state, or without it (typed URL, refresh),
// when it falls back to GET /api/job-listings/{id}/candidate-view - gated on
// having a Candidacy, not on OPEN, so an applied-to listing stays viewable after archiving.
function ListingDetail() {
  const location = useLocation()
  const navigate = useNavigate()
  const { id } = useParams()

  const [listing, setListing] = useState(location.state?.listing ?? null)
  const [loading, setLoading] = useState(!location.state?.listing)
  const [loadError, setLoadError] = useState(null)

  // Semantic match score for the badge. Its own request, independent of whether
  // the listing came from router state or the fallback fetch. Null - no badge -
  // when the viewer isn't a candidate, has no profile embedding, or on any error.
  const [matchScore, setMatchScore] = useState(null)

  const backTo = location.state?.from ?? '/jobs'
  const backLabel = backTo === '/matches' ? 'Back to Matches' : 'Back to Browse Jobs'

  const [applying, setApplying] = useState(false)
  const [applyError, setApplyError] = useState(null)
  const [applied, setApplied] = useState(false)

  useEffect(() => {
    if (listing) return
    getListingForCandidate(id)
      .then((response) => setListing(response.data))
      .catch(() => setLoadError('Failed to load this listing.'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  useEffect(() => {
    getMyMatchScore(id)
      .then((response) => setMatchScore(response.data.score))
      .catch(() => setMatchScore(null))
  }, [id])

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <LoadingSpinner />
      </div>
    )
  }

  if (loadError || !listing) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-3xl mx-auto px-6 py-8">
          <Card>
            <p className="text-sm text-gray-700 mb-3">
              {loadError ?? 'Failed to load this listing.'}
            </p>
            <Link to={backTo} className="text-sm text-blue-600 hover:underline">
              {backLabel}
            </Link>
          </Card>
        </div>
      </div>
    )
  }

  async function handleApply() {
    setApplying(true)
    setApplyError(null)
    try {
      await applyToListing(listing.id)
      setApplied(true)
    } catch (err) {
      if (err.response?.status === 409) {
        setApplyError('You have already applied to this listing, or it is no longer open.')
      } else {
        setApplyError('Failed to submit your application.')
      }
    } finally {
      setApplying(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <button
          onClick={() => navigate(-1)}
          className="text-sm text-gray-500 hover:text-gray-800 mb-4 cursor-pointer"
        >
          ← Back
        </button>

        <Card>
          <div className="flex items-start justify-between">
            <div className="flex items-start gap-3">
              <Avatar src={listing.company?.logoUrl} name={listing.company?.name} size="lg" variant="logo" />
              <div>
                <h1 className="text-xl font-bold text-gray-800">{listing.title}</h1>
                <p className="text-sm text-gray-500 mt-1">
                  {listing.company?.name} · {listing.location || 'No location listed'}
                  {listing.remote ? ' · Remote' : ''}
                </p>
                {listing.createdAt && (
                  <p className="text-xs text-gray-400 mt-1">
                    Posted {formatRelativeTime(listing.createdAt)}
                  </p>
                )}
              </div>
            </div>
            <div className="flex flex-col items-end gap-2 shrink-0">
              <span className="text-xs font-medium text-gray-500 bg-gray-100 px-2.5 py-1 rounded-full">
                {formatEnumLabel(listing.level)}
              </span>
              {matchScore != null && (
                <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full whitespace-nowrap">
                  {Math.round(matchScore * 100)}% match
                </span>
              )}
            </div>
          </div>

          {(listing.company?.description || listing.company?.website) && (
            <div className="mt-4 pt-4 border-t border-gray-200">
              <h2 className="text-sm font-semibold text-gray-700 mb-2">About the company</h2>
              {listing.company.description && (
                <p className="text-sm text-gray-700 whitespace-pre-wrap">{listing.company.description}</p>
              )}
              {listing.company.website && (
                <a
                  href={listing.company.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-blue-600 hover:underline mt-2 inline-block"
                >
                  {listing.company.website}
                </a>
              )}
            </div>
          )}

          <p className="text-sm text-gray-700 mt-4 whitespace-pre-wrap">{listing.description}</p>

          {listing.skills?.length > 0 && (
            <div className="mt-4">
              <h2 className="text-sm font-semibold text-gray-700 mb-2">Required skills</h2>
              <div className="flex flex-wrap gap-1.5">
                {listing.skills.map((skill) => (
                  <span
                    key={skill.id}
                    className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
                  >
                    {skill.name}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="mt-6 pt-4 border-t border-gray-200">
            {applied ? (
              <div>
                <p className="text-sm text-emerald-600 font-medium">
                  Application submitted - check My Candidacies for its status.
                </p>
                <Link to={backTo} className="text-sm text-blue-600 hover:underline mt-2 inline-block">
                  ← {backLabel}
                </Link>
              </div>
            ) : listing.myCandidacyStatus ? (
              <Badge status={listing.myCandidacyStatus} colors={CANDIDACY_STATUS_COLORS} />
            ) : (
              <>
                <Button onClick={handleApply} disabled={applying}>
                  {applying ? 'Applying...' : 'Apply'}
                </Button>
                {applyError && (
                  <p className="text-sm text-red-500 mt-2">{applyError}</p>
                )}
              </>
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}

export default ListingDetail
