import { useState, useEffect } from 'react'

const VISIBLE_MS = 2000

// `trigger` is any value that changes each time a save succeeds (e.g. a timestamp),
// so the indicator re-shows even if it's already visible from a previous save.
function SavedIndicator({ trigger }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (trigger == null) return
    setVisible(true)
    const timer = setTimeout(() => setVisible(false), VISIBLE_MS)
    return () => clearTimeout(timer)
  }, [trigger])

  if (!visible) return null

  return <span className="text-sm text-gray-500">Saved</span>
}

export default SavedIndicator
