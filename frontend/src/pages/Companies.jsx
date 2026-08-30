import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { listCompanies } from '../api/companies'

function Companies() {
  const navigate = useNavigate()

  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [page, setPage] = useState(0)
  const [pageData, setPageData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Debounce the query itself, not the fetch that uses it: page-button clicks
  // stay instant since they only touch the already-settled debouncedQuery.
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query)
      setPage(0)
    }, 400)
    return () => clearTimeout(timer)
  }, [query])

  function loadCompanies() {
    setLoading(true)
    setError(null)
    listCompanies(debouncedQuery, page)
      .then((response) => setPageData(response.data))
      .catch(() => setError('Failed to load companies.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadCompanies()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedQuery, page])

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-5xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Companies</h1>

        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search companies by name"
          className="w-full border border-gray-300 rounded px-3 py-2 text-sm mb-6 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && !error && pageData && pageData.content.length === 0 && (
          <EmptyState
            title={debouncedQuery ? 'No companies match your search' : 'No companies yet'}
            description={
              debouncedQuery
                ? 'Try a different name.'
                : 'Check back once companies join the platform.'
            }
          />
        )}

        {!loading && !error && pageData && pageData.content.length > 0 && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {pageData.content.map((company) => (
                <Card
                  key={company.id}
                  className="cursor-pointer hover:border-blue-300 transition-colors"
                >
                  <div
                    className="flex items-center gap-3"
                    onClick={() => navigate(`/companies/${company.id}`)}
                  >
                    <Avatar src={company.logoUrl} name={company.name} size="lg" variant="logo" />
                    <div>
                      <h3 className="text-base font-semibold text-gray-800">{company.name}</h3>
                      {company.description && (
                        <p className="text-sm text-gray-500 mt-1 line-clamp-2">{company.description}</p>
                      )}
                    </div>
                  </div>
                </Card>
              ))}
            </div>

            <div className="flex items-center justify-between mt-6">
              <div className="flex items-center gap-2">
                <Button variant="secondary" disabled={pageData.first} onClick={() => setPage(0)}>
                  « First
                </Button>
                <Button
                  variant="secondary"
                  disabled={pageData.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
              </div>
              <span className="text-sm text-gray-500">
                Page {pageData.page + 1} of {pageData.totalPages}
              </span>
              <Button variant="secondary" disabled={pageData.last} onClick={() => setPage((p) => p + 1)}>
                Next
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default Companies
