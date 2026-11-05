import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { ProtectedRoute } from './components/ProtectedRoute';
import { currentUser } from './lib/auth';
import { AdminDashboardPage } from './pages/AdminDashboardPage';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { TenantDashboardPage } from './pages/TenantDashboardPage';
import { AdminRoomsPage } from './pages/AdminRoomsPage';
import { TenantRoomsPage } from './pages/TenantRoomsPage';
import { RentalRequestsPage } from './pages/RentalRequestsPage';
import { TenantsPage, TenantProfilePage } from './pages/TenantsPage';
import { ContractsPage } from './pages/ContractsPage';
import { BillingPage } from './pages/BillingPage';
import { PaymentsPage } from './pages/PaymentsPage';
const MaintenancePage = lazy(() => import('./pages/MaintenancePage').then((m) => ({ default: m.MaintenancePage })));
const NotificationsPage = lazy(() =>
  import('./pages/NotificationsPage').then((m) => ({ default: m.NotificationsPage })),
);
const ReportsPage = lazy(() => import('./pages/ReportsPage').then((m) => ({ default: m.ReportsPage })));
const UtilityAdminPage = lazy(() => import('./pages/UtilityAdminPage').then((m) => ({ default: m.UtilityAdminPage })));
function Home() {
  const user = currentUser();
  return <Navigate to={user ? (user.role === 'ADMIN' ? '/admin' : '/tenant') : '/login'} replace />;
}
export default function App() {
  return (
    <Suspense fallback={<div className="p-8 text-slate-500">Đang tải trang...</div>}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute role="ADMIN" />}>
          <Route element={<AppShell />}>
            <Route path="/admin" element={<AdminDashboardPage />} />
            <Route path="/admin/rooms" element={<AdminRoomsPage />} />
            <Route path="/admin/rental-requests" element={<RentalRequestsPage />} />
            <Route path="/admin/tenants" element={<TenantsPage />} />
            <Route path="/admin/contracts" element={<ContractsPage />} />
            <Route path="/admin/invoices" element={<BillingPage />} />
            <Route path="/admin/payments" element={<PaymentsPage />} />
            <Route path="/admin/maintenance" element={<MaintenancePage />} />
            <Route path="/admin/utilities" element={<UtilityAdminPage />} />
            <Route path="/admin/reports" element={<ReportsPage />} />
          </Route>
        </Route>
        <Route element={<ProtectedRoute role="TENANT" />}>
          <Route element={<AppShell />}>
            <Route path="/tenant" element={<TenantDashboardPage />} />
            <Route path="/tenant/rooms" element={<TenantRoomsPage />} />
            <Route path="/tenant/rental-requests" element={<RentalRequestsPage />} />
            <Route path="/tenant/contracts" element={<ContractsPage />} />
            <Route path="/tenant/invoices" element={<BillingPage />} />
            <Route path="/tenant/payments" element={<PaymentsPage />} />
            <Route path="/tenant/maintenance" element={<MaintenancePage />} />
            <Route path="/tenant/notifications" element={<NotificationsPage />} />
            <Route path="/tenant/profile" element={<TenantProfilePage />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
