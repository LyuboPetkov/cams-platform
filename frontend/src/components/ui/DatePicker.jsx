import { useMemo } from 'react'
import ReactDatePicker from 'react-datepicker'
import './datePicker.css'

// Shared input styling, copied from the plain <input> classes used across the
// forms so the trigger field is indistinguishable from its neighbours.
const INPUT_CLASS =
  'w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none ' +
  'focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-400'

// The app passes dates as 'YYYY-MM-DD' strings (what the API uses); react-datepicker
// works in Date objects, so this wrapper converts on the way in and out.
function parseValue(value) {
  if (!value) return null
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month) return null
  return new Date(year, month - 1, day || 1)
}

function formatValue(date) {
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// mode="day"   -> full calendar, emits 'YYYY-MM-DD'
// mode="month" -> month + year grid, emits 'YYYY-MM-01'
function DatePicker({
  value,
  onChange,
  mode = 'day',
  min,
  max,
  isClearable = false,
  placeholder,
  disabled = false,
  required = false,
  id,
  name,
}) {
  const isMonth = mode === 'month'

  // Memoise the Date instances - react-datepicker keys internal effects on these
  // props, so a fresh Date every render loops it while the calendar is open.
  const selected = useMemo(() => parseValue(value), [value])
  const minDate = useMemo(() => parseValue(min), [min])
  const maxDate = useMemo(() => parseValue(max), [max])

  // react-datepicker renders the calendar popper and its focus-trap element as
  // siblings of the input wrapper. This block wrapper keeps them from becoming
  // extra grid/flex items in the caller's layout when the calendar opens, which
  // otherwise pushed the next field onto a new row.
  return (
    <div className="w-full">
      <ReactDatePicker
        selected={selected}
        onChange={(date) => onChange(formatValue(date))}
        calendarStartDay={1}
        showMonthYearPicker={isMonth}
        dateFormat={isMonth ? 'MMMM yyyy' : 'dd MMM yyyy'}
        minDate={minDate}
        maxDate={maxDate}
        isClearable={isClearable && !disabled}
        placeholderText={placeholder}
        disabled={disabled}
        required={required}
        id={id}
        name={name}
        autoComplete="off"
        showPopperArrow={false}
        popperPlacement="bottom-start"
        className={INPUT_CLASS}
        wrapperClassName="w-full"
      />
    </div>
  )
}

export default DatePicker
