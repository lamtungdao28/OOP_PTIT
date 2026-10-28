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

  return <main className="min-h-screen bg-white">
    <div className="min-h-screen w-full overflow-hidden bg-white">
      <header className="flex h-16 items-center bg-[#f8fafc] px-6 sm:px-8">
        <div className="flex items-center gap-3">
          <img src="/assets/ptit-logo.png" alt="PTIT" className="h-9 w-7 object-contain"/>
          <span className="text-[15px] font-bold text-slate-800">Smart Attendance</span>
        </div>
      </header>

      <div className="grid min-h-[calc(100vh-4rem)] lg:grid-cols-2">
        <section className="flex items-center justify-center px-6 py-12 sm:px-12 lg:px-16">
          <div className="w-full max-w-[420px]">
            <div className="text-center">
              <img src="/assets/ptit-logo.png" alt="Logo Học viện Công nghệ Bưu chính Viễn thông" className="mx-auto h-24 w-24 object-contain"/>
              <h1 className="mt-3 text-2xl font-bold text-slate-900">PTIT Smart Attendance</h1>
              <p className="mt-2 text-sm text-slate-500">Hệ thống điểm danh thông minh</p>
            </div>

            <form onSubmit={submit} className="mt-9 space-y-5" noValidate>
              <label className="block">
                <span className="sr-only">Tên đăng nhập</span>
                <div className="relative">
                  <UserRound className="pointer-events-none absolute left-4 top-3.5 h-5 w-5 text-slate-500"/>
                  <input className="field h-12 pl-12" placeholder="Tên đăng nhập" aria-invalid={Boolean(errors.username)} autoComplete="username" {...register('username')} />
                </div>
                {errors.username && <span className="mt-1.5 block text-xs text-brand">{errors.username.message}</span>}
              </label>

              <label className="block">
                <span className="sr-only">Mật khẩu</span>
                <div className="relative">
                  <LockKeyhole className="pointer-events-none absolute left-4 top-3.5 h-5 w-5 text-slate-500"/>
                  <input className="field h-12 px-12" type={showPassword ? 'text' : 'password'} placeholder="Mật khẩu" aria-invalid={Boolean(errors.password)} autoComplete="current-password" {...register('password')} />
                  <button type="button" className="absolute right-3 top-2.5 rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" onClick={() => setShowPassword(v => !v)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}</button>
                </div>
                {errors.password && <span className="mt-1.5 block text-xs text-brand">{errors.password.message}</span>}
              </label>

              <button className="btn-primary h-12 w-full rounded-md" disabled={isSubmitting}>{isSubmitting ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
              <button type="button" className="text-sm text-slate-500 transition hover:text-brand" onClick={() => toast.info('Vui lòng liên hệ quản trị viên để được cấp lại mật khẩu.')}>Quên mật khẩu?</button>
            </form>
          </div>
        </section>

        <section className="relative hidden min-h-[calc(100vh-4rem)] overflow-hidden lg:block">
          <img src="/assets/ptit-campus.png" alt="Tòa nhà Học viện Công nghệ Bưu chính Viễn thông" className="absolute inset-0 h-full w-full object-cover"/>
          <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-900/15 to-transparent"/>
          <div className="absolute inset-x-0 bottom-0 p-10 text-white xl:p-12">
            <h2 className="text-3xl font-bold">Chào mừng trở lại</h2>
            <p className="mt-4 max-w-md text-base leading-7 text-slate-100">Đăng nhập để tiếp tục sử dụng<br/>hệ thống điểm danh thông minh của PTIT.</p>
          </div>
        </section>
      </div>
    </div>
  </main>
}
