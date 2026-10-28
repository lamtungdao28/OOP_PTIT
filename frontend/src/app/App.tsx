import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { AuthProvider, useAuth } from '@/features/auth/AuthProvider'
import { LoginPage } from '@/features/auth/LoginPage'
import { AdminDashboard } from '@/features/admin/AdminDashboard'
import { ClassesPage } from '@/features/admin/ClassesPage'
import { LecturersPage, SemestersPage, StudentsPage, SubjectsPage } from '@/features/admin/EntityPages'
import { AttendancePage, LecturerClassesPage, LecturerDashboard, SessionsPage } from '@/features/lecturer/LecturerPages'
import { AttendanceHistoryPage, FaceRegistrationPage, ProfilePage, StudentClassesPage, StudentDashboard } from '@/features/student/StudentPages'
import { ProtectedRoute, RoleGuard } from '@/routes/guards'

const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1 } } })
const roleHome = { ADMIN: '/admin/dashboard', LECTURER: '/lecturer/dashboard', STUDENT: '/student/dashboard' } as const

function HomeRedirect(){const{user}=useAuth();return <Navigate to={user?roleHome[user.roles[0]]:'/login'} replace/>}
function ErrorPage({ forbidden=false }:{forbidden?:boolean}){return <main className="grid min-h-screen place-items-center bg-canvas p-6 text-center"><div><p className="text-7xl font-bold text-brand">{forbidden?'403':'404'}</p><h1 className="mt-4 text-2xl font-bold">{forbidden?'Bạn không có quyền truy cập':'Không tìm thấy trang'}</h1><a href="/" className="btn-primary mt-6">Về trang chính</a></div></main>}

export default function App(){return <QueryClientProvider client={queryClient}><BrowserRouter><AuthProvider><Routes><Route path="/login" element={<LoginPage/>}/><Route element={<ProtectedRoute/>}><Route path="/" element={<HomeRedirect/>}/><Route element={<DashboardLayout/>}><Route element={<RoleGuard role="ADMIN"/>}><Route path="/admin/dashboard" element={<AdminDashboard/>}/><Route path="/admin/students" element={<StudentsPage/>}/><Route path="/admin/lecturers" element={<LecturersPage/>}/><Route path="/admin/subjects" element={<SubjectsPage/>}/><Route path="/admin/semesters" element={<SemestersPage/>}/><Route path="/admin/classes" element={<ClassesPage/>}/></Route><Route element={<RoleGuard role="LECTURER"/>}><Route path="/lecturer/dashboard" element={<LecturerDashboard/>}/><Route path="/lecturer/classes" element={<LecturerClassesPage/>}/><Route path="/lecturer/sessions" element={<SessionsPage/>}/><Route path="/lecturer/attendance" element={<AttendancePage/>}/></Route><Route element={<RoleGuard role="STUDENT"/>}><Route path="/student/dashboard" element={<StudentDashboard/>}/><Route path="/student/classes" element={<StudentClassesPage/>}/><Route path="/student/face-registration" element={<FaceRegistrationPage/>}/><Route path="/student/attendance-history" element={<AttendanceHistoryPage/>}/><Route path="/student/profile" element={<ProfilePage/>}/></Route></Route></Route><Route path="/forbidden" element={<ErrorPage forbidden/>}/><Route path="*" element={<ErrorPage/>}/></Routes><Toaster richColors position="top-right"/></AuthProvider></BrowserRouter></QueryClientProvider>}
