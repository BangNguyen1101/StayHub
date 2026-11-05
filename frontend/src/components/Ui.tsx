import { ReactNode } from 'react';
import { X } from 'lucide-react';

export function Badge({ value }: { value: string }) {
  const colors: Record<string, string> = {
    VACANT: 'bg-emerald-50 text-emerald-700',
    OCCUPIED: 'bg-blue-50 text-blue-700',
    FULL: 'bg-orange-50 text-orange-700',
    MAINTENANCE: 'bg-amber-50 text-amber-700',
    INACTIVE: 'bg-slate-100 text-slate-500',
    PENDING: 'bg-amber-50 text-amber-700',
    NEEDS_INFO: 'bg-violet-50 text-violet-700',
    APPROVED: 'bg-emerald-50 text-emerald-700',
    REJECTED: 'bg-red-50 text-red-700',
    CANCELLED: 'bg-slate-100 text-slate-500',
    DRAFT: 'bg-slate-100 text-slate-600',
    ACTIVE: 'bg-emerald-50 text-emerald-700',
    EXPIRING: 'bg-orange-50 text-orange-700',
    AWAITING_SIGNATURE: 'bg-amber-50 text-amber-700',
    SIGNED: 'bg-emerald-50 text-emerald-700',
    VOID: 'bg-slate-100 text-slate-500',
    EXPIRED: 'bg-slate-100 text-slate-500',
    TERMINATED: 'bg-slate-100 text-slate-500',
    OPEN: 'bg-blue-50 text-blue-700',
    IN_PROGRESS: 'bg-amber-50 text-amber-700',
    WAITING: 'bg-violet-50 text-violet-700',
    RESOLVED: 'bg-emerald-50 text-emerald-700',
    CLOSED: 'bg-slate-100 text-slate-500',
    LOW: 'bg-slate-100 text-slate-600',
    NORMAL: 'bg-blue-50 text-blue-700',
    HIGH: 'bg-orange-50 text-orange-700',
    URGENT: 'bg-red-50 text-red-700',
  };
  const labels: Record<string, string> = {
    VACANT: 'Còn trống',
    OCCUPIED: 'Đang ở',
    FULL: 'Đã đủ người',
    MAINTENANCE: 'Đang bảo trì',
    INACTIVE: 'Ngừng hoạt động',
    PENDING: 'Chờ xử lý',
    NEEDS_INFO: 'Cần bổ sung',
    APPROVED: 'Đã duyệt',
    REJECTED: 'Từ chối',
    CANCELLED: 'Đã hủy',
    DRAFT: 'Bản nháp',
    ACTIVE: 'Đang hiệu lực',
    EXPIRING: 'Sắp hết hạn',
    AWAITING_SIGNATURE: 'Chờ ký',
    SIGNED: 'Đã ký',
    VOID: 'Đã huỷ bỏ',
    EXPIRED: 'Đã hết hạn',
    TERMINATED: 'Đã kết thúc',
    LOCKED: 'Đã khóa',
    OPEN: 'Mới tiếp nhận',
    IN_PROGRESS: 'Đang xử lý',
    WAITING: 'Chờ thông tin/vật tư',
    RESOLVED: 'Đã xử lý',
    CLOSED: 'Đã đóng',
    LOW: 'Thấp',
    NORMAL: 'Bình thường',
    HIGH: 'Cao',
    URGENT: 'Khẩn cấp',
    UNPAID: 'Chưa thanh toán',
    PARTIALLY_PAID: 'Thanh toán một phần',
    PAID: 'Đã thanh toán',
    OVERDUE: 'Quá hạn',
    SUCCEEDED: 'Thành công',
    FAILED: 'Thất bại',
  };
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${colors[value] || 'bg-slate-100 text-slate-600'}`}
    >
      {labels[value] || value.replace('_', ' ')}
    </span>
  );
}
export function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 p-4">
      <div className="max-h-[90vh] w-full max-w-2xl overflow-auto rounded-2xl bg-white p-6 shadow-2xl">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-bold">{title}</h2>
          <button onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  return (
    <div className="flex items-center justify-between border-t border-slate-100 pt-4 text-sm text-slate-500">
      <span>
        Trang {totalPages ? page + 1 : 0} / {totalPages}
      </span>
      <div className="flex gap-2">
        <button
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
          className="rounded-lg border px-3 py-1.5 disabled:opacity-40"
        >
          Trước
        </button>
        <button
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
          className="rounded-lg border px-3 py-1.5 disabled:opacity-40"
        >
          Sau
        </button>
      </div>
    </div>
  );
}
export function ConfirmDialog({
  title,
  message,
  onConfirm,
  onCancel,
}: {
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="text-sm text-slate-600">{message}</p>
      <div className="mt-6 flex justify-end gap-3">
        <button className="rounded-xl border px-4 py-2 text-sm" onClick={onCancel}>
          Hủy
        </button>
        <button className="rounded-xl bg-teal-600 px-4 py-2 text-sm font-semibold text-white" onClick={onConfirm}>
          Xác nhận
        </button>
      </div>
    </Modal>
  );
}
export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm font-medium text-slate-700">
      <span className="mb-1.5 block">{label}</span>
      {children}
    </label>
  );
}
export const inputClass =
  'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100';
