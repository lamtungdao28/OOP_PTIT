import { X } from 'lucide-react'
import type { ReactNode } from 'react'

export function FormDialog({ open, title, children, onClose, size = 'md' }: { open: boolean; title: string; children: ReactNode; onClose: () => void; size?: 'md' | 'lg' }) {
  if (!open) return null
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/40 p-4" role="dialog" aria-modal="true"><div className={`max-h-[90vh] w-full overflow-y-auto rounded-lg bg-white shadow-2xl ${size === 'lg' ? 'max-w-4xl' : 'max-w-xl'}`}><div className="sticky top-0 z-10 flex items-center justify-between border-b bg-white px-6 py-4"><h2 className="text-lg font-bold">{title}</h2><button className="rounded p-1 hover:bg-slate-100" onClick={onClose}><X/></button></div><div className="p-6">{children}</div></div></div>
}

export function ConfirmDialog({ open, title, description, confirmLabel = 'Xác nhận', onConfirm, onClose }: { open: boolean; title: string; description: string; confirmLabel?: string; onConfirm: () => void; onClose: () => void }) {
  if (!open) return null
  return <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/40 p-4"><div className="w-full max-w-md rounded-lg bg-white p-6 shadow-2xl"><h2 className="text-lg font-bold">{title}</h2><p className="mt-2 text-sm leading-6 text-muted">{description}</p><div className="mt-6 flex justify-end gap-3"><button className="btn-secondary" onClick={onClose}>Hủy</button><button className="btn-primary" onClick={onConfirm}>{confirmLabel}</button></div></div></div>
}
