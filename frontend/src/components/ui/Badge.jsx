import { formatEnumLabel } from '../../utils/formatEnumLabel'

// Reusable status pill: any status enum passes its own color map, rather than
// each page duplicating the STATUS_COLORS pattern from Dashboard.jsx.

function Badge({ status, colors = {} }) {
  const color = colors[status] ?? '#6b7280'

  return (
    <span
      className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
      style={{ backgroundColor: `${color}1a`, color }}
    >
      {formatEnumLabel(status)}
    </span>
  )
}

export default Badge
