function LoadingSpinner({ className = '' }) {
  return (
    <div className={`flex items-center justify-center p-6 ${className}`}>
      <div className="w-6 h-6 border-2 border-gray-200 border-t-blue-600 rounded-full animate-spin" />
    </div>
  )
}

export default LoadingSpinner
