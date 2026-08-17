import axiosInstance from './axiosInstance'

export function applyToListing(jobListingId) {
  return axiosInstance.post('/api/candidacies', { jobListingId })
}

export function getMyCandidacies() {
  return axiosInstance.get('/api/candidacies/mine')
}
