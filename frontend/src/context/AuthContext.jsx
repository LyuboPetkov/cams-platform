import {createContext, useContext, useState, useEffect} from 'react'


const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [initializing, setInitializing] = useState(true)

useEffect(() => {
  const token = localStorage.getItem('token')
  const email = localStorage.getItem('email')
  const fullName = localStorage.getItem('fullName')
  const role = localStorage.getItem('role')
  const pictureUrl = localStorage.getItem('pictureUrl') || null
  if (token) {
    setUser({ token, email, fullName, role, pictureUrl })
  }
  setInitializing(false)
}, [])


    function login(data) {
        localStorage.setItem('token', data.token)
        localStorage.setItem('email', data.email)
        localStorage.setItem('fullName', data.fullName)
        localStorage.setItem('role', data.role)
        localStorage.setItem('pictureUrl', data.pictureUrl ?? '')
        setUser({
            token: data.token,
            email: data.email,
            fullName: data.fullName,
            role: data.role,
            pictureUrl: data.pictureUrl ?? null,
        })
    }

    function updateUserPicture(url) {
        localStorage.setItem('pictureUrl', url ?? '')
        setUser((prev) => (prev ? { ...prev, pictureUrl: url } : prev))
    }

    function updateUserFullName(fullName) {
        localStorage.setItem('fullName', fullName)
        setUser((prev) => (prev ? { ...prev, fullName } : prev))
    }

    function logout() {
        localStorage.removeItem('token')
        localStorage.removeItem('email')
        localStorage.removeItem('fullName')
        localStorage.removeItem('role')
        localStorage.removeItem('pictureUrl')
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, login, logout, updateUserPicture, updateUserFullName, initializing }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
