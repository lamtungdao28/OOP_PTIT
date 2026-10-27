import { Bell, ChevronDown, LogOut, Menu } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/features/auth/AuthProvider'

const roleLabel = { ADMIN: 'Quản trị viên', LECTURER: 'Giảng viên', STUDENT: 'Sinh viên' }
export function AppHeader({ onMenu }: { onMenu: () => void }) {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const role = user?.roles[0]
  return <header className="flex h-[72px] shrink-0 items-center justify-between border-b bg-white px-5 md:justify-end md:px-8">
    <button className="md:hidden" onClick={onMenu}><Menu/></button>
    <div className="flex items-center gap-5"><div className="relative"><Bell className="h-5 w-5 text-slate-600"/><span className="absolute -right-0.5 -top-1 h-2 w-2 rounded-full bg-brand"/></div><div className="relative"><button onClick={() => setOpen(v => !v)} className="flex items-center gap-3"><img src="/assets/admin-avatar.jpeg" alt="Ảnh đại diện" className="h-10 w-10 rounded-full object-cover"/><div className="hidden min-w-24 text-left sm:block"><p className="text-sm font-bold text-slate-700">{user?.username}</p><p className="text-xs text-muted">{role ? roleLabel[role] : ''}</p></div><ChevronDown size={16} className="text-slate-400"/></button>{open && <div className="absolute right-0 top-12 z-50 w-56 rounded-lg border bg-white p-2 shadow-xl"><div className="border-b px-3 py-2"><p className="truncate text-sm font-bold">{user?.username}</p><p className="truncate text-xs text-muted">{user?.email}</p></div><button onClick={logout} className="mt-1 flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-red-600 hover:bg-red-50"><LogOut size={16}/>Đăng xuất</button></div>}</div></div>
  </header>
}
