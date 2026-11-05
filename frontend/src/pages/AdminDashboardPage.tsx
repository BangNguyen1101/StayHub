import {
  Activity,
  AlertCircle,
  BarChart3,
  Building2,
  ChevronLeft,
  ChevronRight,
  FileWarning,
  Receipt,
  Users,
  WalletCards,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { getApiMessage, reportApi } from '../lib/api';
import type { CountRow, Dashboard, RoomRevenue } from '../types/api';
import { Empty, ErrorBox, Loading } from '../components/Feedback';
import { StatCard } from '../components/StatCard';

const money = (value: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);
const dateParam = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

export function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [rooms, setRooms] = useState<CountRow[]>([]);
  const [roomRevenue, setRoomRevenue] = useState<RoomRevenue[]>([]);
  const [selectedMonth, setSelectedMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [loading, setLoading] = useState(true);
  const [revenueLoading, setRevenueLoading] = useState(true);
  const [error, setError] = useState('');
  const selectedYear = selectedMonth.getFullYear();
  const selectedMonthIndex = selectedMonth.getMonth();
  const monthLabel = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(selectedMonth);

  useEffect(() => {
    Promise.all([reportApi.dashboard(), reportApi.rooms()])
      .then(([dashboardData, roomData]) => {
        setDashboard(dashboardData);
        setRooms(roomData);
      })
      .catch((response) => setError(getApiMessage(response)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const from = new Date(selectedYear, selectedMonthIndex, 1);
    const to = new Date(selectedYear, selectedMonthIndex + 1, 0);
    setRevenueLoading(true);
    reportApi
      .revenueByRoom(dateParam(from), dateParam(to))
      .then(setRoomRevenue)
      .catch((response) => setError(getApiMessage(response)))
      .finally(() => setRevenueLoading(false));
  }, [selectedYear, selectedMonthIndex]);

  const totalRevenue = roomRevenue.reduce((sum, room) => sum + Number(room.amount), 0);
  const chartData = useMemo(
    () => roomRevenue.map((room) => ({ name: room.roomCode, value: Number(room.amount) })),
    [roomRevenue],
  );
  const changeMonth = (offset: number) => setSelectedMonth(new Date(selectedYear, selectedMonthIndex + offset, 1));

  if (loading) return <Loading />;
  if (error) return <ErrorBox message={error} />;
  if (!dashboard) return <Empty title="Chưa có dữ liệu dashboard" />;

  return (
    <div className="space-y-8">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <p className="text-sm font-semibold uppercase tracking-widest text-teal-600">Tổng quan vận hành</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight">Dashboard quản trị</h1>
          <p className="mt-2 text-slate-500">Theo dõi sức khỏe vận hành khu nhà trọ theo dữ liệu thực tế.</p>
        </div>
        <span className="rounded-full bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
          Dữ liệu trực tiếp
        </span>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Tổng phòng"
          value={dashboard.totalRooms}
          detail={`${dashboard.occupiedRooms} phòng đang có người`}
          icon={Building2}
        />
        <StatCard
          label="Tỷ lệ lấp đầy"
          value={`${dashboard.occupancyRate.toFixed(2)}%`}
          detail="Theo phòng OCCUPIED/FULL"
          icon={Activity}
          tone="blue"
        />
        <StatCard
          label="Công nợ hiện tại"
          value={money(dashboard.outstandingDebt)}
          detail={`${dashboard.overdueInvoices} hóa đơn quá hạn`}
          icon={WalletCards}
          tone="amber"
        />
        <StatCard label="Người thuê hoạt động" value={dashboard.activeTenants} icon={Users} tone="rose" />
      </div>
      <div className="grid gap-5 xl:grid-cols-[1.35fr_0.65fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="font-bold">Doanh thu theo tháng</h2>
              <p className="mt-1 text-sm text-slate-400">Đã thu theo từng phòng · Payments.SUCCEEDED</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                aria-label="Tháng trước"
                onClick={() => changeMonth(-1)}
                className="rounded-lg border p-2 text-slate-600 hover:bg-slate-50"
              >
                <ChevronLeft size={18} />
              </button>
              <strong className="min-w-32 text-center text-sm capitalize">{monthLabel}</strong>
              <button
                aria-label="Tháng sau"
                onClick={() => changeMonth(1)}
                className="rounded-lg border p-2 text-slate-600 hover:bg-slate-50"
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
          <div className="mt-3 rounded-xl bg-teal-50 px-4 py-3 text-sm text-teal-900">
            Tổng doanh thu: <b className="text-base">{money(totalRevenue)}</b>
          </div>
          {revenueLoading ? (
            <div className="mt-6">
              <Loading />
            </div>
          ) : chartData.length ? (
            <div className="mt-6 h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                  <XAxis dataKey="name" tickLine={false} axisLine={false} />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) =>
                      Number(value) ? `${Math.round(Number(value) / 1000).toLocaleString('vi-VN')}k` : '0'
                    }
                  />
                  <Tooltip formatter={(value) => [money(Number(value)), 'Đã thu']} />
                  <Bar dataKey="value" fill="#0f766e" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="mt-6">
              <Empty title="Chưa có doanh thu" detail="Tháng này chưa có thanh toán được xác nhận." />
            </div>
          )}
        </section>
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h2 className="font-bold">Trạng thái phòng</h2>
          <div className="mt-5 space-y-4">
            {rooms.length ? (
              rooms.map((item) => (
                <div key={item.code}>
                  <div className="mb-2 flex justify-between text-sm">
                    <span className="text-slate-500">{item.code}</span>
                    <strong>{item.count}</strong>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                    <div
                      className="h-full rounded-full bg-teal-500"
                      style={{
                        width: `${dashboard.totalRooms ? Math.min((item.count / dashboard.totalRooms) * 100, 100) : 0}%`,
                      }}
                    />
                  </div>
                </div>
              ))
            ) : (
              <Empty title="Chưa có phòng" />
            )}
          </div>
        </section>
      </div>
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-bold">Chi tiết doanh thu theo phòng</h2>
            <p className="mt-1 text-sm text-slate-400 capitalize">{monthLabel}</p>
          </div>
          <BarChart3 className="text-teal-600" size={20} />
        </div>
        {revenueLoading ? (
          <div className="mt-5">
            <Loading />
          </div>
        ) : roomRevenue.length ? (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr>
                  <th className="rounded-l-xl px-4 py-3">Phòng</th>
                  <th className="rounded-r-xl px-4 py-3 text-right">Doanh thu đã thu</th>
                </tr>
              </thead>
              <tbody>
                {roomRevenue.map((room) => (
                  <tr className="border-b last:border-0" key={room.roomId}>
                    <td className="px-4 py-3 font-semibold">{room.roomCode}</td>
                    <td className="px-4 py-3 text-right font-semibold text-teal-800">{money(room.amount)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td className="px-4 pt-4 font-bold">Tổng</td>
                  <td className="px-4 pt-4 text-right font-bold text-teal-800">{money(totalRevenue)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        ) : (
          <div className="mt-5">
            <Empty title="Chưa có doanh thu theo phòng" />
          </div>
        )}
      </section>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Hợp đồng ACTIVE"
          value={dashboard.activeContracts}
          detail={`${dashboard.expiringContracts} sắp hết hạn`}
          icon={FileWarning}
          tone="blue"
        />
        <StatCard label="Hợp đồng đã hết hạn" value={dashboard.expiredContracts} icon={FileWarning} tone="rose" />
        <StatCard label="Hóa đơn chưa thanh toán" value={dashboard.unpaidInvoices} icon={Receipt} tone="amber" />
        <StatCard
          label="Yêu cầu bảo trì"
          value={dashboard.maintenanceStatuses.reduce((sum, item) => sum + item.count, 0)}
          detail="Tất cả trạng thái"
          icon={AlertCircle}
        />
      </div>
    </div>
  );
}
