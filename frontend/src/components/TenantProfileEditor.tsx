import { useState, type FormEvent } from 'react';
import { getApiMessage, tenantManagementApi } from '../lib/api';
import type { TenantDetail } from '../types/api';
import { Field, inputClass } from './Ui';

export function TenantProfileEditor({
  tenant,
  onSaved,
}: {
  tenant: TenantDetail;
  onSaved: (tenant: TenantDetail) => void;
}) {
  const [form, setForm] = useState({
    email: tenant.email,
    fullName: tenant.fullName,
    phoneNumber: tenant.phoneNumber ?? '',
    contactAddress: tenant.contactAddress ?? '',
    identityDocumentType: tenant.identityDocumentType ?? 'CCCD',
    identityDocumentNumber: tenant.identityDocumentNumber ?? '',
    identityIssuedDate: tenant.identityIssuedDate ?? '',
    identityIssuedPlace: tenant.identityIssuedPlace ?? '',
    notes: tenant.notes ?? '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function save(event: FormEvent) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError('');
    try {
      const updated = await tenantManagementApi.update(tenant.userId, {
        ...form,
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        identityIssuedDate: form.identityIssuedDate || null,
      });
      onSaved(updated);
    } catch (err) {
      setError(getApiMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={save} className="space-y-4">
      <p className="text-sm text-slate-500">Tài khoản: {tenant.username}</p>
      <fieldset disabled={saving} className="grid gap-4 sm:grid-cols-2">
        <Field label="Họ và tên">
          <input
            required
            maxLength={200}
            className={inputClass}
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
          />
        </Field>
        <Field label="Số điện thoại">
          <input
            maxLength={30}
            className={inputClass}
            value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
          />
        </Field>
        <Field label="Email">
          <input
            required
            type="email"
            maxLength={320}
            className={inputClass}
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </Field>
        <Field label="Loại giấy tờ">
          <input
            maxLength={50}
            className={inputClass}
            value={form.identityDocumentType}
            onChange={(e) => setForm({ ...form, identityDocumentType: e.target.value })}
          />
        </Field>
        <Field label="Số CCCD / giấy tờ">
          <input
            maxLength={100}
            className={inputClass}
            value={form.identityDocumentNumber}
            onChange={(e) => setForm({ ...form, identityDocumentNumber: e.target.value })}
          />
        </Field>
        <Field label="Ngày cấp">
          <input
            type="date"
            className={inputClass}
            value={form.identityIssuedDate}
            onChange={(e) => setForm({ ...form, identityIssuedDate: e.target.value })}
          />
        </Field>
        <div className="sm:col-span-2">
          <Field label="Địa chỉ liên hệ">
            <textarea
              maxLength={500}
              className={inputClass}
              value={form.contactAddress}
              onChange={(e) => setForm({ ...form, contactAddress: e.target.value })}
            />
          </Field>
        </div>
        <div className="sm:col-span-2">
          <Field label="Nơi cấp CCCD / giấy tờ">
            <input
              maxLength={200}
              className={inputClass}
              value={form.identityIssuedPlace}
              onChange={(e) => setForm({ ...form, identityIssuedPlace: e.target.value })}
            />
          </Field>
        </div>
        <div className="sm:col-span-2">
          <Field label="Ghi chú">
            <textarea
              maxLength={1000}
              className={inputClass}
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </Field>
        </div>
      </fieldset>
      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
      <button
        disabled={saving}
        className="rounded-xl bg-teal-600 px-5 py-2.5 font-semibold text-white disabled:opacity-50"
      >
        {saving ? 'Đang lưu...' : 'Lưu thông tin'}
      </button>
    </form>
  );
}
