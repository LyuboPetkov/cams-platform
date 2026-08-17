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
