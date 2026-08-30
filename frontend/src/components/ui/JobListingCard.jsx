import Card from './Card'
import Avatar from './Avatar'
import Badge from './Badge'
import { formatRelativeTime } from '../../utils/formatRelativeTime'
import { formatEnumLabel } from '../../utils/formatEnumLabel'

const CANDIDACY_STATUS_COLORS = {
  SUBMITTED: '#3b82f6',
  ACCEPTED: '#10b981',
  REJECTED: '#ef4444',
}

// Shared between BrowseListings and CandidateMatches - they differ only in
// which optional badges they pass (matchScore, myCandidacyStatus).
function JobListingCard({ listing, matchScore, onClick }) {
  return (
    <Card className="cursor-pointer hover:border-blue-300 transition-colors">
      <div onClick={onClick}>
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-base font-semibold text-gray-800">{listing.title}</h3>
          <div className="flex items-center gap-2 flex-wrap justify-end shrink-0">
            {matchScore != null && (
              <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">
                {Math.round(matchScore * 100)}% match
              </span>
            )}
            <span className="text-xs text-gray-500">{formatEnumLabel(listing.level)}</span>
            {listing.myCandidacyStatus && (
              <Badge status={listing.myCandidacyStatus} colors={CANDIDACY_STATUS_COLORS} />
            )}
          </div>
        </div>
        {listing.createdAt && (
          <p className="text-xs text-gray-400 mt-0.5">
            Posted {formatRelativeTime(listing.createdAt)}
          </p>
        )}
        <div className="flex items-center gap-2 mt-1">
          <Avatar src={listing.company?.logoUrl} name={listing.company?.name} size="md" variant="logo" />
          <p className="text-sm text-gray-500">
            {listing.company?.name} · {listing.location || 'No location listed'}
            {listing.remote ? ' · Remote' : ''}
          </p>
        </div>
        {listing.skills?.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-2">
            {listing.skills.map((skill) => (
              <span
                key={skill.id}
                className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full"
              >
                {skill.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </Card>
  )
}

export default JobListingCard
