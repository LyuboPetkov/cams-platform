import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Avatar from '../components/ui/Avatar'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import JobListingCard from '../components/ui/JobListingCard'
import { getCompanyById } from '../api/companies'
import { browseListings } from '../api/jobListings'

function CompanyDetail() {
  const { id } = useParams()

  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [listings, setListings] = useState([])
  const [listingsLoading, setListingsLoading] = useState(true)
  const [listingsError, setListingsError] = useState(null)

  useEffect(() => {
    loadCompany()
    loadListings()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  function loadCompany() {
    setLoading(true)
    getCompanyById(id)
      .then((response) => setCompany(response.data))
      .catch(() => setError('Failed to load this company.'))
      .finally(() => setLoading(false))
  }

  function loadListings() {
    setListingsLoading(true)
    browseListings({ companyId: id, size: 50 })
      .then((response) => setListings(response.data.content))
      .catch(() => setListingsError('Failed to load this company\'s listings.'))
      .finally(() => setListingsLoading(false))
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <LoadingSpinner />
      </div>
    )
  }

  if (error || !company) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="max-w-3xl mx-auto px-6 py-8">
          <Card>
            <p className="text-sm text-gray-700 mb-3">{error ?? 'Failed to load this company.'}</p>
            <Link to="/companies" className="text-sm text-blue-600 hover:underline">
              ← Back to Companies
            </Link>
          </Card>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">
        <Link to="/companies" className="text-sm text-gray-500 hover:text-gray-800">
          ← Back to Companies
        </Link>

        <Card>
          <div className="flex items-start gap-3">
            <Avatar src={company.logoUrl} name={company.name} size="xl" variant="logo" />
            <div>
              <h1 className="text-xl font-bold text-gray-800">{company.name}</h1>
              {company.location && <p className="text-sm text-gray-500 mt-1">{company.location}</p>}
              {company.website && (
                <a
                  href={company.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm text-blue-600 hover:underline mt-1 inline-block"
                >
                  {company.website}
                </a>
              )}
            </div>
          </div>

          {company.description && (
            <p className="text-sm text-gray-700 mt-4 whitespace-pre-wrap">{company.description}</p>
          )}
        </Card>

        <div>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Open listings</h2>

          {listingsLoading && <LoadingSpinner />}
          {!listingsLoading && listingsError && (
            <div className="text-red-500 text-sm">{listingsError}</div>
          )}

          {!listingsLoading && !listingsError && listings.length === 0 && (
            <EmptyState title="No open listings" description="This company has no open listings right now." />
          )}

          {!listingsLoading && !listingsError && listings.length > 0 && (
            <div className="space-y-3">
              {listings.map((listing) => (
                <Link key={listing.id} to={`/jobs/${listing.id}`} state={{ listing }} className="block">
                  <JobListingCard listing={listing} />
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default CompanyDetail
