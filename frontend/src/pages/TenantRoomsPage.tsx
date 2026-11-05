import { useEffect, useState } from 'react';
import { getApiMessage, rentalApi, roomApi } from '../lib/api';
import type { Room } from '../types/api';
import { Badge, Field, Modal, Pagination, inputClass } from '../components/Ui';
export function TenantRoomsPage() {
  const [data, setData] = useState<any>();
  const [room, setRoom] = useState<Room | null>(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    expectedOccupantCount: 1,
    expectedMoveInDate: '',
    note: '',
  });
  const load = () =>
    roomApi
      .available(page, 10)
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));
  useEffect(() => {
    void load();
  }, [page]);
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!room) return;
    rentalApi
      .submit(room.id, form)
      .then(() => {
        setSuccess('Đã gửi yêu cầu thuê phòng.');
        setRoom(null);
        void load();
      })
      .catch((e) => setError(getApiMessage(e)));
  };
  return (
    <section className="space-y-6">
      <div>
        <p className="text-sm font-semibold text-teal-600">PHÒNG CÓ THỂ THUÊ</p>
        <h1 className="mt-1 text-3xl font-bold">Chọn phòng phù hợp</h1>
      </div>
      {success && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">{success}</p>}
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {data?.content?.map((r: Room) => (
          <div key={r.id} className="rounded-2xl border border-slate-200 bg-white p-5">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-lg font-bold">{r.roomCode}</h2>
                <p className="text-sm text-slate-400">{r.roomName || 'Phòng cho thuê'}</p>
              </div>
              <Badge value={r.status} />
            </div>
            <div className="mt-5 grid grid-cols-2 gap-3 text-sm text-slate-600">
              <span>{r.areaM2} m²</span>
              <span>{r.maxOccupants} người</span>
              <span className="font-semibold text-slate-900">{Number(r.baseRent).toLocaleString('vi-VN')} đ/tháng</span>
              <span>{r.allowsSharing ? 'Có ở ghép' : 'Không ở ghép'}</span>
            </div>
            <button
              onClick={() => setRoom(r)}
              className="mt-5 w-full rounded-xl bg-teal-600 py-2.5 text-sm font-semibold text-white"
            >
              Gửi yêu cầu thuê
            </button>
          </div>
        ))}
      </div>
      {!data?.content?.length && (
        <div className="rounded-2xl bg-white p-12 text-center text-slate-400">Hiện chưa có phòng trống.</div>
      )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}{' '}
      {room && (
        <Modal title={`Đăng ký thuê ${room.roomCode}`} onClose={() => setRoom(null)}>
          <form onSubmit={submit} className="space-y-4">
            <Field label="Họ tên liên hệ">
              <input
                required
                className={inputClass}
                value={form.contactName}
                onChange={(e) => setForm({ ...form, contactName: e.target.value })}
              />
            </Field>
            <Field label="Số điện thoại">
              <input
                required
                className={inputClass}
                value={form.contactPhone}
                onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
              />
            </Field>
            <Field label="Email">
              <input
                type="email"
                className={inputClass}
                value={form.contactEmail}
                onChange={(e) => setForm({ ...form, contactEmail: e.target.value })}
              />
            </Field>
            <Field label="Số người dự kiến">
              <input
                required
                min="1"
                type="number"
                className={inputClass}
                value={form.expectedOccupantCount}
                onChange={(e) => setForm({ ...form, expectedOccupantCount: Number(e.target.value) })}
              />
            </Field>
            <Field label="Ngày dự kiến nhận phòng">
              <input
                required
                type="date"
                className={inputClass}
                value={form.expectedMoveInDate}
                onChange={(e) => setForm({ ...form, expectedMoveInDate: e.target.value })}
              />
            </Field>
            <Field label="Ghi chú">
              <textarea
                className={inputClass}
                value={form.note}
                onChange={(e) => setForm({ ...form, note: e.target.value })}
              />
            </Field>
            <button className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white">Gửi yêu cầu</button>
          </form>
        </Modal>
      )}
    </section>
  );
}
