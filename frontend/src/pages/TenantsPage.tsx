import { useEffect, useState } from 'react';
import { getApiMessage, tenantApi, tenantManagementApi } from '../lib/api';
import type { TenantSummary } from '../types/api';
import { ConfirmDialog, Field, Modal, Pagination, inputClass } from '../components/Ui';
import { TenantProfileEditor } from '../components/TenantProfileEditor';
import { TenantRoomAssignment } from '../components/TenantRoomAssignment';

const emptyAccount = { username: '', password: '', fullName: '', phoneNumber: '' };

export function TenantsPage() {
  const [data, setData] = useState<any>();
  const [keyword, setKeyword] = useState('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(0);
  const [detail, setDetail] = useState<any>();
  const [assignment, setAssignment] = useState<TenantSummary>();
  const [deleteTenant, setDeleteTenant] = useState<TenantSummary>();
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({ ...emptyAccount });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = () =>
    tenantManagementApi
      .search({ keyword: keyword || undefined, active: active === '' ? undefined : active === 'true', page })
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));

  useEffect(() => {
    void load();
  }, [page, active]);

  const create = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    tenantManagementApi
      .create(form)
      .then(() => {
        setCreateOpen(false);
        setForm({ ...emptyAccount });
        setSuccess('Đã tạo tài khoản người thuê. Hãy cung cấp tên đăng nhập và mật khẩu ban đầu cho người thuê.');
        void load();
      })
      .catch((e) => setError(getApiMessage(e)));
  };

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-600">QUẢN LÝ NGƯỜI THUÊ</p>
          <h1 className="mt-1 text-3xl font-bold">Danh sách người thuê</h1>
        </div>
        <button
          className="rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white"
          onClick={() => {
            setForm({ ...emptyAccount });
            setCreateOpen(true);
          }}
        >
          Tạo tài khoản người thuê
        </button>
      </div>
      <div className="flex flex-wrap gap-3">
        <input
          className={`${inputClass} flex-1`}
          placeholder="Tìm tên, email, tài khoản"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && void load()}
        />
        <select
          className={`${inputClass} max-w-xs`}
          value={active}
          onChange={(e) => {
            setActive(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Tất cả tài khoản</option>
          <option value="true">Đang hoạt động</option>
          <option value="false">Đã khóa</option>
        </select>
        <button
          onClick={() => {
            setPage(0);
            void load();
          }}
          className="rounded-xl bg-slate-900 px-4 text-sm font-semibold text-white"
        >
          Tìm kiếm
        </button>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {success && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-5 py-4">Người thuê</th>
              <th>Email</th>
              <th>Số điện thoại</th>
              <th>Trạng thái</th>
              <th className="px-5">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map((t: TenantSummary) => (
              <tr className="border-t" key={t.userId}>
                <td className="px-5 py-4">
                  <b>{t.fullName}</b>
                  <div className="text-xs text-slate-400">{t.username}</div>
                </td>
                <td>{t.email}</td>
                <td>{t.phoneNumber || '-'}</td>
                <td>
                  <span className={t.active ? 'text-emerald-700' : 'text-slate-400'}>
                    {t.active ? 'Đang hoạt động' : 'Đã khóa'}
                  </span>
                </td>
                <td className="px-5">
                  <button
                    className="text-teal-700"
                    onClick={() =>
                      tenantManagementApi
                        .detail(t.userId)
                        .then(setDetail)
                        .catch((e) => setError(getApiMessage(e)))
                    }
                  >
                    Sửa thông tin
                  </button>
                  <button
                    className="ml-3 text-blue-700 disabled:opacity-50"
                    disabled={!t.active}
                    onClick={() => setAssignment(t)}
                  >
                    Gán phòng
                  </button>
                  <button
                    className="ml-3 text-red-700 disabled:opacity-50"
                    disabled={!t.active}
                    onClick={() => setDeleteTenant(t)}
                  >
                    Xóa
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có người thuê.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )}
      </div>
      {assignment && (
        <Modal title="Gán phòng — Tạo hợp đồng thuê" onClose={() => setAssignment(undefined)}>
          <TenantRoomAssignment tenant={assignment} />
        </Modal>
      )}
      {deleteTenant && (
        <ConfirmDialog
          title="Xóa người thuê"
          message={`Tài khoản ${deleteTenant.fullName} sẽ bị vô hiệu hóa và không thể đăng nhập hoặc được chọn cho hợp đồng mới. Bạn xác nhận?`}
          onCancel={() => setDeleteTenant(undefined)}
          onConfirm={() =>
            tenantManagementApi
              .deactivate(deleteTenant.userId)
              .then(() => {
                setDeleteTenant(undefined);
                setSuccess('Đã xóa người thuê khỏi danh sách hoạt động.');
                setData((current: any) =>
                  current
                    ? {
                        ...current,
                        content: current.content.filter(
                          (tenant: TenantSummary) => tenant.userId !== deleteTenant.userId,
                        ),
                        totalElements: Math.max(0, current.totalElements - 1),
                      }
                    : current,
                );
              })
              .catch((e) => {
                setDeleteTenant(undefined);
                setError(getApiMessage(e));
              })
          }
        />
      )}
      {createOpen && (
        <Modal title="Tạo tài khoản người thuê" onClose={() => setCreateOpen(false)}>
          <form onSubmit={create} className="grid gap-4 sm:grid-cols-2">
            <Field label="Tên đăng nhập">
              <input
                required
                className={inputClass}
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
            </Field>
            <Field label="Mật khẩu ban đầu">
              <input
                required
                minLength={8}
                type="password"
                className={inputClass}
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </Field>
            <Field label="Họ và tên">
              <input
                required
                className={inputClass}
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </Field>
            <Field label="Số điện thoại">
              <input
                className={inputClass}
                value={form.phoneNumber}
                onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
              />
            </Field>
            <button className="sm:col-span-2 rounded-xl bg-teal-600 py-2.5 font-semibold text-white">
              Tạo tài khoản
            </button>
          </form>
        </Modal>
      )}
      {detail && (
        <Modal title={`Sửa thông tin — ${detail.fullName}`} onClose={() => setDetail(null)}>
          <TenantProfileEditor
            key={detail.userId}
            tenant={detail}
            onSaved={() => {
              setDetail(null);
              setError('');
              setSuccess('Đã cập nhật thông tin người thuê.');
              void load();
            }}
          />
        </Modal>
      )}
    </section>
  );
}

export function TenantProfilePage() {
  const [profile, setProfile] = useState<any>();
  const [form, setForm] = useState({ fullName: '', phoneNumber: '', contactAddress: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  useEffect(() => {
    tenantApi
      .profile()
      .then((p) => {
        setProfile(p);
        setForm({ fullName: p.fullName, phoneNumber: p.phoneNumber || '', contactAddress: p.contactAddress || '' });
      })
      .catch((e) => setError(getApiMessage(e)));
  }, []);
  const save = (e: React.FormEvent) => {
    e.preventDefault();
    tenantApi
      .updateProfile(form)
      .then((p) => {
        setProfile(p);
        setSuccess('Đã cập nhật hồ sơ.');
      })
      .catch((e) => setError(getApiMessage(e)));
  };
  return (
    <section className="max-w-2xl space-y-6">
      <div>
        <p className="text-sm font-semibold text-teal-600">HỒ SƠ CÁ NHÂN</p>
        <h1 className="mt-1 text-3xl font-bold">Thông tin người thuê</h1>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {success && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}
      <form onSubmit={save} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6">
        <Field label="Họ tên">
          <input
            required
            className={inputClass}
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          />
        </Field>
        <Field label="Email">
          <input disabled className={`${inputClass} bg-slate-50`} value={profile?.email || ''} />
        </Field>
        <Field label="Số điện thoại">
          <input
            className={inputClass}
            value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
          />
        </Field>
        <Field label="Địa chỉ liên hệ">
          <textarea
            className={inputClass}
            value={form.contactAddress}
            onChange={(e) => setForm({ ...form, contactAddress: e.target.value })}
          />
        </Field>
        <button className="rounded-xl bg-teal-600 px-5 py-2.5 font-semibold text-white">Lưu thay đổi</button>
      </form>
    </section>
  );
}
