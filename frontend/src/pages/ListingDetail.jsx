import { useState } from 'react'
import { useLocation, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import { applyToListing } from '../api/candidacies'
import { formatRelativeTime } from '../utils/formatRelativeTime'

// There is no candidate-facing GET /api/job-listings/{id} — that route is
// employer-ownership-scoped (403 for a candidate). Browse is the only place a
// candidate can read a listing's full description/skills, so the listing
// object is passed via router state from BrowseListings rather than re-fetched
// here. A direct visit or refresh has no state to work with, so it bounces
// back to /jobs instead of showing a broken page.
function ListingDetail() {
  const location = useLocation()
  const navigate = useNavigate()
  const listing = location.state?.listing

  const [applying, setApplying] = useState(false)
  const [applyError, setApplyError] = useState(null)
  const [applied, setApplied] = useState(false)

  if (!listing) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-3xl mx-auto px-6 py-8">
          <Card>
            <p className="text-sm text-gray-700 mb-3">
              This listing wasn't opened from Browse Jobs, so there's nothing to show here.
            </p>
            <Link to="/jobs" className="text-sm text-blue-600 hover:underline">
              Back to Browse Jobs
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
              <Avatar src={listing.company?.logoUrl} name={listing.company?.name} size="md" />
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
            <span className="text-xs font-medium text-gray-500 bg-gray-100 px-2.5 py-1 rounded-full">
              {listing.level}
            </span>
          </div>

          <p className="text-sm text-gray-700 mt-4 whitespace-pre-wrap">{listing.description}</p>

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
              <p className="text-sm text-emerald-600 font-medium">
                Application submitted — check My Candidacies for its status.
              </p>
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
