import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import Navbar from '../components/Navbar'
import DatePicker from '../components/ui/DatePicker'
import { createApplication } from '../api/applications'
import { formatEnumLabel } from '../utils/formatEnumLabel'

const STATUS_OPTIONS = ['APPLIED', 'INTERVIEWING', 'OFFERED', 'REJECTED', 'WITHDRAWN']
const SOURCE_OPTIONS = ['LINKEDIN', 'COMPANY_WEBSITE', 'REFERRAL', 'JOB_BOARD', 'OTHER']

function CreateApplication() {
  const [companyName, setCompanyName] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [status, setStatus] = useState('')
  const [source, setSource] = useState('')
  const [appliedDate, setAppliedDate] = useState('')
  const [jobUrl, setJobUrl] = useState('')
  const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      await createApplication({
        companyName,
        jobTitle,
        status,
        source,
        appliedDate,
        jobUrl: jobUrl || null,
        notes: notes || null,
      })
      navigate('/applications')
    } catch (err) {
      setError('Failed to create application. Please check your inputs.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-2xl mx-auto px-6 py-8">
        <div className="flex items-center gap-4 mb-6">
          <Link to="/applications" className="text-sm text-gray-500 hover:text-gray-700">
            ← Back
          </Link>
          <h1 className="text-2xl font-bold text-gray-800">New Application</h1>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded mb-4 text-sm">
            {error}
          </div>
        )}

        <div className="bg-white rounded-lg shadow p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Company Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Job Title <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={jobTitle}
                onChange={(e) => setJobTitle(e.target.value)}
                required
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status <span className="text-red-500">*</span>
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  required
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select status</option>
                  {STATUS_OPTIONS.map(s => (
                    <option key={s} value={s}>{formatEnumLabel(s)}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Source <span className="text-red-500">*</span>
                </label>
                <select
                  value={source}
                  onChange={(e) => setSource(e.target.value)}
                  required
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select source</option>
                  {SOURCE_OPTIONS.map(s => (
                    <option key={s} value={s}>{formatEnumLabel(s)}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Applied Date <span className="text-red-500">*</span>
              </label>
              <DatePicker
                value={appliedDate}
                onChange={setAppliedDate}
                required
                max={new Date().toISOString().split('T')[0]}
                placeholder="Select a date"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Job URL
              </label>
              <input
                type="url"
                value={jobUrl}
                onChange={(e) => setJobUrl(e.target.value)}
                placeholder="https://..."
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Notes
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
                placeholder="Any notes about this application..."
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              />
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <Link
                to="/applications"
                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
              >
                Cancel
              </Link>
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 text-sm bg-blue-600 text-white rounded font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors cursor-pointer"
              >
                {loading ? 'Saving...' : 'Save Application'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default CreateApplication