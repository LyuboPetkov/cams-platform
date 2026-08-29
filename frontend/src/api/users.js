import axiosInstance from './axiosInstance'

export function updateMyFullName(fullName) {
  return axiosInstance.patch('/api/users/me', { fullName })
}

export function promoteToAdmin(email) {
  return axiosInstance.post('/api/admin/users/promote', { email })
}
