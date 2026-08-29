import { useState, useEffect, useCallback } from 'react'
import { useParams, useLocation, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Avatar from '../components/ui/Avatar'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { getCandidateProfileForCandidacy, acceptCandidacy, rejectCandidacy } from '../api/candidacies'
import { formatEnumLabel } from '../utils/formatEnumLabel'

const CANDIDACY_STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  REJECTED: '#ef4444',
}

function formatMonthYear(dateStr) {
  const [year, month] = dateStr.split('-')
  return new Date(Number(year), Number(month) - 1).toLocaleDateString('en-US', {
    month: 'short',
    year: 'numeric',
  })
}

// Reached from ListingApplicants with the candidacy in router state, or directly
// with none (typed URL, refresh) - the header then shows a generic label.
// Accept/Reject still work either way; the backend's 409 covers a decision already made.
function CandidateDetail() {
  const { id, candidacyId } = useParams()
  const location = useLocation()

  const candidacyFromState = location.state?.candidacy ?? null

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [status, setStatus] = useState(candidacyFromState?.status ?? null)
  const [deciding, setDeciding] = useState(false)
  const [decideError, setDecideError] = useState(null)

  const loadProfile = useCallback(() => {
    setLoading(true)
    getCandidateProfileForCandidacy(candidacyId)
      .then((response) => setProfile(response.data))
      .catch(() => setError('Failed to load this candidate\'s profile.'))
      .finally(() => setLoading(false))
  }, [candidacyId])

  useEffect(() => {
    loadProfile()
  }, [loadProfile])

  async function handleDecide(decide) {
    setDeciding(true)
    setDecideError(null)
    try {
      const response = await decide(candidacyId)
      setStatus(response.data.status)
    } catch (err) {
      if (err.response?.status === 409) {
        setDecideError('This candidacy has already been decided.')
      } else {
        setDecideError('Failed to record your decision. Try again.')
      }
    } finally {
      setDeciding(false)
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

  if (error || !profile) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="p-6 text-red-500">{error ?? 'Failed to load this candidate\'s profile.'}</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">
        <Link to={`/listings/${id}/applicants`} className="text-sm text-gray-500 hover:text-gray-800">
          ← Back to Applicants
        </Link>

        <Card>
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <Avatar
                src={profile.pictureUrl}
                name={candidacyFromState?.candidate?.name}
                size="lg"
              />
              <div>
                <h1 className="text-xl font-bold text-gray-800">
                  {candidacyFromState?.candidate?.name ?? 'Applicant'}
                </h1>
                {candidacyFromState?.candidate?.email && (
                  <p className="text-sm text-gray-500">{candidacyFromState.candidate.email}</p>
                )}
                {profile.headline && (
                  <p className="text-sm text-gray-600 mt-1">{profile.headline}</p>
                )}
                {profile.location && (
                  <p className="text-sm text-gray-500 mt-1">{profile.location}</p>
                )}
              </div>
            </div>
            {status && <Badge status={status} colors={CANDIDACY_STATUS_COLORS} />}
          </div>

          {profile.description && (
            <p className="text-sm text-gray-700 mt-4 whitespace-pre-wrap">{profile.description}</p>
          )}
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Skills</h2>
          {profile.skills.length === 0 && <p className="text-sm text-gray-500">No skills added.</p>}
          <div className="flex flex-wrap gap-1.5">
            {profile.skills.map((skill) => (
              <span
                key={skill.id}
                className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
              >
                {skill.name}
              </span>
            ))}
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Experience</h2>
          <div className="space-y-3">
            {profile.experience.length === 0 && (
              <p className="text-sm text-gray-500">No experience added.</p>
            )}
            {profile.experience.map((entry) => (
              <div key={entry.id} className="border border-gray-200 rounded p-3">
                <p className="text-sm font-medium text-gray-800">
                  {entry.roleTitle} · {formatMonthYear(entry.startDate)} –{' '}
                  {entry.endDate ? formatMonthYear(entry.endDate) : 'Present'}
                </p>
                {entry.description && (
                  <p className="text-sm text-gray-500 mt-1">{entry.description}</p>
                )}
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Education</h2>
          <div className="space-y-3">
            {profile.education.length === 0 && (
              <p className="text-sm text-gray-500">No education added.</p>
            )}
            {profile.education.map((entry) => (
              <div key={entry.id} className="border border-gray-200 rounded p-3">
                <p className="text-sm font-medium text-gray-800">
                  {entry.institutionName} · {formatEnumLabel(entry.level)}
                </p>
                {entry.specialty && <p className="text-sm text-gray-600">{entry.specialty}</p>}
                <p className="text-sm text-gray-500 mt-1">
                  {formatMonthYear(entry.startDate)} –{' '}
                  {entry.endDate ? formatMonthYear(entry.endDate) : 'Present'}
                </p>
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <h2 className="text-sm font-semibold text-gray-700 mb-3">Courses & Certificates</h2>
          <div className="space-y-3">
            {profile.courses.length === 0 && (
              <p className="text-sm text-gray-500">No courses added.</p>
            )}
            {profile.courses.map((entry) => (
              <div key={entry.id} className="border border-gray-200 rounded p-3">
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
            ))}
          </div>
        </Card>

        {(status === null || status === 'SUBMITTED') && (
          <Card>
            <div className="flex gap-2">
              <Button variant="primary" disabled={deciding} onClick={() => handleDecide(acceptCandidacy)}>
                Accept
              </Button>
              <Button variant="danger" disabled={deciding} onClick={() => handleDecide(rejectCandidacy)}>
                Reject
              </Button>
            </div>
            {decideError && <p className="text-sm text-red-500 mt-2">{decideError}</p>}
          </Card>
        )}
      </div>
    </div>
  )
}

export default CandidateDetail
