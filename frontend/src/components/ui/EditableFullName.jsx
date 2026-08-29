import { useState } from 'react'
import { updateMyFullName } from '../../api/users'

// Click-to-edit for the one field a user can change about their own account.
// Mirrors the inline-input style used across the profile forms.
function EditableFullName({ fullName, onSaved }) {
  const [editing, setEditing] = useState(false)
  const [value, setValue] = useState(fullName ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function startEditing() {
    setValue(fullName ?? '')
    setError(null)
    setEditing(true)
  }

  function cancel() {
    setEditing(false)
    setError(null)
  }

  async function save() {
    const trimmed = value.trim()
    if (!trimmed) {
      setError('Full name is required.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await updateMyFullName(trimmed)
      onSaved(trimmed)
      setEditing(false)
    } catch {
      setError('Failed to save name.')
    } finally {
      setSaving(false)
    }
  }

  if (!editing) {
    return (
      <div className="flex items-center gap-2">
        <h1 className="text-2xl font-bold text-gray-800">{fullName}</h1>
        <button
          type="button"
          onClick={startEditing}
          className="text-xs text-gray-400 hover:text-blue-600 cursor-pointer"
        >
          Edit
        </button>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          autoFocus
          className="text-lg font-semibold text-gray-800 border border-gray-300 rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          type="button"
          onClick={save}
          disabled={saving}
          className="text-xs text-blue-600 hover:underline cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {saving ? 'Saving...' : 'Save'}
        </button>
        <button
          type="button"
          onClick={cancel}
          disabled={saving}
          className="text-xs text-gray-400 hover:text-red-500 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Cancel
        </button>
      </div>
      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
    </div>
  )
}

export default EditableFullName
