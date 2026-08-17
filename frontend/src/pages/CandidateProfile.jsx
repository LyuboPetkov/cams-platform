import { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getMyProfile, updateMyProfile, setMySkills } from '../api/candidateProfile'
import { searchSkills } from '../api/skills'

const WORKING_HOURS_OPTIONS = [
  { value: '', label: 'Not stated' },
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'EITHER', label: 'Either' },
]

function CandidateProfile() {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState(null)

  const [headline, setHeadline] = useState('')
  const [description, setDescription] = useState('')
  const [location, setLocation] = useState('')
  const [openToRemote, setOpenToRemote] = useState(false)
  const [flexibleHours, setFlexibleHours] = useState(false)
  const [desiredWorkingHours, setDesiredWorkingHours] = useState('')

  const [skillSearch, setSkillSearch] = useState('')
  const [skillResults, setSkillResults] = useState([])
  const [skillsSaving, setSkillsSaving] = useState(false)

  function loadProfile() {
    setLoading(true)
    getMyProfile()
      .then((response) => {
        const data = response.data
        setProfile(data)
        setHeadline(data.headline ?? '')
        setDescription(data.description ?? '')
        setLocation(data.location ?? '')
        setOpenToRemote(Boolean(data.openToRemote))
        setFlexibleHours(Boolean(data.flexibleHours))
        setDesiredWorkingHours(data.desiredWorkingHours ?? '')
      })
      .catch(() => setError('Failed to load your profile.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadProfile()
  }, [])

  async function handleSaveProfile(e) {
    e.preventDefault()
    setSaving(true)
    setSaveMessage(null)
    try {
      const response = await updateMyProfile({
        headline,
        description,
        location,
        openToRemote,
        flexibleHours,
        desiredWorkingHours: desiredWorkingHours || null,
      })
      setProfile(response.data)
      setSaveMessage('Profile saved.')
    } catch {
      setSaveMessage('Failed to save profile.')
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
    if (profile.skills.some((s) => s.id === skill.id)) return
    const nextSkillIds = [...profile.skills.map((s) => s.id), skill.id]
    await saveSkills(nextSkillIds)
  }

  async function removeSkill(skillId) {
    const nextSkillIds = profile.skills.filter((s) => s.id !== skillId).map((s) => s.id)
    await saveSkills(nextSkillIds)
  }

  async function saveSkills(skillIds) {
    setSkillsSaving(true)
    try {
      const response = await setMySkills(skillIds)
      setProfile(response.data)
    } catch {
      setSaveMessage('Failed to update skills.')
    } finally {
      setSkillsSaving(false)
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
        <h1 className="text-2xl font-bold text-gray-800">Your Profile</h1>

        <Card>
          <form onSubmit={handleSaveProfile} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Headline</label>
              <input
                type="text"
                value={headline}
                onChange={(e) => setHeadline(e.target.value)}
                placeholder="Backend developer"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                placeholder="Six years building Spring services."
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="Sofia, Bulgaria"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={openToRemote}
                  onChange={(e) => setOpenToRemote(e.target.checked)}
                />
                Open to remote work
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={flexibleHours}
                  onChange={(e) => setFlexibleHours(e.target.checked)}
                />
                Flexible hours
              </label>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Desired working hours
              </label>
              <select
                value={desiredWorkingHours}
                onChange={(e) => setDesiredWorkingHours(e.target.value)}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {WORKING_HOURS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-3">
              <Button type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save Profile'}
              </Button>
              {saveMessage && <span className="text-sm text-gray-500">{saveMessage}</span>}
            </div>
          </form>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Skills</h2>

          <div className="flex flex-wrap gap-2 mb-4">
            {profile.skills.length === 0 && (
              <p className="text-sm text-gray-500">No skills added yet.</p>
            )}
            {profile.skills.map((skill) => (
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
              placeholder="Search skills, e.g. 'project management'"
              className="flex-1 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <Button type="submit" variant="secondary">Search</Button>
          </form>

          {skillResults.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {skillResults.map((skill) => {
                const alreadyAdded = profile.skills.some((s) => s.id === skill.id)
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
      </div>
    </div>
  )
}

export default CandidateProfile
