import { useState, useEffect } from 'react'
import { searchSkills } from '../../api/skills'

const DEBOUNCE_MS = 300
const MIN_CHARS = 2
const RESULT_LIMIT = 8

function SkillTypeahead({ excludeIds = [], onSelect, placeholder = 'Search skills' }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (query.trim().length < MIN_CHARS) {
      setResults([])
      setOpen(false)
      return
    }

    const timer = setTimeout(() => {
      searchSkills(query.trim(), RESULT_LIMIT)
        .then((response) => {
          const filtered = response.data.filter((skill) => !excludeIds.includes(skill.id))
          setResults(filtered)
          setOpen(filtered.length > 0)
        })
        .catch(() => {
          setResults([])
          setOpen(false)
        })
    }, DEBOUNCE_MS)

    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query])

  function handleSelect(skill) {
    onSelect(skill)
    setQuery('')
    setResults([])
    setOpen(false)
  }

  return (
    <div className="relative">
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => setOpen(results.length > 0)}
        onBlur={() => setOpen(false)}
        placeholder={placeholder}
        className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      {open && (
        <div className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded shadow-lg max-h-64 overflow-auto">
          {results.map((skill) => (
            <button
              key={skill.id}
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => handleSelect(skill)}
              className="block w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 cursor-pointer"
            >
              {skill.name}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default SkillTypeahead
