import axiosInstance from './axiosInstance'

export function getApplications(filters = {}) {
  const params = {}
  if (filters.status) params.status = filters.status
  if (filters.source) params.source = filters.source
  return axiosInstance.get('/api/applications', { params })
}

export function getApplicationById(id) {
  return axiosInstance.get(`/api/applications/${id}`)
}

export function createApplication(data) {
  return axiosInstance.post('/api/applications', data)
}

export function updateApplication(id, data) {
  return axiosInstance.put(`/api/applications/${id}`, data)
}

export function deleteApplication(id) {
  return axiosInstance.delete(`/api/applications/${id}`)
}