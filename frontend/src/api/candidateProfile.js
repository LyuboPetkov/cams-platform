import axiosInstance from './axiosInstance'

export function getMyProfile() {
  return axiosInstance.get('/api/candidate-profiles/me')
}

export function updateMyProfile(data) {
  return axiosInstance.put('/api/candidate-profiles/me', data)
}

export function uploadMyPicture(file) {
  const formData = new FormData()
  formData.append('file', file)
  return axiosInstance.post('/api/candidate-profiles/me/picture', formData)
}

export function setMySkills(skillIds) {
  return axiosInstance.put('/api/candidate-profiles/me/skills', { skillIds })
}

export function setMyExperience(experiences) {
  return axiosInstance.put('/api/candidate-profiles/me/experience', { experiences })
}

export function setMyEducation(educations) {
  return axiosInstance.put('/api/candidate-profiles/me/education', { educations })
}

export function setMyCourses(courses) {
  return axiosInstance.put('/api/candidate-profiles/me/courses', { courses })
}
