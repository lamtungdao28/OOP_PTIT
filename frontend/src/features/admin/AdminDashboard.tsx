import { useQuery } from '@tanstack/react-query'
import { BookOpenCheck, Building2, CircleUserRound, GraduationCap } from 'lucide-react'
import { Link } from 'react-router-dom'
import { DataTable } from '@/components/common/DataTable'
import { PageHeader, StatusBadge } from '@/components/common/Controls'
import { StatCard } from '@/components/common/StatCard'
import { ErrorState, LoadingSkeleton } from '@/components/common/States'
import { classService, lecturerService, studentService, subjectService } from '@/services'
import type { CourseClass } from '@/types/api'

export function AdminDashboard() {
  const students = useQuery({ queryKey: ['students', 0, 1000], queryFn: () => studentService.list(0, 1000) })
  const lecturers = useQuery({ queryKey: ['lecturers', 0, 1000], queryFn: () => lecturerService.list(0, 1000) })
  const subjects = useQuery({ queryKey: ['subjects', 0, 1000], queryFn: () => subjectService.list(0, 1000) })
  const classes = useQuery({ queryKey: ['classes', 0, 1000], queryFn: () => classService.list(0, 1000) })
  const loading = [students, lecturers, subjects, classes].some(q => q.isLoading)
  const error = [students, lecturers, subjects, classes].find(q => q.error)?.error
  const count = <T,>(page?: { items: T[]; totalPage: number }) => page ? `${page.items.length}${page.totalPage > 1 ? '+' : ''}` : 0
  return <div className="space-y-8"><PageHeader title="Tổng quan hệ thống" description="Theo dõi nhanh dữ liệu vận hành Smart Attendance."/>{loading ? <LoadingSkeleton rows={4}/> : error ? <ErrorState message="Không thể tải dữ liệu tổng quan."/> : <><div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-4"><StatCard label="Sinh viên" value={count(students.data)} icon={GraduationCap}/><StatCard label="Giảng viên" value={count(lecturers.data)} icon={CircleUserRound} variant="green"/><StatCard label="Học phần" value={count(subjects.data)} icon={BookOpenCheck} variant="orange"/><StatCard label="Lớp học" value={count(classes.data)} icon={Building2} variant="red"/></div><section><div className="mb-5 flex items-center justify-between"><h2 className="text-xl font-bold">Lớp học gần đây</h2><Link to="/admin/classes" className="text-sm font-bold text-brand">Xem tất cả →</Link></div><div className="panel"><DataTable<CourseClass> rows={(classes.data?.items ?? []).slice(0,5)} rowKey={r => r.id} columns={[{ key: 'code', title: 'Mã lớp', render: r => <b>{r.classCode}</b> }, { key: 'subject', title: 'Mã học phần', render: r => `#${r.subjectId}` }, { key: 'lecturer', title: 'Giảng viên', render: r => `#${r.lecturerId}` }, { key: 'semester', title: 'Học kỳ', render: r => `#${r.semesterId}` }, { key: 'status', title: 'Trạng thái', render: r => <StatusBadge status={r.status}/> }]}/></div></section></>}</div>
}
