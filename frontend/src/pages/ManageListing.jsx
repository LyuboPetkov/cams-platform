import { useState, useEffect, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import SkillTypeahead from '../components/ui/SkillTypeahead'
import SavedIndicator from '../components/ui/SavedIndicator'
import { getListing, updateListing, setListingSkills, archiveListing } from '../api/jobListings'
import { formatEnumLabel } from '../utils/formatEnumLabel'

const LEVEL_OPTIONS = ['INTERN', 'JUNIOR', 'MID', 'SENIOR']

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

  const [skillsSaving, setSkillsSaving] = useState(false)
  const [skillsSavedAt, setSkillsSavedAt] = useState(null)

  const [archiving, setArchiving] = useState(false)

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

  useEffect(() => {
    loadListing()
  }, [loadListing])

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
      setSkillsSavedAt((tick) => tick + 1)
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
        <div>
          <Link to={`/listings/${id}`} className="text-sm text-gray-500 hover:text-gray-800">
            ← Back to listing
          </Link>
        </div>

        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800">{listing.title}</h1>
          <div className="flex items-center gap-3">
            <Badge status={listing.status} />
            <Link to={`/listings/${id}/applicants`} className="text-sm text-blue-600 hover:underline">
              Applicants →
            </Link>
          </div>
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

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
                    <option key={opt} value={opt}>{formatEnumLabel(opt)}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-sm font-medium text-gray-700">Skills</label>
                <SavedIndicator trigger={skillsSavedAt} />
              </div>

              <div className="flex flex-wrap gap-2 mb-2">
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

              <SkillTypeahead
                excludeIds={listing.skills.map((s) => s.id)}
                onSelect={addSkill}
                placeholder="Search skills to require"
              />
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
      </div>
    </div>
  )
}

export default ManageListing
