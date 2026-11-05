import { Navigate, Outlet } from 'react-router-dom';
import type { Role } from '../types/api';
import { currentUser } from '../lib/auth';
export function ProtectedRoute({ role }: { role?: Role }) {
  const user = currentUser();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}
