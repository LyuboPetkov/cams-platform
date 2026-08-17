import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import Avatar from '../components/ui/Avatar'
import {
  getListing,
  updateListing,
  setListingSkills,
  archiveListing,
  getCandidaciesForListing,
  getMatchesForListing,
} from '../api/jobListings'
import { acceptCandidacy, rejectCandidacy } from '../api/candidacies'
import { searchSkills } from '../api/skills'

const LEVEL_OPTIONS = ['INTERN', 'JUNIOR', 'MID', 'SENIOR']
const CANDIDACY_STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  REJECTED: '#ef4444',
}

function ManageListing() {
  const { id } = useParams()

  const [listing, setListing] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [location, setLocation] = useState('')
  const [remote, setRemote] = useState(false)
  const [level, setLevel] = useState('MID')
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState(null)

  const [skillSearch, setSkillSearch] = useState('')
  const [skillResults, setSkillResults] = useState([])
  const [skillsSaving, setSkillsSaving] = useState(false)

  const [archiving, setArchiving] = useState(false)

  const [candidacies, setCandidacies] = useState([])
  const [candidaciesLoading, setCandidaciesLoading] = useState(true)
  const [candidaciesError, setCandidaciesError] = useState(null)
  const [decidingId, setDecidingId] = useState(null)

  const [matches, setMatches] = useState(null)
  const [matchesLoading, setMatchesLoading] = useState(true)
  const [noEmbedding, setNoEmbedding] = useState(false)
  const [matchesError, setMatchesError] = useState(null)

  const loadListing = useCallback(() => {
    setLoading(true)
    getListing(id)
      .then((response) => {
        const data = response.data
        setListing(data)
        setTitle(data.title)
        setDescription(data.description)
        setLocation(data.location ?? '')
        setRemote(Boolean(data.remote))
        setLevel(data.level)
      })
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

  const loadMatches = useCallback(() => {
    setMatchesLoading(true)
    setNoEmbedding(false)
    getMatchesForListing(id)
      .then((response) => setMatches(response.data))
      .catch((err) => {
        if (err.response?.status === 409) {
          setNoEmbedding(true)
        } else {
          setMatchesError('Failed to load matches.')
        }
      })
      .finally(() => setMatchesLoading(false))
  }, [id])

  useEffect(() => {
    loadListing()
    loadCandidacies()
    loadMatches()
  }, [loadListing, loadCandidacies, loadMatches])

  async function handleSaveFields(e) {
    e.preventDefault()
    setSaving(true)
    setSaveMessage(null)
    try {
      const response = await updateListing(id, { title, description, location, remote, level })
      setListing(response.data)
      setSaveMessage('Listing saved.')
    } catch {
      setSaveMessage('Failed to save listing.')
    } finally {
      setSaving(false)
    }
  }

  async function handleSkillSearch(e) {
    e.preventDefault()
    if (!skillSearch.trim()) {
      setSkillResults([])
      return
    }
    try {
      const response = await searchSkills(skillSearch)
      setSkillResults(response.data)
    } catch {
      setSkillResults([])
    }
  }

  async function addSkill(skill) {
    if (listing.skills.some((s) => s.id === skill.id)) return
    const nextSkillIds = [...listing.skills.map((s) => s.id), skill.id]
    await saveSkills(nextSkillIds)
  }

  async function removeSkill(skillId) {
    const nextSkillIds = listing.skills.filter((s) => s.id !== skillId).map((s) => s.id)
    await saveSkills(nextSkillIds)
  }

  async function saveSkills(skillIds) {
    setSkillsSaving(true)
    try {
      const response = await setListingSkills(id, skillIds)
      setListing(response.data)
    } catch {
      setSaveMessage('Failed to update skills.')
    } finally {
      setSkillsSaving(false)
    }
  }

  async function handleArchive() {
    setArchiving(true)
    try {
      const response = await archiveListing(id)
      setListing(response.data)
    } catch {
      setSaveMessage('Failed to archive listing.')
    } finally {
      setArchiving(false)
    }
  }

  async function handleDecide(candidacyId, decide) {
    setDecidingId(candidacyId)
    try {
      const response = await decide(candidacyId)
      setCandidacies((prev) =>
        prev.map((c) => (c.id === candidacyId ? response.data : c))
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

  const isArchived = listing.status === 'ARCHIVED'

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800">{listing.title}</h1>
          <Badge status={listing.status} colors={{ OPEN: '#10b981', ARCHIVED: '#6b7280' }} />
        </div>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Listing details</h2>
          <form onSubmit={handleSaveFields} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                rows={4}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={remote}
                  onChange={(e) => setRemote(e.target.checked)}
                />
                Remote-friendly
              </label>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Level</label>
                <select
                  value={level}
                  onChange={(e) => setLevel(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {LEVEL_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <Button type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save Listing'}
              </Button>
              {saveMessage && <span className="text-sm text-gray-500">{saveMessage}</span>}
            </div>
          </form>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Skills</h2>

          <div className="flex flex-wrap gap-2 mb-4">
            {listing.skills.length === 0 && (
              <p className="text-sm text-gray-500">No skills required yet.</p>
            )}
            {listing.skills.map((skill) => (
              <span
                key={skill.id}
                className="inline-flex items-center gap-2 bg-blue-50 text-blue-700 text-xs font-medium px-2.5 py-1 rounded-full"
              >
                {skill.name}
                <button
                  type="button"
                  onClick={() => removeSkill(skill.id)}
                  disabled={skillsSaving}
                  className="text-blue-400 hover:text-red-500 cursor-pointer"
                >
                  ×
                </button>
              </span>
            ))}
          </div>

          <form onSubmit={handleSkillSearch} className="flex gap-2 mb-3">
            <input
              type="text"
              value={skillSearch}
              onChange={(e) => setSkillSearch(e.target.value)}
              placeholder="Search skills to require"
              className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <Button type="submit" variant="secondary">Search</Button>
          </form>

          {skillResults.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {skillResults.map((skill) => {
                const alreadyAdded = listing.skills.some((s) => s.id === skill.id)
                return (
                  <button
                    key={skill.id}
                    type="button"
                    disabled={alreadyAdded || skillsSaving}
                    onClick={() => addSkill(skill)}
                    className="text-xs font-medium px-2.5 py-1 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  >
                    {alreadyAdded ? `${skill.name} (added)` : `+ ${skill.name}`}
                  </button>
                )
              })}
            </div>
          )}
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-2">Archive</h2>
          <p className="text-sm text-gray-500 mb-4">
            Archiving removes this listing from browse and stops new applications. This cannot be undone.
          </p>
          {isArchived ? (
            <p className="text-sm text-gray-500">This listing is archived.</p>
          ) : (
            <Button variant="danger" onClick={handleArchive} disabled={archiving}>
              {archiving ? 'Archiving...' : 'Archive Listing'}
            </Button>
          )}
        </Card>

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
                        <h3 className="text-sm font-semibold text-gray-800">
                          {candidacy.candidate.name}
                        </h3>
                        <p className="text-sm text-gray-500">{candidacy.candidate.email}</p>
                      </div>
                    </div>
                    <Badge status={candidacy.status} colors={CANDIDACY_STATUS_COLORS} />
                  </div>

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

                  {candidacy.status === 'SUBMITTED' && (
                    <div className="flex gap-2 mt-3">
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
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Matches</h2>

          {matchesLoading && <LoadingSpinner />}
          {!matchesLoading && matchesError && (
            <div className="text-red-500 text-sm">{matchesError}</div>
          )}

          {!matchesLoading && !matchesError && noEmbedding && (
            <EmptyState
              title="This listing has no embedding yet"
              description="It may have just been created; try again shortly."
            />
          )}

          {!matchesLoading && !matchesError && !noEmbedding && matches?.length === 0 && (
            <EmptyState
              title="No matches yet"
              description="Check back once more candidates have filled in their profiles."
            />
          )}

          {!matchesLoading && !matchesError && !noEmbedding && matches?.length > 0 && (
            <div className="space-y-3">
              {matches.map((match, i) => (
                <div key={i} className="border border-gray-200 rounded-lg p-4">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-gray-800">{match.candidate.name}</h3>
                    <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">
                      {Math.round(match.similarityScore * 100)}% match
                    </span>
                  </div>
                  {match.candidate.headline && (
                    <p className="text-sm text-gray-500 mt-1">{match.candidate.headline}</p>
                  )}
                  {match.candidate.skills?.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {match.candidate.skills.map((skill) => (
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
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}

export default ManageListing
