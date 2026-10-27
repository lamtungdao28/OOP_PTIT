import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, LockKeyhole, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'
import { useAuth } from './AuthProvider'
import { getErrorMessage } from '@/services/apiClient'

const schema = z.object({ username: z.string().min(1, 'Vui lòng nhập tên đăng nhập'), password: z.string().min(1, 'Vui lòng nhập mật khẩu') })
type FormValues = z.infer<typeof schema>
const roleHome = { ADMIN: '/admin/dashboard', LECTURER: '/lecturer/dashboard', STUDENT: '/student/dashboard' } as const

export function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [showPassword, setShowPassword] = useState(false)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({ resolver: zodResolver(schema) })
  if (user) return <Navigate to={roleHome[user.roles[0]] || '/'} replace />

  const submit = handleSubmit(async (values) => {
    try {
      const response = await login(values.username, values.password)
      toast.success('Đăng nhập thành công')
      navigate(roleHome[response.user.roles[0]] || '/', { replace: true })
    } catch (error) { toast.error(getErrorMessage(error)) }
  })

  return <main className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_.95fr]">
    <section className="relative hidden overflow-hidden bg-navy p-14 text-white lg:flex lg:flex-col lg:justify-between">
      <div className="absolute -right-32 -top-28 h-96 w-96 rounded-full border-[80px] border-white/5" />
      <div className="relative flex items-center gap-3"><img src="/assets/ptit-logo.png" alt="PTIT" className="h-11 w-9 object-contain"/><span className="text-xl font-bold">Smart Attendance</span></div>
      <div className="relative max-w-xl pb-12"><p className="mb-5 text-sm font-bold uppercase tracking-[.22em] text-red-300">Học viện Công nghệ Bưu chính Viễn thông</p><h1 className="text-5xl font-bold leading-tight">Điểm danh thông minh,<br/>quản lý hiệu quả.</h1><p className="mt-6 max-w-lg text-lg leading-8 text-slate-300">Hệ thống quản lý lớp học và điểm danh sinh viên bằng nhận diện khuôn mặt.</p></div>
      <p className="relative text-sm text-slate-400">© 2026 Smart Attendance</p>
    </section>
    <section className="flex items-center justify-center bg-[#fcfdfe] px-6 py-12">
      <div className="w-full max-w-md">
        <div className="mb-10 lg:hidden"><span className="text-xl font-bold text-navy">Smart Attendance</span></div>
        <p className="text-sm font-bold uppercase tracking-wider text-brand">Chào mừng trở lại</p>
        <h2 className="mt-2 text-3xl font-bold text-ink">Đăng nhập hệ thống</h2>
        <p className="mt-3 text-muted">Sử dụng tài khoản được cấp để tiếp tục.</p>
        <form onSubmit={submit} className="mt-9 space-y-5" noValidate>
          <label className="block"><span className="label">Tên đăng nhập</span><div className="relative"><UserRound className="absolute left-3 top-3 h-5 w-5 text-slate-400"/><input className="field pl-11" placeholder="Nhập tên đăng nhập" autoComplete="username" {...register('username')} /></div>{errors.username && <span className="mt-1 block text-xs text-brand">{errors.username.message}</span>}</label>
          <label className="block"><span className="label">Mật khẩu</span><div className="relative"><LockKeyhole className="absolute left-3 top-3 h-5 w-5 text-slate-400"/><input className="field px-11" type={showPassword ? 'text' : 'password'} placeholder="Nhập mật khẩu" autoComplete="current-password" {...register('password')} /><button type="button" className="absolute right-3 top-2.5 p-1 text-slate-400" onClick={() => setShowPassword(v => !v)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button></div>{errors.password && <span className="mt-1 block text-xs text-brand">{errors.password.message}</span>}</label>
          <button className="btn-primary h-12 w-full" disabled={isSubmitting}>{isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
        </form>
      </div>
    </section>
  </main>
}
