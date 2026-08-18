import axiosInstance from './axiosInstance'

export function searchSkills(search, limit) {
  const params = {}
  if (search) params.search = search
  if (limit) params.limit = limit
  return axiosInstance.get('/api/skills', { params })
}
