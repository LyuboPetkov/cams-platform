import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { browseListings } from '../api/jobListings'
import { searchSkills } from '../api/skills'

const LEVEL_OPTIONS = ['', 'INTERN', 'JUNIOR', 'MID', 'SENIOR']
const REMOTE_OPTIONS = [
  { value: '', label: 'Any' },
  { value: 'true', label: 'Remote only' },
  { value: 'false', label: 'Onsite only' },
]

function BrowseListings() {
  const navigate = useNavigate()

  const [location, setLocation] = useState('')
  const [remote, setRemote] = useState('')
  const [level, setLevel] = useState('')
  const [skillFilters, setSkillFilters] = useState([])
  const [skillQuery, setSkillQuery] = useState('')
  const [skillResults, setSkillResults] = useState([])

  const [page, setPage] = useState(0)
  const [pageData, setPageData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loadListings()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  function loadListings() {
    setLoading(true)
    setError(null)
    browseListings({
      location: location || undefined,
      remote: remote === '' ? undefined : remote === 'true',
      level: level || undefined,
      skillIds: skillFilters.map((s) => s.id),
      page,
    })
      .then((response) => setPageData(response.data))
      .catch(() => setError('Failed to load listings.'))
      .finally(() => setLoading(false))
  }

  function handleFilterSubmit(e) {
    e.preventDefault()
    setPage(0)
    loadListings()
  }

  async function handleSkillSearch(e) {
    e.preventDefault()
    if (!skillQuery.trim()) {
      setSkillResults([])
      return
    }
    const response = await searchSkills(skillQuery)
    setSkillResults(response.data)
  }

  function addSkillFilter(skill) {
    if (skillFilters.some((s) => s.id === skill.id)) return
    setSkillFilters([...skillFilters, skill])
  }

  function removeSkillFilter(skillId) {
    setSkillFilters(skillFilters.filter((s) => s.id !== skillId))
  }

  function openListing(listing) {
    navigate(`/jobs/${listing.id}`, { state: { listing } })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-5xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Browse Jobs</h1>

        <Card className="mb-6">
          <form onSubmit={handleFilterSubmit} className="space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                <input
                  type="text"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="e.g. Bulgaria"
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Remote</label>
                <select
                  value={remote}
                  onChange={(e) => setRemote(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {REMOTE_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select
                  value={level}
                  onChange={(e) => setLevel(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {LEVEL_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt || 'Any'}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Skills</label>
              {skillFilters.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-2">
                  {skillFilters.map((skill) => (
                    <span
                      key={skill.id}
                      className="inline-flex items-center gap-2 bg-blue-50 text-blue-700 text-xs font-medium px-2.5 py-1 rounded-full"
                    >
                      {skill.name}
                      <button
                        type="button"
                        onClick={() => removeSkillFilter(skill.id)}
                        className="text-blue-400 hover:text-red-500 cursor-pointer"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={skillQuery}
                  onChange={(e) => setSkillQuery(e.target.value)}
                  placeholder="Search skills to filter by"
                  className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <Button type="button" variant="secondary" onClick={handleSkillSearch}>
                  Search
                </Button>
              </div>
              {skillResults.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-2">
                  {skillResults.map((skill) => (
                    <button
                      key={skill.id}
                      type="button"
                      onClick={() => addSkillFilter(skill)}
                      className="text-xs font-medium px-2.5 py-1 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 cursor-pointer"
                    >
                      + {skill.name}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <Button type="submit">Apply Filters</Button>
          </form>
        </Card>

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && !error && pageData && pageData.content.length === 0 && (
          <EmptyState
            title="No listings match your filters"
            description="Try widening your search."
          />
        )}

        {!loading && !error && pageData && pageData.content.length > 0 && (
          <>
            <div className="space-y-3">
              {pageData.content.map((listing) => (
                <Card
                  key={listing.id}
                  className="cursor-pointer hover:border-blue-300 transition-colors"
                >
                  <div onClick={() => openListing(listing)}>
                    <div className="flex items-center justify-between">
                      <h3 className="text-base font-semibold text-gray-800">{listing.title}</h3>
                      <span className="text-xs text-gray-500">{listing.level}</span>
                    </div>
                    <p className="text-sm text-gray-500 mt-1">
                      {listing.company?.name} · {listing.location || 'No location listed'}
                      {listing.remote ? ' · Remote' : ''}
                    </p>
                    {listing.skills?.length > 0 && (
                      <div className="flex flex-wrap gap-1.5 mt-2">
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
              ))}
            </div>

            <div className="flex items-center justify-between mt-6">
              <Button
                variant="secondary"
                disabled={pageData.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <span className="text-sm text-gray-500">
                Page {pageData.page + 1} of {pageData.totalPages}
              </span>
              <Button
                variant="secondary"
                disabled={pageData.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default BrowseListings
