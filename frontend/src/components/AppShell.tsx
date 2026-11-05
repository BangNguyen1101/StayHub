import {
  BarChart3,
  Bell,
  Building2,
  ChevronRight,
  FileText,
  Gauge,
  LayoutDashboard,
  LogOut,
  Menu,
  Receipt,
  Settings,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { clearAuth, currentUser } from '../lib/auth';
import { notificationApi } from '../lib/api';
import type { AppNotification } from '../types/api';
import { AiAssistantWidget } from './AiAssistantWidget';

const adminLinks = [
  ['/admin', 'Tổng quan', LayoutDashboard],
  ['/admin/rooms', 'Quản lý phòng', Building2],
  ['/admin/tenants', 'Người thuê', Users],
  ['/admin/contracts', 'Hợp đồng', FileText],
  ['/admin/utilities', 'Điện nước & dịch vụ', Gauge],
  ['/admin/invoices', 'Hóa đơn & công nợ', Receipt],
  ['/admin/maintenance', 'Bảo trì', Wrench],
  ['/admin/reports', 'Báo cáo', BarChart3],
] as const;

const tenantLinks = [
  ['/tenant', 'Tổng quan', LayoutDashboard],
  ['/tenant/contracts', 'Hợp đồng', FileText],
  ['/tenant/invoices', 'Hóa đơn & công nợ', Receipt],
  ['/tenant/payments', 'Thanh toán', Receipt],
  ['/tenant/maintenance', 'Bảo trì', Wrench],
  ['/tenant/notifications', 'Thông báo', Bell],
  ['/tenant/profile', 'Hồ sơ cá nhân', Settings],
] as const;

export function AppShell() {
  const user = currentUser();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [popup, setPopup] = useState<AppNotification[]>([]);
  const [showPopup, setShowPopup] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const links = user?.role === 'ADMIN' ? adminLinks : tenantLinks;
  const title =
    links.find((x) => x[0] === location.pathname)?.[1] ||
    (user?.role === 'ADMIN' ? 'Dashboard quản trị' : 'Dashboard người thuê');

  const refreshNotifications = () => {
    if (user?.role === 'ADMIN') {
      return notificationApi
        .adminUnreadCount()
        .then(setUnread)
        .catch(() => undefined);
    }
    if (user?.role === 'TENANT') {
      return notificationApi
        .unreadCount()
        .then(setUnread)
        .catch(() => undefined);
    }
    return Promise.resolve();
  };

  useEffect(() => {
    void refreshNotifications();
    const timer = window.setInterval(() => void refreshNotifications(), 15000);
    return () => window.clearInterval(timer);
  }, [user?.role]);

  const toggle = () => {
    const next = !showPopup;
    setShowPopup(next);
    if (!next) return;
    const list =
      user?.role === 'ADMIN'
        ? notificationApi.adminList({ page: 0, size: 5 })
        : notificationApi.tenantList({ page: 0, size: 5 });
    list.then((response) => setPopup(response.content)).catch(() => setPopup([]));
  };

  const openNotification = (notification: AppNotification) => {
    const markRead =
      user?.role === 'ADMIN'
        ? notificationApi.adminMarkRead(notification.id)
        : notificationApi.markRead(notification.id);
    markRead
      .catch(() => undefined)
      .finally(() => {
        if (!notification.read) setUnread((value) => Math.max(0, value - 1));
        setPopup((items) => items.map((item) => (item.id === notification.id ? { ...item, read: true } : item)));
        setShowPopup(false);
        if (user?.role === 'ADMIN' && notification.relatedEntityType === 'MAINTENANCE_REQUEST') {
          navigate('/admin/maintenance');
        }
      });
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-72 transform border-r border-slate-200 bg-white px-5 py-6 transition-transform lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded-2xl bg-teal-600 text-white">
              <Building2 size={20} />
            </span>
            <div>
              <p className="font-bold">Nhà trọ Tòng Bạt</p>
              <p className="text-xs text-slate-400">Quản lý vận hành</p>
            </div>
          </div>
          <button className="lg:hidden" onClick={() => setOpen(false)}>
            <X size={20} />
          </button>
        </div>
        <nav className="mt-10 space-y-1">
          {links.map(([to, label, Icon]) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/admin' || to === '/tenant'}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium ${isActive ? 'bg-teal-600 text-white' : 'text-slate-500 hover:bg-slate-50'}`
              }
            >
              <Icon size={18} />
              {label}
            </NavLink>
          ))}
        </nav>
        <button
          onClick={() => {
            clearAuth();
            navigate('/login');
          }}
          className="absolute bottom-6 flex items-center gap-3 text-sm text-slate-500"
        >
          <LogOut size={18} />
          Đăng xuất
        </button>
      </aside>
      <div className="lg:pl-72">
        <header className="sticky top-0 z-30 flex h-20 items-center justify-between border-b bg-white/90 px-5 backdrop-blur md:px-8">
          <button className="lg:hidden" onClick={() => setOpen(true)}>
            <Menu size={22} />
          </button>
          <div className="hidden items-center gap-2 text-sm text-slate-400 md:flex">
            Trang chủ
            <ChevronRight size={15} />
            {title}
          </div>
          <div className="flex items-center gap-3">
            <div className="relative">
              <button
                onClick={toggle}
                aria-label="Mở thông báo"
                className="relative rounded-xl p-2 text-slate-500 hover:bg-slate-100"
              >
                <Bell size={19} />
                {unread > 0 && <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-orange-500" />}
              </button>
              {showPopup && (
                <div className="absolute right-0 top-12 z-50 w-[360px] max-w-[calc(100vw-2rem)] rounded-2xl border bg-white p-3 shadow-xl">
                  <p className="px-2 pb-2 text-sm font-bold">Thông báo mới</p>
                  {popup.length ? (
                    popup.map((notification) => (
                      <button
                        type="button"
                        key={notification.id}
                        onClick={() => openNotification(notification)}
                        className={`block w-full rounded-xl p-2 text-left hover:bg-slate-50 ${notification.read ? '' : 'bg-teal-50'}`}
                      >
                        <b className="block whitespace-normal break-words text-sm">{notification.title}</b>
                        <p className="mt-1 whitespace-normal break-words text-xs text-slate-500">
                          {notification.content}
                        </p>
                      </button>
                    ))
                  ) : (
                    <p className="p-2 text-sm text-slate-400">Không có thông báo.</p>
                  )}
                </div>
              )}
            </div>
            <div className="h-9 w-9 rounded-xl bg-teal-100 text-center leading-9 text-teal-700">
              {user?.fullName?.slice(0, 1).toUpperCase()}
            </div>
            <div className="hidden sm:block">
              <p className="text-sm font-semibold">{user?.fullName}</p>
              <p className="text-xs text-slate-400">{user?.role === 'ADMIN' ? 'Quản trị viên' : 'Người thuê'}</p>
            </div>
          </div>
        </header>
        <main className="mx-auto max-w-[1600px] p-5 md:p-8">
          <Outlet />
        </main>
      </div>
      {user && <AiAssistantWidget role={user.role} />}
    </div>
  );
}
