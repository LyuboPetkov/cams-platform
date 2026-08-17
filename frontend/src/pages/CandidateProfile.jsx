import { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import {
  getMyProfile,
  updateMyProfile,
  setMySkills,
  setMyExperience,
  setMyEducation,
} from '../api/candidateProfile'
import { searchSkills } from '../api/skills'

const WORKING_HOURS_OPTIONS = [
  { value: '', label: 'Not stated' },
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'EITHER', label: 'Either' },
]

const EDUCATION_LEVEL_OPTIONS = [
  { value: 'SECONDARY', label: 'Secondary' },
  { value: 'BACHELORS', label: "Bachelor's" },
  { value: 'MASTERS', label: "Master's" },
  { value: 'DOCTORATE', label: 'Doctorate' },
  { value: 'OTHER', label: 'Other' },
]

function formatMonthYear(dateStr) {
  const [year, month] = dateStr.split('-')
  return new Date(Number(year), Number(month) - 1).toLocaleDateString('en-US', {
    month: 'short',
    year: 'numeric',
  })
}

function toExperienceRequest(entry) {
  return {
    roleTitle: entry.roleTitle,
    yearsOfExperience: entry.yearsOfExperience,
    description: entry.description,
  }
}

function toEducationRequest(entry) {
  return {
    institutionName: entry.institutionName,
    level: entry.level,
    startDate: entry.startDate,
    endDate: entry.endDate,
  }
}

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

  const [experienceForm, setExperienceForm] = useState({
    roleTitle: '',
    yearsOfExperience: '',
    description: '',
  })
  const [experienceSaving, setExperienceSaving] = useState(false)
  const [experienceError, setExperienceError] = useState(null)

  const [educationForm, setEducationForm] = useState({
    institutionName: '',
    level: 'BACHELORS',
    startMonth: '',
    endMonth: '',
  })
  const [educationSaving, setEducationSaving] = useState(false)
  const [educationError, setEducationError] = useState(null)

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

  async function saveExperience(entries) {
    setExperienceSaving(true)
    setExperienceError(null)
    try {
      const response = await setMyExperience(entries.map(toExperienceRequest))
      setProfile(response.data)
    } catch {
      setExperienceError('Failed to update experience.')
    } finally {
      setExperienceSaving(false)
    }
  }

  async function addExperience(e) {
    e.preventDefault()
    if (!experienceForm.roleTitle.trim()) return
    const entry = {
      roleTitle: experienceForm.roleTitle,
      yearsOfExperience: Number(experienceForm.yearsOfExperience) || 0,
      description: experienceForm.description || null,
    }
    await saveExperience([...profile.experience, entry])
    setExperienceForm({ roleTitle: '', yearsOfExperience: '', description: '' })
  }

  async function removeExperience(id) {
    await saveExperience(profile.experience.filter((entry) => entry.id !== id))
  }

  async function saveEducation(entries) {
    setEducationSaving(true)
    setEducationError(null)
    try {
      const response = await setMyEducation(entries.map(toEducationRequest))
      setProfile(response.data)
    } catch (err) {
      setEducationError(err.response?.data?.message || 'Failed to update education.')
    } finally {
      setEducationSaving(false)
    }
  }

  async function addEducation(e) {
    e.preventDefault()
    if (!educationForm.institutionName.trim() || !educationForm.startMonth) return
    const entry = {
      institutionName: educationForm.institutionName,
      level: educationForm.level,
      startDate: `${educationForm.startMonth}-01`,
      endDate: educationForm.endMonth ? `${educationForm.endMonth}-01` : null,
    }
    await saveEducation([...profile.education, entry])
    setEducationForm({ institutionName: '', level: 'BACHELORS', startMonth: '', endMonth: '' })
  }

  async function removeEducation(id) {
    await saveEducation(profile.education.filter((entry) => entry.id !== id))
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

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Experience</h2>

          <div className="space-y-3 mb-4">
            {profile.experience.length === 0 && (
              <p className="text-sm text-gray-500">No experience added yet.</p>
            )}
            {profile.experience.map((entry) => (
              <div
                key={entry.id}
                className="flex items-start justify-between gap-3 border border-gray-200 rounded p-3"
              >
                <div>
                  <p className="text-sm font-medium text-gray-800">
                    {entry.roleTitle} · {entry.yearsOfExperience} yr{entry.yearsOfExperience === 1 ? '' : 's'}
                  </p>
                  {entry.description && (
                    <p className="text-sm text-gray-500 mt-1">{entry.description}</p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => removeExperience(entry.id)}
                  disabled={experienceSaving}
                  className="text-xs text-gray-400 hover:text-red-500 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>

          {experienceError && <p className="text-sm text-red-500 mb-3">{experienceError}</p>}

          <form onSubmit={addExperience} className="space-y-2">
            <div className="grid grid-cols-3 gap-2">
              <input
                type="text"
                value={experienceForm.roleTitle}
                onChange={(e) => setExperienceForm({ ...experienceForm, roleTitle: e.target.value })}
                placeholder="Role title"
                className="col-span-2 border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="number"
                min="0"
                value={experienceForm.yearsOfExperience}
                onChange={(e) =>
                  setExperienceForm({ ...experienceForm, yearsOfExperience: e.target.value })
                }
                placeholder="Years"
                className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <textarea
              value={experienceForm.description}
              onChange={(e) => setExperienceForm({ ...experienceForm, description: e.target.value })}
              rows={2}
              placeholder="Description (optional)"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <Button type="submit" variant="secondary" disabled={experienceSaving}>
              {experienceSaving ? 'Adding...' : 'Add'}
            </Button>
          </form>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Education</h2>

          <div className="space-y-3 mb-4">
            {profile.education.length === 0 && (
              <p className="text-sm text-gray-500">No education added yet.</p>
            )}
            {profile.education.map((entry) => (
              <div
                key={entry.id}
                className="flex items-start justify-between gap-3 border border-gray-200 rounded p-3"
              >
                <div>
                  <p className="text-sm font-medium text-gray-800">
                    {entry.institutionName} ·{' '}
                    {EDUCATION_LEVEL_OPTIONS.find((opt) => opt.value === entry.level)?.label ?? entry.level}
                  </p>
                  <p className="text-sm text-gray-500 mt-1">
                    {formatMonthYear(entry.startDate)} –{' '}
                    {entry.endDate ? formatMonthYear(entry.endDate) : 'Present / Expected'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => removeEducation(entry.id)}
                  disabled={educationSaving}
                  className="text-xs text-gray-400 hover:text-red-500 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>

          {educationError && <p className="text-sm text-red-500 mb-3">{educationError}</p>}

          <form onSubmit={addEducation} className="space-y-2">
            <input
              type="text"
              value={educationForm.institutionName}
              onChange={(e) => setEducationForm({ ...educationForm, institutionName: e.target.value })}
              placeholder="Institution name"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="grid grid-cols-3 gap-2">
              <select
                value={educationForm.level}
                onChange={(e) => setEducationForm({ ...educationForm, level: e.target.value })}
                className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {EDUCATION_LEVEL_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
              <input
                type="month"
                value={educationForm.startMonth}
                onChange={(e) => setEducationForm({ ...educationForm, startMonth: e.target.value })}
                className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <input
                type="month"
                value={educationForm.endMonth}
                onChange={(e) => setEducationForm({ ...educationForm, endMonth: e.target.value })}
                placeholder="End (optional)"
                className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <Button type="submit" variant="secondary" disabled={educationSaving}>
              {educationSaving ? 'Adding...' : 'Add'}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  )
}

export default CandidateProfile
