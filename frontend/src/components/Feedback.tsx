export function Loading({ label = 'Đang tải dữ liệu...' }: { label?: string }) {
  return (
    <div className="flex min-h-40 items-center justify-center text-sm text-slate-400">
      <span className="mr-3 h-5 w-5 animate-spin rounded-full border-2 border-teal-600 border-t-transparent" />
      {label}
    </div>
  );
}
export function Empty({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <p className="font-semibold text-slate-700">{title}</p>
      {detail && <p className="mt-1 text-sm text-slate-400">{detail}</p>}
    </div>
  );
}
export function ErrorBox({ message }: { message: string }) {
  return <div className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">{message}</div>;
}
