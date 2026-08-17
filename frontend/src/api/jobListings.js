import axiosInstance from './axiosInstance'

export function browseListings(filters = {}) {
  const params = {}
  if (filters.location) params.location = filters.location
  if (filters.remote !== undefined && filters.remote !== null) params.remote = filters.remote
  if (filters.level) params.level = filters.level
  if (filters.skillIds?.length) params.skillIds = filters.skillIds
  params.page = filters.page ?? 0
  params.size = filters.size ?? 20
  return axiosInstance.get('/api/job-listings', { params })
}

export function getMyMatches() {
  return axiosInstance.get('/api/job-listings/matches')
}

export function createListing(data) {
  return axiosInstance.post('/api/job-listings', data)
}

export function getMyListings() {
  return axiosInstance.get('/api/job-listings/mine')
}

export function getListing(id) {
  return axiosInstance.get(`/api/job-listings/${id}`)
}

export function updateListing(id, data) {
  return axiosInstance.put(`/api/job-listings/${id}`, data)
}

export function setListingSkills(id, skillIds) {
  return axiosInstance.put(`/api/job-listings/${id}/skills`, { skillIds })
}

export function archiveListing(id) {
  return axiosInstance.post(`/api/job-listings/${id}/archive`)
}

export function getCandidaciesForListing(id) {
  return axiosInstance.get(`/api/job-listings/${id}/candidacies`)
}

export function getMatchesForListing(id) {
  return axiosInstance.get(`/api/job-listings/${id}/matches`)
}
