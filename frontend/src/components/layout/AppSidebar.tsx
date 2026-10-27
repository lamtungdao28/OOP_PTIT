import { BarChart3, BookOpen, CalendarDays, Camera, ChevronLeft, ChevronRight, ClipboardCheck, GraduationCap, History, LayoutDashboard, School, UserRound, UsersRound, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'
import type { Role } from '@/types/api'

const iconMap = { dashboard: LayoutDashboard, students: GraduationCap, lecturers: UsersRound, subjects: BookOpen, semesters: CalendarDays, classes: School, sessions: CalendarDays, attendance: ClipboardCheck, history: History, reports: BarChart3, face: Camera, profile: UserRound }
const menus: Record<Role, { label: string; to: string; icon: keyof typeof iconMap }[]> = {
  ADMIN: [
    { label: 'Tổng quan', to: '/admin/dashboard', icon: 'dashboard' }, { label: 'Sinh viên', to: '/admin/students', icon: 'students' }, { label: 'Giảng viên', to: '/admin/lecturers', icon: 'lecturers' }, { label: 'Học phần', to: '/admin/subjects', icon: 'subjects' }, { label: 'Học kỳ', to: '/admin/semesters', icon: 'semesters' }, { label: 'Lớp học phần', to: '/admin/classes', icon: 'classes' },
  ],
  LECTURER: [
    { label: 'Tổng quan', to: '/lecturer/dashboard', icon: 'dashboard' }, { label: 'Lớp phụ trách', to: '/lecturer/classes', icon: 'classes' }, { label: 'Buổi học', to: '/lecturer/sessions', icon: 'sessions' }, { label: 'Điểm danh', to: '/lecturer/attendance', icon: 'attendance' }, { label: 'Lịch sử điểm danh', to: '/lecturer/history', icon: 'history' }, { label: 'Thống kê', to: '/lecturer/statistics', icon: 'reports' },
  ],
  STUDENT: [
    { label: 'Tổng quan', to: '/student/dashboard', icon: 'dashboard' }, { label: 'Lớp học của tôi', to: '/student/classes', icon: 'classes' }, { label: 'Đăng ký khuôn mặt', to: '/student/face-registration', icon: 'face' }, { label: 'Lịch sử điểm danh', to: '/student/attendance-history', icon: 'history' }, { label: 'Hồ sơ cá nhân', to: '/student/profile', icon: 'profile' },
  ],
}

export function AppSidebar({ role, collapsed, mobileOpen, onCollapse, onMobileClose }: { role: Role; collapsed: boolean; mobileOpen: boolean; onCollapse: () => void; onMobileClose: () => void }) {
  return <aside className={cn('fixed inset-y-0 left-0 z-40 m-0 flex flex-col overflow-hidden bg-navy text-white transition-all md:sticky md:top-0 md:m-6 md:mr-0 md:h-[calc(100vh-3rem)] md:rounded-lg', collapsed ? 'w-[84px]' : 'w-[248px]', mobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0')}>
    <div className="flex h-24 items-center gap-3 px-5"><img src="/assets/ptit-logo.png" alt="PTIT" className="h-[34px] w-7 shrink-0 object-contain"/>{!collapsed && <span className="truncate font-bold">Smart Attendance</span>}<button className="ml-auto md:hidden" onClick={onMobileClose}><X/></button></div>
    <nav className="flex-1 space-y-2 px-4">{menus[role].map(item => { const Icon = iconMap[item.icon]; return <NavLink key={item.to} to={item.to} onClick={onMobileClose} className={({ isActive }) => cn('flex h-[52px] items-center gap-4 rounded-md px-4 text-[15px] font-medium transition', isActive ? 'bg-brand text-white' : 'text-[#B8C4CE] hover:bg-white/7 hover:text-white')} title={collapsed ? item.label : undefined}><Icon className="h-[22px] w-[22px] shrink-0"/>{!collapsed && <span>{item.label}</span>}</NavLink>})}</nav>
    <button onClick={onCollapse} className="m-4 hidden h-11 items-center justify-center rounded-md border border-white/10 text-slate-300 hover:bg-white/5 md:flex">{collapsed ? <ChevronRight/> : <><ChevronLeft/><span className="ml-2 text-sm">Thu gọn</span></>}</button>
  </aside>
}
