import axiosInstance from './axiosInstance'

export function applyToListing(jobListingId) {
  return axiosInstance.post('/api/candidacies', { jobListingId })
}

export function getMyCandidacies() {
  return axiosInstance.get('/api/candidacies/mine')
}

export function acceptCandidacy(id) {
  return axiosInstance.post(`/api/candidacies/${id}/accept`)
}

export function rejectCandidacy(id) {
  return axiosInstance.post(`/api/candidacies/${id}/reject`)
}

export function offerCandidacy(id) {
  return axiosInstance.post(`/api/candidacies/${id}/offer`)
}

export function getCandidateProfileForCandidacy(id) {
  return axiosInstance.get(`/api/candidacies/${id}/candidate-profile`)
}
