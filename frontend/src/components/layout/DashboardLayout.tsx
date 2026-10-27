import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuth } from '@/features/auth/AuthProvider'
import { AppHeader } from './AppHeader'
import { AppSidebar } from './AppSidebar'

export function DashboardLayout() {
  const { user } = useAuth()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  if (!user) return null
  return <div className="min-h-screen bg-canvas md:flex"><AppSidebar role={user.roles[0]} collapsed={collapsed} mobileOpen={mobileOpen} onCollapse={() => setCollapsed(v => !v)} onMobileClose={() => setMobileOpen(false)}/>{mobileOpen && <button aria-label="Đóng menu" className="fixed inset-0 z-30 bg-black/30 md:hidden" onClick={() => setMobileOpen(false)}/>}<div className="min-w-0 flex-1 md:m-6 md:ml-0 md:overflow-hidden md:rounded-r-lg"><div className="flex min-h-screen flex-col bg-[#fcfdfe] md:min-h-[calc(100vh-3rem)]"><AppHeader onMenu={() => setMobileOpen(true)}/><main className="flex-1 p-5 md:p-8 lg:p-10"><Outlet/></main></div></div></div>
}
