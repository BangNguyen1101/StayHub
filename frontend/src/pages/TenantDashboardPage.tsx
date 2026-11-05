import { Bell, CalendarDays, FileText, Receipt, WalletCards } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getApiMessage, tenantApi } from '../lib/api';
import type { Contract, DebtItem, Invoice, Notification, PageResponse } from '../types/api';
import { Empty, ErrorBox, Loading } from '../components/Feedback';
import { StatCard } from '../components/StatCard';
const money = (v: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(v || 0);
const date = (v?: string) => (v ? new Intl.DateTimeFormat('vi-VN').format(new Date(v)) : '—');
export function TenantDashboardPage() {
  const [invoices, setInvoices] = useState<PageResponse<Invoice> | null>(null);
  const [debts, setDebts] = useState<PageResponse<DebtItem> | null>(null);
  const [contracts, setContracts] = useState<PageResponse<Contract> | null>(null);
  const [notifications, setNotifications] = useState<PageResponse<Notification> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    Promise.all([tenantApi.invoices(), tenantApi.debts(), tenantApi.contracts(), tenantApi.notifications()])
      .then(([i, d, c, n]) => {
        setInvoices(i);
        setDebts(d);
        setContracts(c);
        setNotifications(n);
      })
      .catch((e) => setError(getApiMessage(e)))
      .finally(() => setLoading(false));
  }, []);
  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;
  const active = contracts?.content.find((c) => ['ACTIVE', 'EXPIRING'].includes(c.status));
  return (
    <div className="space-y-8">
      <div>
        <p className="text-sm font-semibold uppercase tracking-widest text-teal-600">Không gian của bạn</p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight">Xin chào, người thuê</h1>
        <p className="mt-2 text-slate-500">Tổng hợp nhanh hợp đồng, hóa đơn và thông báo cá nhân.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Hợp đồng hiện tại"
          value={active?.contractCode || 'Chưa có'}
          detail={active ? `Phòng ${active.roomCode || 'Chưa có mã phòng'}` : 'Bạn chưa có hợp đồng hiệu lực'}
          icon={FileText}
        />
        <StatCard
          label="Hóa đơn gần đây"
          value={invoices?.content.length || 0}
          detail="Theo danh sách cá nhân"
          icon={Receipt}
          tone="blue"
        />
        <StatCard
          label="Công nợ đang xem"
          value={debts?.content.length || 0}
          detail="Các khoản cần theo dõi"
          icon={WalletCards}
          tone="amber"
        />
        <StatCard
          label="Thông báo mới"
          value={notifications?.content.filter((n) => !n.read).length || 0}
          detail="Chưa đánh dấu đã đọc"
          icon={Bell}
          tone="rose"
        />
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="font-bold">Hóa đơn gần đây</h2>
            <Receipt size={19} className="text-teal-600" />
          </div>
          {invoices?.content.length ? (
            <div className="mt-5 divide-y divide-slate-100">
              {invoices.content.map((invoice) => (
                <div className="flex items-center justify-between gap-3 py-4" key={invoice.id}>
                  <div>
                    <p className="font-semibold">{invoice.invoiceCode}</p>
                    <p className="mt-1 text-xs text-slate-400">
                      Kỳ {invoice.billingPeriod} · Hạn {date(invoice.dueDate)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold">{money(invoice.totalAmount)}</p>
                    <p className="mt-1 text-xs text-slate-400">Còn nợ {money(invoice.outstandingAmount)}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-5">
              <Empty title="Chưa có hóa đơn" detail="Hóa đơn của bạn sẽ hiển thị tại đây." />
            </div>
          )}
        </section>
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="font-bold">Thông tin cần nhớ</h2>
            <CalendarDays size={19} className="text-teal-600" />
          </div>
          {active ? (
            <div className="mt-5 rounded-2xl bg-teal-50 p-5">
              <p className="text-sm font-semibold text-teal-800">
                {active.contractCode} · {active.status}
              </p>
              <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-teal-700/60">Bắt đầu</p>
                  <p className="mt-1 font-semibold text-teal-950">{date(active.startDate)}</p>
                </div>
                <div>
                  <p className="text-teal-700/60">Kết thúc</p>
                  <p className="mt-1 font-semibold text-teal-950">{date(active.endDate)}</p>
                </div>
              </div>
            </div>
          ) : (
            <Empty
              title="Chưa có hợp đồng hiệu lực"
              detail="Bạn có thể liên hệ quản trị viên để được hỗ trợ đăng ký phòng."
            />
          )}
          {notifications?.content.length ? (
            <div className="mt-5 space-y-3">
              {notifications.content.slice(0, 3).map((n) => (
                <div className="rounded-xl bg-slate-50 p-3" key={n.id}>
                  <p className="text-sm font-semibold">{n.title}</p>
                  <p className="mt-1 line-clamp-2 text-xs text-slate-500">{n.content}</p>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      </div>
    </div>
  );
}
