import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, getApiMessage } from '../lib/api';
import { saveAuth } from '../lib/auth';
import { LoginPage } from './LoginPage';
export function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', fullName: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const update = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [key]: e.target.value });
  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = await authApi.register(form);
      saveAuth(auth);
      navigate('/tenant', { replace: true });
    } catch (err) {
      setError(getApiMessage(err));
    } finally {
      setLoading(false);
    }
  }
  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="mx-auto max-w-xl rounded-3xl border border-slate-200 bg-white p-7 shadow-sm md:p-10">
        <Link to="/login" className="text-sm font-semibold text-teal-700">
          ← Quay lại đăng nhập
        </Link>
        <h1 className="mt-8 text-3xl font-bold">Tạo tài khoản người thuê</h1>
        <p className="mt-2 text-slate-500">Tài khoản đăng ký mới mặc định có vai trò TENANT.</p>
        <form onSubmit={submit} className="mt-8 grid gap-5 md:grid-cols-2">
          {[
            ['fullName', 'Họ và tên', 'Nguyễn Văn A'],
            ['username', 'Tên đăng nhập', 'nguyenvana'],
            ['email', 'Email', 'you@example.com'],
            ['phoneNumber', 'Số điện thoại', '090...'],
            ['password', 'Mật khẩu', 'Tối thiểu 8 ký tự'],
          ].map(([key, label, placeholder]) => (
            <label key={key} className={`field-label ${key === 'password' ? 'md:col-span-2' : ''}`}>
              {label}
              <input
                className="field-input mt-2"
                required={key !== 'phoneNumber'}
                type={key === 'password' ? 'password' : 'text'}
                value={form[key as keyof typeof form]}
                onChange={update(key as keyof typeof form)}
                placeholder={placeholder}
              />
            </label>
          ))}
          {error && <p className="md:col-span-2 text-sm text-red-600">{error}</p>}
          <button disabled={loading} className="primary-button md:col-span-2">
            {loading ? 'Đang tạo tài khoản...' : 'Đăng ký tài khoản'}
          </button>
        </form>
      </div>
    </div>
  );
}
