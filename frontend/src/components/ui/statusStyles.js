// One place for every status pill in the app. Keyed by status value across all the
// status enums - their values don't collide, and equivalent stages deliberately share
// a colour so the same reality reads the same way on every screen.
//
// tint/text are the pill's background and label: a dark label on a pale tint, because
// the mid-tone hue used for both was too light to read. chart is the saturated hue,
// for Recharts fills where the colour is a fill rather than text.
export const STATUS_STYLES = {
  // ApplicationStatus - the candidate's own tracker
  APPLIED:      { tint: '#dbeafe', text: '#1d4ed8', chart: '#3b82f6' },
  INTERVIEWING: { tint: '#fef9c3', text: '#a16207', chart: '#f59e0b' },
  WITHDRAWN:    { tint: '#f3f4f6', text: '#374151', chart: '#6b7280' },

  // CandidacyStatus - candidacies inside the platform
  SUBMITTED:    { tint: '#dbeafe', text: '#1d4ed8', chart: '#3b82f6' },
  ACCEPTED:     { tint: '#fef9c3', text: '#a16207', chart: '#f59e0b' },

  // shared by both lifecycles
  OFFERED:      { tint: '#d1fae5', text: '#047857', chart: '#10b981' },
  REJECTED:     { tint: '#fee2e2', text: '#b91c1c', chart: '#ef4444' },

  // EmployerRequestStatus
  PENDING:      { tint: '#fef9c3', text: '#a16207', chart: '#f59e0b' },
  APPROVED:     { tint: '#d1fae5', text: '#047857', chart: '#10b981' },

  // JobListingStatus
  OPEN:         { tint: '#d1fae5', text: '#047857', chart: '#10b981' },
  ARCHIVED:     { tint: '#f3f4f6', text: '#374151', chart: '#6b7280' },
}

export const UNKNOWN_STATUS_STYLE = { tint: '#f3f4f6', text: '#374151', chart: '#6b7280' }
