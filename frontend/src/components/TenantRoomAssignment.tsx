import { useEffect, useState } from 'react';
import { contractApi, getApiMessage } from '../lib/api';
import type { Contract, TenantSummary } from '../types/api';
import { Field, inputClass } from './Ui';
export function TenantRoomAssignment({ tenant }: { tenant: TenantSummary }) {
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [contractId, setContractId] = useState('');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [error, setError] = useState('');
  const [ok, setOk] = useState(false);
  useEffect(() => {
    contractApi
      .adminSearch({ status: 'ACTIVE', page: 0, size: 200 })
      .then((x) => setContracts(x.content))
      .catch((e) => setError(getApiMessage(e)));
  }, []);
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const c = contracts.find((x) => x.id === Number(contractId));
    if (!c) return;
    contractApi
      .addOccupant(c.id, {
        userId: tenant.userId,
        fullName: tenant.fullName,
        phoneNumber: tenant.phoneNumber || '',
        primaryTenant: false,
        moveInDate: date,
      })
      .then(() => setOk(true))
      .catch((e) => setError(getApiMessage(e)));
  };
  if (ok)
    return (
      <p role="status" className="rounded-xl bg-emerald-50 p-3 text-emerald-700">
        Đã gán {tenant.fullName} vào phòng {contracts.find((c) => c.id === Number(contractId))?.roomCode} trong hợp đồng
        đã có.
      </p>
    );
  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm text-slate-600">
        Chọn hợp đồng/phòng đã tạo để thêm <b>{tenant.fullName}</b> làm người ở cùng.
      </p>
      <Field label="Hợp đồng và phòng">
        <select required className={inputClass} value={contractId} onChange={(e) => setContractId(e.target.value)}>
          <option value="">Chọn phòng</option>
          {contracts.map((c) => (
            <option key={c.id} value={c.id}>
              {c.roomCode} — {c.contractCode} ({c.tenantName || 'người thuê chính'})
            </option>
          ))}
        </select>
      </Field>
      <Field label="Ngày bắt đầu ở">
        <input required type="date" className={inputClass} value={date} onChange={(e) => setDate(e.target.value)} />
      </Field>
      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
      <button
        disabled={!contractId}
        className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
      >
        Gán vào phòng
      </button>
    </form>
  );
}
