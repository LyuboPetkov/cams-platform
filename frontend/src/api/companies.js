import axiosInstance from './axiosInstance'

export function getMyCompany() {
  return axiosInstance.get('/api/companies/me')
}

export function updateMyCompany(data) {
  return axiosInstance.put('/api/companies/me', data)
}
