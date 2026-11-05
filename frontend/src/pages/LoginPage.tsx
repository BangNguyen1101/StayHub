import { Building2, Eye, EyeOff, LockKeyhole, Mail } from 'lucide-react';
import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi, getApiMessage } from '../lib/api';
import { saveAuth } from '../lib/auth';
export function LoginPage() {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = await authApi.login({ identifier, password });
      saveAuth(auth);
      navigate(auth.user.role === 'ADMIN' ? '/admin' : '/tenant', { replace: true });
    } catch (err) {
      setError(getApiMessage(err));
    } finally {
      setLoading(false);
    }
  }
  return (
    <AuthFrame title="Chào mừng trở lại" subtitle="Đăng nhập để quản lý không gian sống của bạn">
      <form onSubmit={submit} className="space-y-5">
        <label className="field-label">
          Email hoặc tên đăng nhập
          <div className="input-wrap">
            <Mail size={18} />
            <input
              autoFocus
              required
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="you@example.com"
            />
          </div>
        </label>
        <label className="field-label">
          Mật khẩu
          <div className="input-wrap">
            <LockKeyhole size={18} />
            <input
              required
              type={show ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
            />
            <button type="button" onClick={() => setShow(!show)}>
              {show ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </label>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button disabled={loading} className="primary-button w-full">
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
        <p className="text-center text-sm text-slate-500">Tài khoản người thuê được cấp bởi quản trị viên.</p>
      </form>
    </AuthFrame>
  );
}
function AuthFrame({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen bg-white lg:grid-cols-[1.05fr_0.95fr]">
      <div className="hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-2xl bg-teal-500">
            <Building2 />
          </span>
          <span className="text-lg font-bold">Nhà trọ Tòng Bạt</span>
        </div>
        <div>
          <p className="max-w-lg text-5xl font-bold leading-tight">
            Không gian sống được quản lý <span className="text-teal-400">đơn giản hơn.</span>
          </p>
          <p className="mt-6 max-w-md text-slate-400">
            Một nơi duy nhất để theo dõi phòng, hợp đồng, hóa đơn và yêu cầu hỗ trợ.
          </p>
        </div>
        <p className="text-sm text-slate-500">© 2026 Nhà trọ Tòng Bạt</p>
      </div>
      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-md">
          <div className="mb-10 lg:hidden">
            <div className="flex items-center gap-3">
              <span className="grid h-10 w-10 place-items-center rounded-2xl bg-teal-600 text-white">
                <Building2 size={20} />
              </span>
              <span className="font-bold">Nhà trọ Tòng Bạt</span>
            </div>
          </div>
          <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
          <p className="mt-2 text-slate-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}
