import type { LucideIcon } from 'lucide-react';
export function StatCard({
  label,
  value,
  detail,
  icon: Icon,
  tone = 'teal',
}: {
  label: string;
  value: string | number;
  detail?: string;
  icon: LucideIcon;
  tone?: 'teal' | 'amber' | 'blue' | 'rose';
}) {
  const colors = {
    teal: 'bg-teal-50 text-teal-700',
    amber: 'bg-amber-50 text-amber-700',
    blue: 'bg-blue-50 text-blue-700',
    rose: 'bg-rose-50 text-rose-700',
  };
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="mt-3 text-2xl font-bold tracking-tight">{value}</p>
          {detail && <p className="mt-1 text-xs text-slate-400">{detail}</p>}
        </div>
        <span className={`grid h-10 w-10 place-items-center rounded-xl ${colors[tone]}`}>
          <Icon size={19} />
        </span>
      </div>
    </div>
  );
}
