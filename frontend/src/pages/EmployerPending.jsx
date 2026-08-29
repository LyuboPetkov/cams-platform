import { Link } from 'react-router-dom'

// Public, unauthenticated confirmation page - a just-registered or resubmitted
// employer applicant has no token yet, so there's no logged-in user to read.
function EmployerPending() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-6">
      <div className="bg-white p-8 rounded-lg border border-gray-200 max-w-md text-center">
        <h1 className="text-xl font-bold text-gray-800 mb-2">
          Your employer registration is pending review
        </h1>
        <p className="text-sm text-gray-500 mb-6">
          An admin needs to approve your company before you can sign in and post job
          listings. There's nothing else to do right now - check back later.
        </p>
        <Link to="/login" className="text-sm text-blue-600 hover:underline cursor-pointer">
          Back to sign in
        </Link>
      </div>
    </div>
  )
}

export default EmployerPending
