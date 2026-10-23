import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Badge from '../components/ui/Badge'
import { getApplications, deleteApplication } from '../api/applications'
import { formatEnumLabel } from '../utils/formatEnumLabel'
import { EXPORT_FORMATS, exportApplications } from '../utils/exportApplications'

const STATUS_OPTIONS = ['APPLIED', 'INTERVIEWING', 'OFFERED', 'REJECTED', 'WITHDRAWN']
const SOURCE_OPTIONS = ['LINKEDIN', 'COMPANY_WEBSITE', 'REFERRAL', 'JOB_BOARD', 'OTHER']

function ApplicationsList() {
  const [applications, setApplications] = useState([])
  const location = useLocation()
  const [status, setStatus] = useState(location.state?.status || '')
  const [source, setSource] = useState(location.state?.source || '')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [viewApp, setViewApp] = useState(null)
  const [exportOpen, setExportOpen] = useState(false)
  const [exporting, setExporting] = useState(false)

  const navigate = useNavigate()

  // Refetch whenever a filter changes. The fetch lives inside the effect so the
  // effect only depends on status and source, not on a function redeclared every render.
  // Loading/error are reset in handleFilterChange rather than here, because setting
  // state synchronously inside an effect causes an extra render.
  useEffect(() => {
    getApplications({ status, source })
      .then((response) => setApplications(response.data))
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false))
  }, [status, source])

  function handleFilterChange(setFilter, value) {
    setLoading(true)
    setError(null)
    setFilter(value)
  }

  async function handleDelete(id) {
    try {
      await deleteApplication(id)
      setApplications(applications.filter(app => app.id !== id))
    } catch (err) {
      if (err.response?.status === 409 && err.response.data?.message) {
        setError(err.response.data.message)
      } else {
        setError('Failed to delete application.')
      }
    } finally {
      setDeleteId(null)
    }
  }

  // Exports exactly what is currently shown, so active filters apply.
  async function handleExport(format) {
    setExportOpen(false)
    setExporting(true)
    setError(null)
    try {
      await exportApplications(applications, format)
    } catch {
      setError('Failed to export applications.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-gray-800">My Applications</h1>
          <div className="flex items-center gap-3">
            <div className="relative">
              <button
                onClick={() => setExportOpen(!exportOpen)}
                disabled={loading || exporting || applications.length === 0}
                className="border border-gray-300 bg-white text-gray-700 px-4 py-2 rounded font-medium hover:bg-gray-50 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {exporting ? 'Exporting...' : 'Export ▾'}
              </button>
              {exportOpen && (
                <>
                  {/* Invisible backdrop so a click anywhere else closes the menu */}
                  <div className="fixed inset-0 z-10" onClick={() => setExportOpen(false)} />
                  <div className="absolute right-0 mt-2 w-44 bg-white border border-gray-200 rounded shadow-lg z-20 py-1">
                    {EXPORT_FORMATS.map(f => (
                      <button
                        key={f.value}
                        onClick={() => handleExport(f.value)}
                        className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 cursor-pointer"
                      >
                        {f.label}
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
            <button
              onClick={() => navigate('/applications/new')}
              className="bg-blue-600 text-white px-4 py-2 rounded font-medium hover:bg-blue-700 transition-colors cursor-pointer"
            >
              + New Application
            </button>
          </div>
        </div>

        <div className="flex gap-4 mb-6">
          <select
            value={status}
            onChange={(e) => handleFilterChange(setStatus, e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            {STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{formatEnumLabel(s)}</option>
            ))}
          </select>

          <select
            value={source}
            onChange={(e) => handleFilterChange(setSource, e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Sources</option>
            {SOURCE_OPTIONS.map(s => (
              <option key={s} value={s}>{formatEnumLabel(s)}</option>
            ))}
          </select>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded mb-4 text-sm">
            {error}
          </div>
        )}

        {loading ? (
          <div className="text-center text-gray-500 py-12">Loading...</div>
        ) : applications.length === 0 ? (
          <div className="text-center text-gray-500 py-12">
  {status || source ? (
    'No applications match the selected filters.'
  ) : (
    <>
      No applications yet.{' '}
      <Link to="/applications/new" className="text-blue-600 hover:underline">
        Add your first one.
      </Link>
    </>
  )}
</div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Company</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Job Title</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Status</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Source</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Applied Date</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {applications.map(app => (
                  <tr key={app.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-800">{app.companyName}</td>
                    <td className="px-4 py-3 text-gray-600">{app.jobTitle}</td>
                    <td className="px-4 py-3">
                      <Badge status={app.status} />
                    </td>
                    <td className="px-4 py-3 text-gray-600">{formatEnumLabel(app.source)}</td>
                    <td className="px-4 py-3 text-gray-600">{app.appliedDate}</td>
                    <td className="px-4 py-3 flex gap-2">
                      <button
                        onClick={() => setViewApp(app)}
                        className="text-gray-500 hover:underline cursor-pointer"
                      >
                        View
                      </button>
                      <button
                        onClick={() => navigate(`/applications/${app.id}/edit`)}
                        className="text-blue-600 hover:underline cursor-pointer"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => setDeleteId(app.id)}
                        className="text-red-500 hover:underline cursor-pointer"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {deleteId && (
          <div className="fixed inset-0 bg-black/20 backdrop-blur-sm flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-sm">
              <h2 className="text-lg font-semibold text-gray-800 mb-2">Delete Application</h2>
              <p className="text-sm text-gray-600 mb-6">
                Are you sure you want to delete this application? This cannot be undone.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setDeleteId(null)}
                  className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleDelete(deleteId)}
                  className="px-4 py-2 text-sm bg-red-600 text-white rounded hover:bg-red-700"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}
        {viewApp && (
           <div className="fixed inset-0 bg-black/20 backdrop-blur-sm flex items-center justify-center z-50">
    <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-lg">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-800">Application Details</h2>
        <button
          onClick={() => setViewApp(null)}
          className="text-gray-400 hover:text-gray-600 text-xl font-bold cursor-pointer"
        >
          &times;
        </button>
      </div>

      <div className="space-y-3 text-sm">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-gray-500 font-medium">Company</p>
            <p className="text-gray-800">{viewApp.companyName}</p>
          </div>
          <div>
            <p className="text-gray-500 font-medium">Job Title</p>
            <p className="text-gray-800">{viewApp.jobTitle}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-gray-500 font-medium">Status</p>
            <Badge status={viewApp.status} />
          </div>
          <div>
            <p className="text-gray-500 font-medium">Source</p>
            <p className="text-gray-800">{formatEnumLabel(viewApp.source)}</p>
          </div>
        </div>

        <div>
          <p className="text-gray-500 font-medium">Applied Date</p>
          <p className="text-gray-800">{viewApp.appliedDate}</p>
        </div>

        <div>
          <p className="text-gray-500 font-medium">Job URL</p>
          {viewApp.jobListingId ? (
            <Link
              to={`/jobs/${viewApp.jobListingId}`}
              className="text-blue-600 hover:underline"
            >
              View listing
            </Link>
          ) : viewApp.jobUrl ? (
            <a
              href={viewApp.jobUrl}
              target="_blank"
              rel="noreferrer"
              className="text-blue-600 hover:underline break-all"
            >
              {viewApp.jobUrl}
            </a>
) : (
  <p className="text-gray-400 italic">Not provided</p>
)}
        </div>

        <div>
          <p className="text-gray-500 font-medium">Notes</p>
          {viewApp.notes ? (
            <p className="text-gray-800 whitespace-pre-wrap">{viewApp.notes}</p>
          ) : (
            <p className="text-gray-400 italic">No notes</p>
          )}
        </div>
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <button
          onClick={() => {
            const id = viewApp.id
            setViewApp(null)
            navigate(`/applications/${id}/edit`)
          }}
          className="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 cursor-pointer"
        >
          Edit
        </button>
        <button
          onClick={() => setViewApp(null)}
          className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 cursor-pointer"
        >
          Close
        </button>
      </div>
    </div>
  </div>
)}
      </div>
    </div>
  )
}

export default ApplicationsList