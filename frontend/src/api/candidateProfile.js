import axiosInstance from './axiosInstance'

export function getMyProfile() {
  return axiosInstance.get('/api/candidate-profiles/me')
}

export function updateMyProfile(data) {
  return axiosInstance.put('/api/candidate-profiles/me', data)
}

export function setMySkills(skillIds) {
  return axiosInstance.put('/api/candidate-profiles/me/skills', { skillIds })
}
