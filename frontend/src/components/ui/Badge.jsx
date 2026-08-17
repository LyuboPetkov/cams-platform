// Generalizes the inline STATUS_COLORS pattern from Dashboard.jsx into something
// any status enum (CandidacyStatus, JobListingStatus, ...) can reuse by passing
// its own color map rather than duplicating a near-identical pill component.
function Badge({ status, colors = {} }) {
  const color = colors[status] ?? '#6b7280'

  return (
    <span
      className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
      style={{ backgroundColor: `${color}1a`, color }}
    >
      {status}
    </span>
  )
}

export default Badge
