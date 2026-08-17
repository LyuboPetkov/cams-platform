import { useState, useEffect } from 'react'

// Same small fixed palette approach as Badge's status-color map: deterministic
// per name, no external color library needed.
const PALETTE = [
  '#2563eb', '#7c3aed', '#db2777', '#dc2626', '#d97706',
  '#65a30d', '#059669', '#0891b2', '#4f46e5', '#c026d3',
]

const SIZE_CLASSES = {
  sm: 'w-8 h-8 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-16 h-16 text-lg',
}

function initialsOf(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase() || '?'
}

function colorOf(name) {
  let hash = 0
  for (const char of name || '') {
    hash = char.charCodeAt(0) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
}

// Shared fallback for every picture/logo shown in the app: a plain <img> when
// a URL is set and loads, otherwise initials in a deterministic color circle
// rather than the browser's broken-image icon (unset and dead URLs are both
// the common case outside the owner's own edit page, not the exception).
function Avatar({ src, name, size = 'md', className = '' }) {
  const [failed, setFailed] = useState(false)

  useEffect(() => setFailed(false), [src])

  const sizeClasses = SIZE_CLASSES[size] ?? SIZE_CLASSES.md

  if (src && !failed) {
    return (
      <img
        src={src}
        alt={name || 'Avatar'}
        onError={() => setFailed(true)}
        className={`${sizeClasses} rounded-full object-cover shrink-0 ${className}`}
      />
    )
  }

  return (
    <div
      className={`${sizeClasses} rounded-full flex items-center justify-center font-semibold text-white shrink-0 ${className}`}
      style={{ backgroundColor: colorOf(name) }}
    >
      {initialsOf(name)}
    </div>
  )
}

export default Avatar
