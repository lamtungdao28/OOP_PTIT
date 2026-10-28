import { ChevronLeft, ChevronRight, Search, X } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export function PageHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><div><h1 className="text-2xl font-bold text-ink md:text-[28px]">{title}</h1>{description && <p className="mt-1 text-sm text-muted">{description}</p>}</div>{action}</div>
}

export function SearchInput({ value, onChange, placeholder = 'Tìm kiếm…' }: { value: string; onChange: (v: string) => void; placeholder?: string }) {
  return <div className="relative w-full sm:w-96"><Search className="pointer-events-none absolute left-3 top-2.5 h-5 w-5 text-slate-400"/><input aria-label={placeholder} className="field px-10" type="search" maxLength={100} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder}/>{value && <button type="button" className="absolute right-2 top-1.5 rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700" onClick={() => onChange('')} aria-label="Xóa từ khóa tìm kiếm"><X size={17}/></button>}</div>
}

export function StatusBadge({ status }: { status?: string | null }) {
  const key = status || 'UNKNOWN'
  const styles: Record<string, string> = { ACTIVE: 'bg-emerald-50 text-emerald-700', OPEN: 'bg-emerald-50 text-emerald-700', PRESENT: 'bg-emerald-50 text-emerald-700', LATE: 'bg-amber-50 text-amber-700', ABSENT: 'bg-red-50 text-red-700', CLOSED: 'bg-slate-100 text-slate-600', INACTIVE: 'bg-slate-100 text-slate-600', LOCKED: 'bg-red-50 text-red-700' }
  const labels: Record<string, string> = { ACTIVE: 'Hoạt động', OPEN: 'Đang mở', PRESENT: 'Có mặt', LATE: 'Đi muộn', ABSENT: 'Vắng mặt', CLOSED: 'Đã đóng', INACTIVE: 'Ngừng hoạt động', LOCKED: 'Đã khóa', UNKNOWN: 'Chưa xác định' }
  return <span className={cn('inline-flex rounded-full px-2.5 py-1 text-xs font-bold', styles[key] || styles.INACTIVE)}>{labels[key] || key}</span>
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  return <div className="flex items-center justify-between border-t px-5 py-4 text-sm"><span className="text-muted">Trang {page + 1} / {Math.max(totalPages, 1)}</span><div className="flex gap-2"><button className="btn-secondary h-9 px-3" disabled={page <= 0} onClick={() => onChange(page - 1)}><ChevronLeft size={16}/>Trước</button><button className="btn-secondary h-9 px-3" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Sau<ChevronRight size={16}/></button></div></div>
}
