import { useState, useEffect } from 'react'
import Navbar from '../components/Navbar'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import Avatar from '../components/ui/Avatar'
import EditableFullName from '../components/ui/EditableFullName'
import { getMyCompany, updateMyCompany, uploadMyLogo } from '../api/companies'
import { useAuth } from '../context/AuthContext'

function CompanyProfile() {
  const { user, updateUserPicture, updateUserFullName } = useAuth()
  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState(null)

  const [description, setDescription] = useState('')
  const [website, setWebsite] = useState('')
  const [location, setLocation] = useState('')
  const [logoUrl, setLogoUrl] = useState('')
  const [logoUploading, setLogoUploading] = useState(false)
  const [logoError, setLogoError] = useState(null)

  useEffect(() => {
    getMyCompany()
      .then((response) => {
        const data = response.data
        setCompany(data)
        setDescription(data.description ?? '')
        setWebsite(data.website ?? '')
        setLocation(data.location ?? '')
        setLogoUrl(data.logoUrl ?? '')
      })
      .catch(() => setError('Failed to load your company profile.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setSaveMessage(null)
    try {
      const response = await updateMyCompany({ description, website, location })
      setCompany(response.data)
      setSaveMessage('Company profile saved.')
    } catch {
      setSaveMessage('Failed to save company profile.')
    } finally {
      setSaving(false)
    }
  }

  async function handleLogoUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return

    setLogoUploading(true)
    setLogoError(null)
    try {
      const response = await uploadMyLogo(file)
      setCompany(response.data)
      setLogoUrl(response.data.logoUrl)
      updateUserPicture(response.data.logoUrl)
    } catch (err) {
      setLogoError(err.response?.data?.message || 'Failed to upload logo.')
    } finally {
      setLogoUploading(false)
      e.target.value = ''
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <LoadingSpinner />
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="p-6 text-red-500">{error}</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">
        <div className="flex items-center gap-4">
          <Avatar src={logoUrl} name={company.name} size="lg" />
          <div>
            <EditableFullName fullName={user?.fullName} onSaved={updateUserFullName} />
            <p className="text-sm text-gray-500">{user?.email}</p>
          </div>
        </div>

        <Card>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                type="text"
                value={company.name}
                disabled
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-gray-100 text-gray-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                placeholder="We build widgets."
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Website</label>
              <input
                type="url"
                value={website}
                onChange={(e) => setWebsite(e.target.value)}
                placeholder="https://acme.example.com"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="Sofia, Bulgaria"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Logo</label>
              <div className="flex items-center gap-3">
                <Avatar src={logoUrl} name={company.name} size="md" />
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  onChange={handleLogoUpload}
                  disabled={logoUploading}
                  className="flex-1 text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 file:cursor-pointer disabled:opacity-50"
                />
              </div>
              {logoUploading && <p className="text-xs text-gray-400 mt-1">Uploading...</p>}
              {logoError && <p className="text-sm text-red-500 mt-1">{logoError}</p>}
            </div>

            <div className="flex items-center gap-3">
              <Button type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save Company Profile'}
              </Button>
              {saveMessage && <span className="text-sm text-gray-500">{saveMessage}</span>}
            </div>
          </form>
        </Card>
      </div>
    </div>
  )
}

export default CompanyProfile
