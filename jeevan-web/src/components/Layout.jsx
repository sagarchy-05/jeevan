import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import VerifyBanner from './VerifyBanner'

export default function Layout() {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  const onLogout = () => {
    logout()
    navigate('/login')
  }

  const linkClass = ({ isActive }) =>
    `px-3 py-2 rounded-md text-sm ${
      isActive ? 'bg-slate-200 text-slate-900' : 'text-slate-600 hover:text-slate-900'
    }`

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/doctors" className="text-lg font-semibold text-slate-900">
            Jeevan
          </Link>
          <nav className="flex items-center gap-1">
            <NavLink to="/doctors" className={linkClass}>
              Doctors
            </NavLink>
            {isAuthenticated && (
              <NavLink to="/appointments" className={linkClass}>
                My Appointments
              </NavLink>
            )}
            {isAuthenticated ? (
              <>
                <span className="px-3 text-sm text-slate-500">{user?.fullName}</span>
                <button
                  onClick={onLogout}
                  className="px-3 py-2 text-sm text-slate-600 hover:text-slate-900"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={linkClass}>
                  Login
                </NavLink>
                <NavLink to="/register" className={linkClass}>
                  Register
                </NavLink>
              </>
            )}
          </nav>
        </div>
      </header>
      <VerifyBanner />
      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
