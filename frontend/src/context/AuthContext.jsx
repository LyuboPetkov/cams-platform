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
  const hasPendingEmployerRequest = localStorage.getItem('hasPendingEmployerRequest') === 'true'
  if (token) {
    setUser({ token, email, fullName, role, hasPendingEmployerRequest })
  }
  setInitializing(false)
}, [])


    function login(data) {
        localStorage.setItem('token', data.token)
        localStorage.setItem('email', data.email)
        localStorage.setItem('fullName', data.fullName)
        localStorage.setItem('role', data.role)
        localStorage.setItem('hasPendingEmployerRequest', String(Boolean(data.hasPendingEmployerRequest)))
        setUser({
            token: data.token,
            email: data.email,
            fullName: data.fullName,
            role: data.role,
            hasPendingEmployerRequest: Boolean(data.hasPendingEmployerRequest),
        })
    }

    function logout() {
        localStorage.removeItem('token')
        localStorage.removeItem('email')
        localStorage.removeItem('fullName')
        localStorage.removeItem('role')
        localStorage.removeItem('hasPendingEmployerRequest')
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, login, logout, initializing }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
