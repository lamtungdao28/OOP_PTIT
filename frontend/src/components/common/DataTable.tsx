import type { ReactNode } from 'react'
import { EmptyState } from './States'

export interface Column<T> { key: string; title: string; render: (row: T) => ReactNode; className?: string }
export function DataTable<T>({ columns, rows, rowKey }: { columns: Column<T>[]; rows: T[]; rowKey: (row: T) => string | number }) {
  return <div className="overflow-x-auto"><table className="w-full min-w-[720px] border-collapse text-left"><thead><tr className="border-b bg-slate-50/80">{columns.map(c => <th key={c.key} className="px-5 py-4 text-xs font-bold uppercase tracking-wide text-slate-500">{c.title}</th>)}</tr></thead><tbody>{rows.map(row => <tr key={rowKey(row)} className="border-b last:border-0 hover:bg-slate-50/60">{columns.map(c => <td key={c.key} className={`px-5 py-4 text-sm text-slate-700 ${c.className || ''}`}>{c.render(row)}</td>)}</tr>)}</tbody></table>{!rows.length && <EmptyState/>}</div>
}
