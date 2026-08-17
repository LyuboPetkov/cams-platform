import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getMyListings } from '../api/jobListings'

const STATUS_COLORS = {
  OPEN: '#10b981',
  ARCHIVED: '#6b7280',
}

function MyListings() {
  const navigate = useNavigate()
  const [listings, setListings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    getMyListings()
      .then((response) => setListings(response.data))
      .catch(() => setError('Failed to load your listings.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-800">My Listings</h1>
          <Button onClick={() => navigate('/listings/new')}>New Listing</Button>
        </div>

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && !error && listings.length === 0 && (
          <EmptyState
            title="You haven't posted any listings yet"
            description="Create your first listing to start receiving applications."
            action={
              <Link to="/listings/new" className="text-sm text-blue-600 hover:underline">
                Create a listing
              </Link>
            }
          />
        )}

        {!loading && !error && listings.length > 0 && (
          <div className="space-y-3">
            {listings.map((listing) => (
              <Card
                key={listing.id}
                className="cursor-pointer hover:border-blue-300 transition-colors"
              >
                <div onClick={() => navigate(`/listings/${listing.id}`)}>
                  <div className="flex items-center justify-between">
                    <h3 className="text-base font-semibold text-gray-800">{listing.title}</h3>
                    <Badge status={listing.status} colors={STATUS_COLORS} />
                  </div>
                  <p className="text-sm text-gray-500 mt-1">
                    {listing.location || 'No location listed'}
                    {listing.remote ? ' · Remote' : ''} · {listing.level}
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

export default MyListings
