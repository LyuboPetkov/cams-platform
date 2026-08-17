import axiosInstance from './axiosInstance'

export function searchSkills(search) {
  const params = {}
  if (search) params.search = search
  return axiosInstance.get('/api/skills', { params })
}
