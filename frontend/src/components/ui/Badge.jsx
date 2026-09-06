import { formatEnumLabel } from '../../utils/formatEnumLabel'
import { STATUS_STYLES, UNKNOWN_STATUS_STYLE } from './statusStyles'

// Reusable status pill. The colour comes from the shared map rather than a colour
// object passed per page, so one status cannot end up two colours on two screens.
function Badge({ status }) {
  const { tint, text } = STATUS_STYLES[status] ?? UNKNOWN_STATUS_STYLE

  return (
    <span
      className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
      style={{ backgroundColor: tint, color: text }}
    >
      {formatEnumLabel(status)}
    </span>
  )
}

export default Badge
