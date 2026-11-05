import { useEffect, useState } from 'react';
import {
  BarChart,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { getApiMessage, reportsApi } from '../lib/api';
import type { ReportCount, ReportMoney } from '../types/api';
import { inputClass } from '../components/Ui';
const colors = ['#0d9488', '#f59e0b', '#3b82f6', '#ef4444', '#8b5cf6'];
export function ReportsPage() {
  const [from, setFrom] = useState('2025-01-01');
  const [to, setTo] = useState('2025-12-31');
  const [dashboard, setDashboard] = useState<any>();
  const [revenue, setRevenue] = useState<ReportMoney[]>([]);
  const [invoiced, setInvoiced] = useState<ReportMoney[]>([]);
  const [items, setItems] = useState<ReportMoney[]>([]);
  const [rooms, setRooms] = useState<ReportCount[]>([]);
  const [contracts, setContracts] = useState<ReportCount[]>([]);
  const [maintenance, setMaintenance] = useState<ReportCount[]>([]);
  const [utilities, setUtilities] = useState<ReportMoney[]>([]);
  const [debt, setDebt] = useState<any>();
  const [error, setError] = useState('');
  const load = () =>
    Promise.all([
      reportsApi.dashboard(to),
      reportsApi.revenue(from, to),
      reportsApi.invoiced(from, to),
      reportsApi.invoiceItems(from, to),
      reportsApi.rooms(),
      reportsApi.contracts(),
      reportsApi.maintenance(),
      reportsApi.utilities(from.slice(0, 7), to.slice(0, 7)),
      reportsApi.debts(to),
    ])
      .then(([d, r, i, it, ro, c, m, u, de]) => {
        setDashboard(d);
        setRevenue(r.rows);
        setInvoiced(i.rows);
        setItems(it.rows);
        setRooms(ro);
        setContracts(c);
        setMaintenance(m);
        setUtilities(u.rows);
        setDebt(de);
      })
      .catch((e) => setError(getApiMessage(e)));
  useEffect(() => {
    void load();
  }, []);
  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-600">BÁO CÁO & THỐNG KÊ</p>
          <h1 className="mt-1 text-3xl font-bold">Tổng quan vận hành</h1>
        </div>
        <div className="flex gap-2">
          <input type="date" className={inputClass} value={from} onChange={(e) => setFrom(e.target.value)} />
          <input type="date" className={inputClass} value={to} onChange={(e) => setTo(e.target.value)} />
          <button onClick={load} className="rounded-xl bg-teal-600 px-4 text-sm font-semibold text-white">
            Lọc
          </button>
        </div>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Phòng', dashboard?.totalRooms],
          ['Hợp đồng ACTIVE', dashboard?.activeContracts],
          ['Yêu cầu thuê chờ xử lý', dashboard?.pendingRentalRequests],
          ['Công nợ', Number(debt?.outstanding || dashboard?.outstandingDebt || 0).toLocaleString('vi-VN') + ' đ'],
        ].map(([l, v]) => (
          <div key={String(l)} className="rounded-2xl bg-white p-5">
            <p className="text-sm text-slate-500">{l}</p>
            <p className="mt-2 text-2xl font-bold">{v ?? '—'}</p>
          </div>
        ))}
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <Chart title="Doanh thu thực thu" data={revenue} />
        <Chart title="Tổng tiền đã lập hóa đơn" data={invoiced} />
        <Chart title="Chi tiết InvoiceItems" data={items} />
        <Donut title="Phòng theo trạng thái" data={rooms} />
        <Donut title="Hợp đồng theo trạng thái" data={contracts} />
        <Donut title="Bảo trì theo trạng thái" data={maintenance} />
        <Chart title="Tiêu thụ điện nước" data={utilities} />
      </div>
    </section>
  );
}
function Chart({ title, data }: { title: string; data: ReportMoney[] }) {
  return (
    <div className="rounded-2xl bg-white p-5">
      <h2 className="font-bold">{title}</h2>
      {data.length ? (
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="code" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="amount" fill="#0d9488" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <p className="p-10 text-center text-sm text-slate-400">Chưa có dữ liệu.</p>
      )}
    </div>
  );
}
function Donut({ title, data }: { title: string; data: ReportCount[] }) {
  return (
    <div className="rounded-2xl bg-white p-5">
      <h2 className="font-bold">{title}</h2>
      {data.length ? (
        <ResponsiveContainer width="100%" height={260}>
          <PieChart>
            <Pie data={data} dataKey="count" nameKey="code" outerRadius={90} label>
              {data.map((_, i) => (
                <Cell key={i} fill={colors[i % colors.length]} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      ) : (
        <p className="p-10 text-center text-sm text-slate-400">Chưa có dữ liệu.</p>
      )}
    </div>
  );
}
