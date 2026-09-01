import { useState, useEffect, useRef } from 'react'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import Avatar from '../components/ui/Avatar'
import EditableFullName from '../components/ui/EditableFullName'
import SkillTypeahead from '../components/ui/SkillTypeahead'
import SavedIndicator from '../components/ui/SavedIndicator'
import DatePicker from '../components/ui/DatePicker'
import {
  getMyProfile,
  updateMyProfile,
  uploadMyPicture,
  setMySkills,
  setMyExperience,
  setMyEducation,
  setMyCourses,
} from '../api/candidateProfile'
import { useAuth } from '../context/AuthContext'

const PROFILE_AUTOSAVE_DEBOUNCE_MS = 800

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
    startDate: entry.startDate,
    endDate: entry.endDate,
    description: entry.description,
  }
}

function toEducationRequest(entry) {
  return {
    institutionName: entry.institutionName,
    specialty: entry.specialty,
    level: entry.level,
    startDate: entry.startDate,
    endDate: entry.endDate,
  }
}

function toCourseRequest(entry) {
  return {
    name: entry.name,
    issuingOrganization: entry.issuingOrganization,
    completionDate: entry.completionDate,
    credentialUrl: entry.credentialUrl,
  }
}

function CandidateProfile() {
  const { user, updateUserPicture, updateUserFullName } = useAuth()

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saveMessage, setSaveMessage] = useState(null)

  const [headline, setHeadline] = useState('')
  const [description, setDescription] = useState('')
  const [location, setLocation] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [pictureUrl, setPictureUrl] = useState('')
  const [openToRemote, setOpenToRemote] = useState(false)
  const [flexibleHours, setFlexibleHours] = useState(false)
  const [desiredWorkingHours, setDesiredWorkingHours] = useState('')
  const [profileSavedAt, setProfileSavedAt] = useState(null)
  const skipProfileAutosaveRef = useRef(true)

  const [pictureUploading, setPictureUploading] = useState(false)
  const [pictureError, setPictureError] = useState(null)

  const [skillsSaving, setSkillsSaving] = useState(false)
  const [skillsSavedAt, setSkillsSavedAt] = useState(null)

  const [experienceForm, setExperienceForm] = useState({
    roleTitle: '',
    startDate: '',
    endDate: '',
    description: '',
  })
  const [experienceSaving, setExperienceSaving] = useState(false)
  const [experienceError, setExperienceError] = useState(null)
  const [experienceSavedAt, setExperienceSavedAt] = useState(null)

  const [educationForm, setEducationForm] = useState({
    institutionName: '',
    specialty: '',
    level: 'BACHELORS',
    startDate: '',
    endDate: '',
  })
  const [educationSaving, setEducationSaving] = useState(false)
  const [educationError, setEducationError] = useState(null)
  const [educationSavedAt, setEducationSavedAt] = useState(null)

  const [courseForm, setCourseForm] = useState({
    name: '',
    issuingOrganization: '',
    completionDate: '',
    credentialUrl: '',
  })
  const [coursesSaving, setCoursesSaving] = useState(false)
  const [coursesError, setCoursesError] = useState(null)
  const [coursesSavedAt, setCoursesSavedAt] = useState(null)

  function loadProfile() {
    setLoading(true)
    getMyProfile()
      .then((response) => {
        const data = response.data
        setProfile(data)
        setHeadline(data.headline ?? '')
        setDescription(data.description ?? '')
        setLocation(data.location ?? '')
        setPhoneNumber(data.phoneNumber ?? '')
        setPictureUrl(data.pictureUrl ?? '')
        setOpenToRemote(Boolean(data.openToRemote))
        setFlexibleHours(Boolean(data.flexibleHours))
        setDesiredWorkingHours(data.desiredWorkingHours ?? '')
        skipProfileAutosaveRef.current = true
      })
      .catch(() => setError('Failed to load your profile.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadProfile()
  }, [])

  async function saveProfileFields(fields) {
    setSaveMessage(null)
    try {
      const response = await updateMyProfile(fields)
      setProfile(response.data)
      setProfileSavedAt((tick) => tick + 1)
      updateUserPicture(response.data.pictureUrl)
    } catch {
      setSaveMessage('Failed to save profile.')
    }
  }

  useEffect(() => {
    if (skipProfileAutosaveRef.current) {
      skipProfileAutosaveRef.current = false
      return
    }

    const timer = setTimeout(() => {
      saveProfileFields({
        headline,
        description,
        location,
        phoneNumber,
        openToRemote,
        flexibleHours,
        desiredWorkingHours: desiredWorkingHours || null,
      })
    }, PROFILE_AUTOSAVE_DEBOUNCE_MS)

    return () => clearTimeout(timer)
  }, [headline, description, location, phoneNumber, openToRemote, flexibleHours, desiredWorkingHours])

  // Unlike the debounced text autosave above, a picture upload fires immediately
  // on file selection and surfaces StorageService's content-type/size errors.
  async function handlePictureUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return

    setPictureUploading(true)
    setPictureError(null)
    try {
      const response = await uploadMyPicture(file)
      setProfile(response.data)
      setPictureUrl(response.data.pictureUrl)
      setProfileSavedAt((tick) => tick + 1)
      updateUserPicture(response.data.pictureUrl)
    } catch (err) {
      setPictureError(err.response?.data?.message || 'Failed to upload picture.')
    } finally {
      setPictureUploading(false)
      e.target.value = ''
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
      setSkillsSavedAt((tick) => tick + 1)
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
      setExperienceSavedAt((tick) => tick + 1)
    } catch {
      setExperienceError('Failed to update experience.')
    } finally {
      setExperienceSaving(false)
    }
  }

  async function addExperience(e) {
    e.preventDefault()
    if (!experienceForm.roleTitle.trim() || !experienceForm.startDate) return
    const entry = {
      roleTitle: experienceForm.roleTitle,
      startDate: experienceForm.startDate,
      endDate: experienceForm.endDate || null,
      description: experienceForm.description || null,
    }
    await saveExperience([...profile.experience, entry])
    setExperienceForm({ roleTitle: '', startDate: '', endDate: '', description: '' })
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
      setEducationSavedAt((tick) => tick + 1)
    } catch (err) {
      setEducationError(err.response?.data?.message || 'Failed to update education.')
    } finally {
      setEducationSaving(false)
    }
  }

  async function addEducation(e) {
    e.preventDefault()
    if (!educationForm.institutionName.trim() || !educationForm.startDate) return
    const entry = {
      institutionName: educationForm.institutionName,
      specialty: educationForm.specialty || null,
      level: educationForm.level,
      startDate: educationForm.startDate,
      endDate: educationForm.endDate || null,
    }
    await saveEducation([...profile.education, entry])
    setEducationForm({ institutionName: '', specialty: '', level: 'BACHELORS', startDate: '', endDate: '' })
  }

  async function removeEducation(id) {
    await saveEducation(profile.education.filter((entry) => entry.id !== id))
  }

  async function saveCourses(entries) {
    setCoursesSaving(true)
    setCoursesError(null)
    try {
      const response = await setMyCourses(entries.map(toCourseRequest))
      setProfile(response.data)
      setCoursesSavedAt((tick) => tick + 1)
    } catch {
      setCoursesError('Failed to update courses.')
    } finally {
      setCoursesSaving(false)
    }
  }

  async function addCourse(e) {
    e.preventDefault()
    if (!courseForm.name.trim() || !courseForm.issuingOrganization.trim()) return
    const entry = {
      name: courseForm.name,
      issuingOrganization: courseForm.issuingOrganization,
      completionDate: courseForm.completionDate || null,
      credentialUrl: courseForm.credentialUrl || null,
    }
    await saveCourses([...profile.courses, entry])
    setCourseForm({ name: '', issuingOrganization: '', completionDate: '', credentialUrl: '' })
  }

  async function removeCourse(id) {
    await saveCourses(profile.courses.filter((entry) => entry.id !== id))
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

      <div className="max-w-5xl mx-auto px-6 py-8 space-y-6">
        <div className="flex items-center gap-4">
          <Avatar src={pictureUrl} name={user?.fullName} size="lg" />
          <div>
            <EditableFullName fullName={user?.fullName} onSaved={updateUserFullName} />
            <p className="text-sm text-gray-500">{user?.email}</p>
          </div>
        </div>

        <Card>
          <div className="space-y-4">
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
                rows={6}
                placeholder="Six years building Spring services."
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="flex items-center gap-3">
              <SavedIndicator trigger={profileSavedAt} />
              {saveMessage && <span className="text-sm text-red-500">{saveMessage}</span>}
            </div>
          </div>
        </Card>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-1 space-y-6">
        <Card>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Picture</label>
              <div className="flex items-center gap-3">
                <Avatar src={pictureUrl} name={user?.fullName} size="md" />
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  onChange={handlePictureUpload}
                  disabled={pictureUploading}
                  className="flex-1 text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 file:cursor-pointer disabled:opacity-50"
                />
              </div>
              {pictureUploading && <p className="text-xs text-gray-400 mt-1">Uploading...</p>}
              {pictureError && <p className="text-sm text-red-500 mt-1">{pictureError}</p>}
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

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Phone number</label>
              <input
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="+359 88 123 4567"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
              <SavedIndicator trigger={profileSavedAt} />
              {saveMessage && <span className="text-sm text-red-500">{saveMessage}</span>}
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">Skills</h2>
            <SavedIndicator trigger={skillsSavedAt} />
          </div>

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

          <SkillTypeahead
            excludeIds={profile.skills.map((s) => s.id)}
            onSelect={addSkill}
            placeholder="Search skills, e.g. 'project management'"
          />
        </Card>
        </div>

        <div className="md:col-span-2 space-y-6">
        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">Experience</h2>
            <SavedIndicator trigger={experienceSavedAt} />
          </div>

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
                    {entry.roleTitle} · {formatMonthYear(entry.startDate)} –{' '}
                    {entry.endDate ? formatMonthYear(entry.endDate) : 'Present'}
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
            <input
              type="text"
              value={experienceForm.roleTitle}
              onChange={(e) => setExperienceForm({ ...experienceForm, roleTitle: e.target.value })}
              placeholder="Role title"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <textarea
              value={experienceForm.description}
              onChange={(e) => setExperienceForm({ ...experienceForm, description: e.target.value })}
              rows={2}
              placeholder="Description (optional)"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <DatePicker
                mode="month"
                value={experienceForm.startDate}
                onChange={(next) => setExperienceForm({ ...experienceForm, startDate: next })}
                placeholder="Start month"
              />
              <DatePicker
                mode="month"
                value={experienceForm.endDate}
                onChange={(next) => setExperienceForm({ ...experienceForm, endDate: next })}
                placeholder="End (optional)"
                isClearable
              />
            </div>
            <p className="text-xs text-gray-400 text-right">Leave blank for "Present"</p>
            <Button type="submit" variant="secondary" disabled={experienceSaving}>
              {experienceSaving ? 'Adding...' : 'Add'}
            </Button>
          </form>
        </Card>

        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">Education</h2>
            <SavedIndicator trigger={educationSavedAt} />
          </div>

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
                  {entry.specialty && (
                    <p className="text-sm text-gray-600">{entry.specialty}</p>
                  )}
                  <p className="text-sm text-gray-500 mt-1">
                    {formatMonthYear(entry.startDate)} –{' '}
                    {entry.endDate ? formatMonthYear(entry.endDate) : 'Present'}
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
            <input
              type="text"
              value={educationForm.specialty}
              onChange={(e) => setEducationForm({ ...educationForm, specialty: e.target.value })}
              placeholder="Specialty (optional, e.g. Computer and Software Engineering)"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
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
              <DatePicker
                mode="month"
                value={educationForm.startDate}
                onChange={(next) => setEducationForm({ ...educationForm, startDate: next })}
                placeholder="Start month"
              />
              <DatePicker
                mode="month"
                value={educationForm.endDate}
                onChange={(next) => setEducationForm({ ...educationForm, endDate: next })}
                placeholder="End (optional)"
                isClearable
              />
            </div>
            <p className="text-xs text-gray-400 text-right">Leave blank for "Present"</p>
            <Button type="submit" variant="secondary" disabled={educationSaving}>
              {educationSaving ? 'Adding...' : 'Add'}
            </Button>
          </form>
        </Card>

        <Card>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-700">Courses & Certificates</h2>
            <SavedIndicator trigger={coursesSavedAt} />
          </div>

          <div className="space-y-3 mb-4">
            {profile.courses.length === 0 && (
              <p className="text-sm text-gray-500">No courses added yet.</p>
            )}
            {profile.courses.map((entry) => (
              <div
                key={entry.id}
                className="flex items-start justify-between gap-3 border border-gray-200 rounded p-3"
              >
                <div>
                  <p className="text-sm font-medium text-gray-800">
                    {entry.name} · {entry.issuingOrganization}
                  </p>
                  <p className="text-sm text-gray-500 mt-1">
                    {entry.completionDate ? formatMonthYear(entry.completionDate) : 'Ongoing / no date'}
                  </p>
                  {entry.credentialUrl && (
                    <a
                      href={entry.credentialUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-sm text-blue-600 hover:underline"
                    >
                      View credential
                    </a>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => removeCourse(entry.id)}
                  disabled={coursesSaving}
                  className="text-xs text-gray-400 hover:text-red-500 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed shrink-0"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>

          {coursesError && <p className="text-sm text-red-500 mb-3">{coursesError}</p>}

          <form onSubmit={addCourse} className="space-y-2">
            <input
              type="text"
              value={courseForm.name}
              onChange={(e) => setCourseForm({ ...courseForm, name: e.target.value })}
              placeholder="Course/certificate title"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="text"
              value={courseForm.issuingOrganization}
              onChange={(e) => setCourseForm({ ...courseForm, issuingOrganization: e.target.value })}
              placeholder="Issuing organization"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <DatePicker
                mode="month"
                value={courseForm.completionDate}
                onChange={(next) => setCourseForm({ ...courseForm, completionDate: next })}
                placeholder="Completion date (optional)"
                isClearable
              />
              <input
                type="url"
                value={courseForm.credentialUrl}
                onChange={(e) => setCourseForm({ ...courseForm, credentialUrl: e.target.value })}
                placeholder="Credential URL (optional)"
                className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <Button type="submit" variant="secondary" disabled={coursesSaving}>
              {coursesSaving ? 'Adding...' : 'Add'}
            </Button>
          </form>
        </Card>
        </div>
        </div>
      </div>
    </div>
  )
}

export default CandidateProfile
