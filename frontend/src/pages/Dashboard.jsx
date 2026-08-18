import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getApplications } from '../api/applications'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts'

const STATUS_COLORS = {
  APPLIED: '#3b82f6',
  INTERVIEWING: '#f59e0b',
  OFFERED: '#10b981',
  REJECTED: '#ef4444',
  WITHDRAWN: '#6b7280'
}

const SOURCE_COLOR = '#3b82f6'

function computeStats(applications) {
  const statusCounts = applications.reduce((acc, app) => {
    acc[app.status] = (acc[app.status] || 0) + 1
    return acc
  }, {})

  const sourceCounts = applications.reduce((acc, app) => {
    acc[app.source] = (acc[app.source] || 0) + 1
    return acc
  }, {})

  const timelineCounts = applications.reduce((acc, app) => {
    const month = app.appliedDate.slice(0, 7)
    acc[month] = (acc[month] || 0) + 1
    return acc
  }, {})

  const statusData = Object.entries(statusCounts).map(([name, value]) => ({ name, value }))
  const sourceData = Object.entries(sourceCounts).map(([name, value]) => ({ name, value }))
  const timelineData = Object.entries(timelineCounts)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([name, value]) => ({ name, value }))

  return { statusData, sourceData, timelineData }
}

function Dashboard() {
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

    const navigate = useNavigate()

  useEffect(() => {
    getApplications({})
      .then(response => {
        setApplications(response.data)
      })
      .catch(() => {
        setError('Failed to load applications.')
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  if (loading) return <div className="p-6">Loading...</div>
  if (error) return <div className="p-6 text-red-500">{error}</div>

  const { statusData, sourceData, timelineData } = computeStats(applications)

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-5xl mx-auto px-6 py-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-8">Dashboard</h1>

        {/* Summary counts */}
        <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-4 mb-10">
          <div className="bg-white rounded-lg border border-gray-200 p-5">
            <p className="text-sm text-gray-500">Total Applications</p>
            <p className="text-3xl font-bold text-gray-800 mt-1">{applications.length}</p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-5">
            <p className="text-sm text-gray-500">Interviews</p>
            <p className="text-3xl font-bold text-amber-500 mt-1">
              {statusData.find(s => s.name === 'INTERVIEWING')?.value ?? 0}
            </p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-5">
            <p className="text-sm text-gray-500">Offers</p>
            <p className="text-3xl font-bold text-emerald-500 mt-1">
              {statusData.find(s => s.name === 'OFFERED')?.value ?? 0}
            </p>
          </div>
        </div>
        {/* Charts */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">

          {/* Status pie chart */}
          <div className="bg-white rounded-lg border border-gray-200 p-5">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">By Status</h2>
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                    data={statusData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    outerRadius={90}
                    label={({ name, value }) => `${name} (${value})`}
                    onClick={(entry) => navigate('/applications', { state: { status: entry.name } })}
                    style={{ cursor: 'pointer' }}
                >
                  {statusData.map(entry => (
                    <Cell key={entry.name} fill={STATUS_COLORS[entry.name] ?? '#cbd5e1'} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* Source bar chart */}
          <div className="bg-white rounded-lg border border-gray-200 p-5">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">By Source</h2>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={sourceData} margin={{ top: 5, right: 10, left: 0, bottom: 60 }}>
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: 11 }}
                  angle={-35}
                  textAnchor="end"
                  interval={0}
                />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar
                    dataKey="value"
                    fill={SOURCE_COLOR}
                    radius={[4, 4, 0, 0]}
                    onClick={(entry) => navigate('/applications', { state: { source: entry.name } })}
                    style={{ cursor: 'pointer' }}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>

        </div>

        {/* Timeline bar chart — full width */}
        <div className="bg-white rounded-lg border border-gray-200 p-5">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Applications Over Time</h2>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={timelineData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="value" fill={SOURCE_COLOR} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

      </div>
    </div>
  )
}

export default Dashboard