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
  xl: 'w-20 h-20 text-xl',
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

// Shared picture/logo fallback: a plain <img> when the URL loads, otherwise
// initials in a deterministic color circle instead of the broken-image icon.
//
// variant "avatar" (default) is for people — round, cropped to fill (object-cover).
// variant "logo" is for company logos — rounded square, whole logo visible
// (object-contain) on a padded white tile so wordmarks/wide marks aren't cut off.
function Avatar({ src, name, size = 'md', variant = 'avatar', className = '' }) {
  const [failed, setFailed] = useState(false)

  useEffect(() => setFailed(false), [src])

  const sizeClasses = SIZE_CLASSES[size] ?? SIZE_CLASSES.md
  const isLogo = variant === 'logo'
  const shapeClasses = isLogo ? 'rounded-lg' : 'rounded-full'

  if (src && !failed) {
    const imgFit = isLogo
      ? 'object-contain bg-white border border-gray-200 p-1'
      : 'object-cover'
    return (
      <img
        src={src}
        alt={name || 'Avatar'}
        onError={() => setFailed(true)}
        className={`${sizeClasses} ${shapeClasses} ${imgFit} shrink-0 ${className}`}
      />
    )
  }

  return (
    <div
      className={`${sizeClasses} ${shapeClasses} flex items-center justify-center font-semibold text-white shrink-0 ${className}`}
      style={{ backgroundColor: colorOf(name) }}
    >
      {initialsOf(name)}
    </div>
  )
}

export default Avatar
