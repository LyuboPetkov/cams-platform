function EmptyState({ title, description, action }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-10 text-center">
      <p className="text-sm font-medium text-gray-700">{title}</p>
      {description && (
        <p className="text-sm text-gray-500 mt-1">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export default EmptyState
