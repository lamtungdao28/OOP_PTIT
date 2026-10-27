import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

const variants = { blue: ['bg-[#EAF4FF]', 'bg-[#1484F5]', 'text-[#247CD5]'], green: ['bg-[#E7F8EE]', 'bg-[#22B369]', 'text-[#249553]'], orange: ['bg-[#FFF5E4]', 'bg-[#FF891F]', 'text-[#E47721]'], red: ['bg-[#FFF0F1]', 'bg-[#E83149]', 'text-[#D92D43]'] }
export function StatCard({ label, value, icon: Icon, variant = 'blue' }: { label: string; value: string | number; icon: LucideIcon; variant?: keyof typeof variants }) {
  const [bg, iconBg, color] = variants[variant]
  return <div className={cn('flex h-40 flex-col justify-center rounded-lg px-6', bg, color)}><div className="flex items-center gap-4"><div className={cn('grid h-11 w-11 place-items-center rounded-full text-white', iconBg)}><Icon size={22}/></div><strong className="text-3xl">{value}</strong></div><p className="mt-4 text-center font-medium">{label}</p></div>
}
