import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth/AuthProvider'
import type { Role } from '@/types/api'
import { LoadingSkeleton } from '@/components/common/States'

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  if (loading) return <div className="p-10"><LoadingSkeleton rows={6}/></div>
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

export function RoleGuard({ role }: { role: Role }) {
  const { hasRole } = useAuth()
  return hasRole(role) ? <Outlet /> : <Navigate to="/forbidden" replace />
}
