import { Inbox, RefreshCw } from 'lucide-react'

export function LoadingSkeleton({ rows = 4 }: { rows?: number }) {
  return <div className="animate-pulse space-y-3" aria-label="Đang tải">{Array.from({ length: rows }).map((_, i) => <div key={i} className="h-12 rounded-md bg-slate-100" />)}</div>
}

export function EmptyState({ title = 'Chưa có dữ liệu', description = 'Dữ liệu sẽ xuất hiện tại đây khi được cập nhật.' }: { title?: string; description?: string }) {
  return <div className="flex min-h-52 flex-col items-center justify-center text-center"><div className="mb-4 rounded-full bg-slate-100 p-3"><Inbox className="text-slate-400"/></div><h3 className="font-bold text-slate-700">{title}</h3><p className="mt-1 max-w-sm text-sm text-muted">{description}</p></div>
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return <div className="rounded-lg border border-red-200 bg-red-50 p-5 text-sm text-red-700"><p>{message}</p>{onRetry && <button className="mt-3 inline-flex items-center gap-2 font-bold" onClick={onRetry}><RefreshCw size={15}/>Thử lại</button>}</div>
}
