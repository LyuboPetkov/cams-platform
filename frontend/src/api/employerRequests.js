import axiosInstance from './axiosInstance'

export function listEmployerRequests(status) {
  return axiosInstance.get('/api/admin/employer-requests', { params: { status } })
}

export function approveEmployerRequest(id) {
  return axiosInstance.post(`/api/admin/employer-requests/${id}/approve`)
}

export function rejectEmployerRequest(id, rejectionReason) {
  return axiosInstance.post(`/api/admin/employer-requests/${id}/reject`, { rejectionReason })
}
