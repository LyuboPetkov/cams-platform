import axiosInstance from './axiosInstance'

export function listCompanies(query = '', page = 0, size = 20) {
  const params = { page, size }
  if (query) params.query = query
  return axiosInstance.get('/api/companies', { params })
}

export function getCompanyById(id) {
  return axiosInstance.get(`/api/companies/${id}`)
}

export function getMyCompany() {
  return axiosInstance.get('/api/companies/me')
}

export function updateMyCompany(data) {
  return axiosInstance.put('/api/companies/me', data)
}

export function uploadMyLogo(file) {
  const formData = new FormData()
  formData.append('file', file)
  return axiosInstance.post('/api/companies/me/logo', formData)
}
