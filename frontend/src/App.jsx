import { Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import EmployerPending from './pages/EmployerPending'
import ApplicationsList from './pages/ApplicationsList'
import CreateApplication from './pages/CreateApplication'
import EditApplication from './pages/EditApplication'
import Dashboard from './pages/Dashboard'
import CandidateProfile from './pages/CandidateProfile'
import BrowseListings from './pages/BrowseListings'
import ListingDetail from './pages/ListingDetail'
import MyCandidacies from './pages/MyCandidacies'
import CandidateMatches from './pages/CandidateMatches'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/employer-pending"
        element={
          // Not role-gated to EMPLOYER: a just-registered pending employer is
          // still role=CANDIDATE until an admin approves them (see
          // EmployerRequestService.approve), so this route has to be reachable
          // by both that state and an already-approved EMPLOYER.
          <ProtectedRoute>
            <EmployerPending />
          </ProtectedRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications"
        element={
          <ProtectedRoute>
            <ApplicationsList />
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications/new"
        element={
          <ProtectedRoute>
            <CreateApplication />
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications/:id/edit"
        element={
          <ProtectedRoute>
            <EditApplication />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute role="CANDIDATE">
            <CandidateProfile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/jobs"
        element={
          <ProtectedRoute role="CANDIDATE">
            <BrowseListings />
          </ProtectedRoute>
        }
      />
      <Route
        path="/jobs/:id"
        element={
          <ProtectedRoute role="CANDIDATE">
            <ListingDetail />
          </ProtectedRoute>
        }
      />
      <Route
        path="/candidacies"
        element={
          <ProtectedRoute role="CANDIDATE">
            <MyCandidacies />
          </ProtectedRoute>
        }
      />
      <Route
        path="/matches"
        element={
          <ProtectedRoute role="CANDIDATE">
            <CandidateMatches />
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

export default App
