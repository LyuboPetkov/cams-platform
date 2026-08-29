import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getListing } from '../api/jobListings'
import { formatEnumLabel } from '../utils/formatEnumLabel'

function ListingOverview() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [listing, setListing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadListing = useCallback(() => {
    setLoading(true)
    getListing(id)
      .then((response) => setListing(response.data))
      .catch(() => setError('Failed to load this listing.'))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    loadListing()
  }, [loadListing])

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
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800">{listing.title}</h1>
          <Badge status={listing.status} colors={{ OPEN: '#10b981', ARCHIVED: '#6b7280' }} />
        </div>

        <Card>
          <p className="text-sm text-gray-500 mb-4">
            {listing.location || 'No location listed'}
            {listing.remote ? ' · Remote' : ''} · {formatEnumLabel(listing.level)}
          </p>

          <p className="text-sm text-gray-700 whitespace-pre-wrap">{listing.description}</p>

          <div className="mt-4">
            <h2 className="text-sm font-semibold text-gray-700 mb-2">Required skills</h2>
            {listing.skills.length === 0 ? (
              <p className="text-sm text-gray-500">No skills required yet.</p>
            ) : (
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
            )}
          </div>
        </Card>

        <div className="flex items-center gap-4">
          <Button onClick={() => navigate(`/listings/${id}/edit`)}>Edit Listing</Button>
          <Link to={`/listings/${id}/applicants`} className="text-sm text-blue-600 hover:underline">
            Applicants →
          </Link>
        </div>
      </div>
    </div>
  )
}

export default ListingOverview
