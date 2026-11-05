import { useEffect, useState } from 'react';
import { getApiMessage, roomApi, utilityApi } from '../lib/api';
import { Badge, Field, Modal, Pagination, inputClass } from '../components/Ui';
const blank = {
  roomId: 0,
  utilityType: 'ELECTRICITY',
  billingPeriod: '',
  readingStart: 0,
  readingEnd: 0,
  readingDate: '',
  note: '',
};
export function UtilityAdminPage() {
  const [tab, setTab] = useState<'readings' | 'services' | 'fees'>('readings');
  const [data, setData] = useState<any>();
  const [rooms, setRooms] = useState<any[]>([]);
  const [modal, setModal] = useState<string | null>(null);
  const [form, setForm] = useState<any>(blank);
  const [editing, setEditing] = useState<any>();
  const [error, setError] = useState('');
  const load = () => {
    (tab === 'readings'
      ? utilityApi.readings({})
      : tab === 'services'
        ? utilityApi.services({})
        : utilityApi.feeConfigs({})
    )
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));
  };
  useEffect(() => {
    void load();
  }, [tab]);
  useEffect(() => {
    roomApi
      .search({})
      .then((x) => setRooms(x.content))
      .catch(() => undefined);
  }, []);
  const save = (e: React.FormEvent) => {
    e.preventDefault();
    const fn =
      tab === 'readings'
        ? editing
          ? utilityApi.updateReading(editing.id, form)
          : utilityApi.createReading(form)
        : tab === 'services'
          ? editing
            ? utilityApi.updateService(editing.id, form)
            : utilityApi.createService(form)
          : editing
            ? utilityApi.updateFeeConfig(editing.id, form)
            : utilityApi.createFeeConfig(form);
    fn.then(() => {
      setModal(null);
      setEditing(null);
      void load();
    }).catch((e) => setError(getApiMessage(e)));
  };
  const open = () => {
    setEditing(null);
    setForm(
      tab === 'readings'
        ? { ...blank }
        : tab === 'services'
          ? { serviceCode: '', serviceName: '', pricingMode: 'PER_ROOM', defaultUnitPrice: 0, active: true }
          : {
              feeCode: '',
              feeName: '',
              unitName: '',
              unitPrice: 0,
              pricingMode: 'PER_USAGE',
              effectiveFrom: '',
              effectiveTo: '',
              active: true,
            },
    );
    setModal(tab);
  };
  return (
    <section className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-sm font-semibold text-teal-600">ĐIỆN NƯỚC & DỊCH VỤ</p>
          <h1 className="mt-1 text-3xl font-bold">Quản lý vận hành</h1>
        </div>
        <button onClick={open} className="rounded-xl bg-teal-600 px-4 py-2.5 font-semibold text-white">
          + Thêm
        </button>
      </div>
      <div className="flex gap-2">
        {[
          ['readings', 'Chỉ số điện nước'],
          ['services', 'Dịch vụ'],
          ['fees', 'Đơn giá'],
        ].map(([v, l]) => (
          <button
            key={v}
            onClick={() => setTab(v as any)}
            className={`rounded-xl px-4 py-2 text-sm font-semibold ${tab === v ? 'bg-teal-600 text-white' : 'bg-white text-slate-500'}`}
          >
            {l}
          </button>
        ))}
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-x-auto rounded-2xl border bg-white">
        <table className="w-full text-left text-sm">
          <tbody>
            {data?.content?.map((x: any) => (
              <tr className="border-t" key={x.id}>
                <td className="px-5 py-4">
                  {tab === 'readings'
                    ? rooms.find((r) => r.id === x.roomId)?.roomCode || `Phòng ${x.roomId}`
                    : tab === 'services'
                      ? x.serviceName
                      : x.feeName}
                </td>
                <td>
                  {tab === 'readings'
                    ? `${x.utilityType} · ${x.billingPeriod}`
                    : tab === 'services'
                      ? `${x.serviceCode} · ${Number(x.defaultUnitPrice).toLocaleString('vi-VN')} đ`
                      : `${x.feeCode} · ${Number(x.unitPrice).toLocaleString('vi-VN')} đ`}
                </td>
                <td>
                  <button
                    className="text-teal-700"
                    onClick={() => {
                      setEditing(x);
                      setForm({ ...x });
                      setModal(tab);
                    }}
                  >
                    Sửa
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có dữ liệu.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={() => undefined} />
          </div>
        )}
      </div>
      {modal && (
        <Modal title={editing ? 'Cập nhật' : 'Thêm mới'} onClose={() => setModal(null)}>
          <form onSubmit={save} className="grid gap-4 sm:grid-cols-2">
            {modal === 'readings' && (
              <>
                <Field label="Phòng">
                  <select
                    required
                    className={inputClass}
                    value={form.roomId}
                    onChange={(e) => setForm({ ...form, roomId: Number(e.target.value) })}
                  >
                    <option value={0}>Chọn phòng</option>
                    {rooms.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.roomCode}
                        {r.roomName ? ` — ${r.roomName}` : ''}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Loại">
                  <select
                    className={inputClass}
                    value={form.utilityType}
                    onChange={(e) => setForm({ ...form, utilityType: e.target.value })}
                  >
                    <option>ELECTRICITY</option>
                    <option>WATER</option>
                  </select>
                </Field>
                <Field label="Kỳ (YYYY-MM)">
                  <input
                    required
                    placeholder="Ví dụ: 2026-10"
                    className={inputClass}
                    value={form.billingPeriod}
                    onChange={(e) => setForm({ ...form, billingPeriod: e.target.value })}
                  />
                </Field>
                <Field label="Chỉ số đầu">
                  <input
                    required
                    type="number"
                    className={inputClass}
                    value={form.readingStart}
                    onChange={(e) => setForm({ ...form, readingStart: Number(e.target.value) })}
                  />
                </Field>
                <Field label="Chỉ số cuối">
                  <input
                    required
                    type="number"
                    className={inputClass}
                    value={form.readingEnd}
                    onChange={(e) => setForm({ ...form, readingEnd: Number(e.target.value) })}
                  />
                </Field>
                <Field label="Ngày ghi">
                  <input
                    required
                    type="date"
                    className={inputClass}
                    value={form.readingDate}
                    onChange={(e) => setForm({ ...form, readingDate: e.target.value })}
                  />
                </Field>
              </>
            )}
            {modal === 'services' && (
              <>
                <Field label="Mã">
                  <input
                    required
                    className={inputClass}
                    value={form.serviceCode}
                    onChange={(e) => setForm({ ...form, serviceCode: e.target.value })}
                  />
                </Field>
                <Field label="Tên">
                  <input
                    required
                    className={inputClass}
                    value={form.serviceName}
                    onChange={(e) => setForm({ ...form, serviceName: e.target.value })}
                  />
                </Field>
                <Field label="Đơn giá">
                  <input
                    required
                    type="number"
                    className={inputClass}
                    value={form.defaultUnitPrice}
                    onChange={(e) => setForm({ ...form, defaultUnitPrice: Number(e.target.value) })}
                  />
                </Field>
              </>
            )}
            {modal === 'fees' && (
              <>
                <Field label="Mã phí">
                  <input
                    required
                    className={inputClass}
                    value={form.feeCode}
                    onChange={(e) => setForm({ ...form, feeCode: e.target.value })}
                  />
                </Field>
                <Field label="Tên phí">
                  <input
                    required
                    className={inputClass}
                    value={form.feeName}
                    onChange={(e) => setForm({ ...form, feeName: e.target.value })}
                  />
                </Field>
                <Field label="Đơn giá">
                  <input
                    required
                    type="number"
                    className={inputClass}
                    value={form.unitPrice}
                    onChange={(e) => setForm({ ...form, unitPrice: Number(e.target.value) })}
                  />
                </Field>
                <Field label="Từ ngày">
                  <input
                    required
                    type="date"
                    className={inputClass}
                    value={form.effectiveFrom}
                    onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
                  />
                </Field>
              </>
            )}
            <button className="sm:col-span-2 rounded-xl bg-teal-600 py-2.5 font-semibold text-white">Lưu</button>
          </form>
        </Modal>
      )}
    </section>
  );
}
