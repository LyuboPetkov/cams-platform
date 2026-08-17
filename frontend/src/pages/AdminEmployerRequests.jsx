import { useState, useEffect, useCallback } from 'react'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { listEmployerRequests, approveEmployerRequest, rejectEmployerRequest } from '../api/employerRequests'

const TABS = ['PENDING', 'APPROVED', 'REJECTED']

const STATUS_COLORS = {
  PENDING: '#f59e0b',
  APPROVED: '#10b981',
  REJECTED: '#ef4444',
}

function AdminEmployerRequests() {
  const [activeTab, setActiveTab] = useState('PENDING')
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [actioningId, setActioningId] = useState(null)
  const [rejectingId, setRejectingId] = useState(null)
  const [rejectionReason, setRejectionReason] = useState('')
  const [actionError, setActionError] = useState(null)

  const fetchRequests = useCallback((status) => {
    setLoading(true)
    setError(null)
    listEmployerRequests(status)
      .then((response) => setRequests(response.data))
      .catch(() => setError('Failed to load employer requests.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    fetchRequests(activeTab)
  }, [activeTab, fetchRequests])

  function handleTabChange(tab) {
    setActiveTab(tab)
    setRejectingId(null)
    setRejectionReason('')
    setActionError(null)
  }

  function handleApprove(id) {
    setActioningId(id)
    setActionError(null)
    approveEmployerRequest(id)
      .then(() => fetchRequests(activeTab))
      .catch(() => setActionError('Failed to approve this request.'))
      .finally(() => setActioningId(null))
  }

  function handleRejectClick(id) {
    setRejectingId(id)
    setRejectionReason('')
    setActionError(null)
  }

  function handleRejectCancel() {
    setRejectingId(null)
    setRejectionReason('')
  }

  function handleRejectConfirm(id) {
    setActioningId(id)
    setActionError(null)
    rejectEmployerRequest(id, rejectionReason.trim())
      .then(() => {
        setRejectingId(null)
        setRejectionReason('')
        fetchRequests(activeTab)
      })
      .catch(() => setActionError('Failed to reject this request.'))
      .finally(() => setActioningId(null))
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-6">Employer Requests</h1>

        <div className="flex gap-2 mb-6 border-b border-gray-200">
          {TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => handleTabChange(tab)}
              className={`px-3 py-2 text-sm font-medium border-b-2 transition-colors cursor-pointer ${
                activeTab === tab
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-900'
              }`}
            >
              {tab.charAt(0) + tab.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {actionError && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded mb-4 text-sm">{actionError}</div>
        )}

        {loading && <LoadingSpinner />}
        {!loading && error && <div className="text-red-500 text-sm">{error}</div>}

        {!loading && !error && requests.length === 0 && (
          <EmptyState
            title={`No ${activeTab.toLowerCase()} requests`}
            description="Check back later or switch tabs to see other requests."
          />
        )}

        {!loading && !error && requests.length > 0 && (
          <div className="space-y-3">
            {requests.map((request) => (
              <Card key={request.id}>
                <div className="flex items-center justify-between">
                  <h3 className="text-base font-semibold text-gray-800">{request.companyName}</h3>
                  <Badge status={request.status} colors={STATUS_COLORS} />
                </div>

                <p className="text-sm text-gray-600 mt-2">
                  {request.requesterFullName} &middot; {request.requesterEmail}
                </p>

                {request.companyDescription && (
                  <p className="text-sm text-gray-500 mt-2">{request.companyDescription}</p>
                )}

                <div className="text-sm text-gray-500 mt-2 space-y-0.5">
                  {request.companyWebsite && <p>Website: {request.companyWebsite}</p>}
                  {request.companyLocation && <p>Location: {request.companyLocation}</p>}
                </div>

                <p className="text-xs text-gray-400 mt-3">
                  Submitted {new Date(request.requestedAt).toLocaleDateString()}
                </p>

                {request.status !== 'PENDING' && (
                  <p className="text-xs text-gray-400 mt-1">
                    Reviewed {new Date(request.reviewedAt).toLocaleDateString()}
                  </p>
                )}

                {request.status === 'REJECTED' && request.rejectionReason && (
                  <p className="text-sm text-red-600 mt-2">Reason: {request.rejectionReason}</p>
                )}

                {request.status === 'PENDING' && rejectingId !== request.id && (
                  <div className="flex gap-2 mt-4">
                    <Button
                      disabled={actioningId === request.id}
                      onClick={() => handleApprove(request.id)}
                    >
                      Approve
                    </Button>
                    <Button
                      variant="danger"
                      disabled={actioningId === request.id}
                      onClick={() => handleRejectClick(request.id)}
                    >
                      Reject
                    </Button>
                  </div>
                )}

                {request.status === 'PENDING' && rejectingId === request.id && (
                  <div className="mt-4">
                    <textarea
                      value={rejectionReason}
                      onChange={(e) => setRejectionReason(e.target.value)}
                      placeholder="Reason for rejection"
                      className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      rows={2}
                    />
                    <div className="flex gap-2 mt-2">
                      <Button
                        variant="danger"
                        disabled={!rejectionReason.trim() || actioningId === request.id}
                        onClick={() => handleRejectConfirm(request.id)}
                      >
                        Confirm reject
                      </Button>
                      <Button variant="secondary" onClick={handleRejectCancel}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminEmployerRequests
