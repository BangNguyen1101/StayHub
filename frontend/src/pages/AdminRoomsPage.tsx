import { useEffect, useMemo, useState } from 'react';
import { Building2, CircleDot, Plus, Search, Wrench } from 'lucide-react';
import { getApiMessage, roomApi } from '../lib/api';
import type { Room } from '../types/api';
import { Field, Modal, inputClass } from '../components/Ui';
import { RoomDetails } from '../components/RoomDetails';

const empty = {
  roomCode: '',
  roomName: '',
  floorNumber: 1,
  areaM2: 20,
  baseRent: 0,
  maxOccupants: 1,
  allowsSharing: false,
  notes: '',
};
const statuses = [
  ['', 'Tất cả phòng'],
  ['VACANT', 'Còn trống'],
  ['OCCUPIED', 'Đang thuê'],
  ['FULL', 'Đã đủ người'],
  ['MAINTENANCE', 'Bảo trì'],
  ['INACTIVE', 'Ngừng hoạt động'],
] as const;
const tones: Record<string, string> = {
  VACANT: 'border-emerald-200 bg-emerald-50 text-emerald-800 hover:bg-emerald-100',
  OCCUPIED: 'border-blue-200 bg-blue-100 text-blue-800 hover:bg-blue-200',
  FULL: 'border-violet-200 bg-violet-100 text-violet-800 hover:bg-violet-200',
  MAINTENANCE: 'border-amber-200 bg-amber-100 text-amber-800 hover:bg-amber-200',
  INACTIVE: 'border-slate-200 bg-slate-100 text-slate-500 hover:bg-slate-200',
};
const statusName = (status: string) => statuses.find(([value]) => value === status)?.[1] || status;

export function AdminRoomsPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [form, setForm] = useState<any>(empty);
  const [editing, setEditing] = useState<Room | null>(null);
  const [selected, setSelected] = useState<Room | null>(null);
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { keyword: keyword || undefined, status: filter || undefined, size: 100 };
      const first = await roomApi.search({ ...params, page: 0 });
      const remaining = await Promise.all(
        Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, index) =>
          roomApi.search({ ...params, page: index + 1 }),
        ),
      );
      setRooms([first, ...remaining].flatMap((page) => page.content));
    } catch (response) {
      setError(getApiMessage(response));
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [filter]);
  const floors = useMemo(() => {
    const result = new Map<number, Room[]>();
    rooms.forEach((room) => {
      const floor = room.floorNumber || 0;
      result.set(floor, [...(result.get(floor) || []), room]);
    });
    return [...result.entries()]
      .sort(([a], [b]) => b - a)
      .map(([floor, units]) => [floor, units.sort((a, b) => a.roomCode.localeCompare(b.roomCode, 'vi'))] as const);
  }, [rooms]);
  const counts = useMemo(
    () => ({
      total: rooms.length,
      vacant: rooms.filter((room) => room.status === 'VACANT').length,
      occupied: rooms.filter((room) => ['OCCUPIED', 'FULL'].includes(room.status)).length,
      maintenance: rooms.filter((room) => room.status === 'MAINTENANCE').length,
    }),
    [rooms],
  );
  const close = () => {
    setOpen(false);
    setEditing(null);
    setForm({ ...empty });
  };
  const save = (event: React.FormEvent) => {
    event.preventDefault();
    const request = editing ? roomApi.update(editing.id, form) : roomApi.create(form);
    request
      .then(() => {
        close();
        void load();
      })
      .catch((response) => setError(getApiMessage(response)));
  };
  const startEdit = (room: Room) => {
    setEditing(room);
    setForm({ ...room });
    setOpen(true);
  };
  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-bold tracking-wider text-teal-600">QUẢN LÝ PHÒNG</p>
          <h1 className="mt-1 text-3xl font-bold">Sơ đồ nhà trọ 2D</h1>
          <p className="mt-2 text-sm text-slate-500">Phòng mới sẽ tự động hiển thị theo tầng trong sơ đồ.</p>
        </div>
        <button
          onClick={() => {
            setEditing(null);
            setForm({ ...empty });
            setOpen(true);
          }}
          className="primary-button flex items-center gap-2"
        >
          <Plus size={17} /> Thêm phòng
        </button>
      </div>
      <div className="grid gap-px overflow-hidden rounded-2xl border bg-slate-200 sm:grid-cols-2 xl:grid-cols-4">
        {[
          [Building2, 'Tổng số phòng', counts.total, 'text-slate-800'],
          [CircleDot, 'Còn trống', counts.vacant, 'text-emerald-700'],
          [Building2, 'Đang thuê', counts.occupied, 'text-blue-700'],
          [Wrench, 'Đang bảo trì', counts.maintenance, 'text-amber-700'],
        ].map(([Icon, label, value, color]: any) => (
          <div className="flex items-center gap-3 bg-white p-4" key={label}>
            <span className={'grid h-10 w-10 place-items-center rounded-xl bg-slate-50 ' + color}>
              <Icon size={19} />
            </span>
            <div>
              <p className="text-xs text-slate-500">{label}</p>
              <p className={'text-xl font-bold ' + color}>{value}</p>
            </div>
          </div>
        ))}
      </div>
      <div className="flex flex-col gap-3 rounded-2xl border bg-white p-3 shadow-sm md:flex-row">
        <div className="input-wrap m-0 flex-1 py-2">
          <Search size={16} />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && void load()}
            placeholder="Tìm mã hoặc tên phòng..."
          />
        </div>
        <div className="flex flex-wrap gap-2">
          {statuses.map(([value, label]) => (
            <button
              key={value}
              onClick={() => setFilter(value)}
              className={
                'rounded-xl px-3 py-2 text-xs font-semibold transition ' +
                (filter === value ? 'bg-teal-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200')
              }
            >
              {label}
            </button>
          ))}
        </div>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="grid gap-5 xl:grid-cols-[1fr_290px]">
        <section className="overflow-hidden rounded-2xl border bg-white shadow-sm">
          <div className="flex items-center justify-between border-b p-5">
            <div>
              <h2 className="font-bold">Tòa nhà</h2>
              <p className="mt-1 text-sm text-slate-400">Nhấn vào ô phòng để xem chi tiết.</p>
            </div>
            <span className="rounded-full bg-teal-50 px-3 py-1 text-xs font-semibold text-teal-700">
              {counts.total} phòng
            </span>
          </div>
          {loading ? (
            <p className="p-12 text-center text-sm text-slate-400">Đang tải sơ đồ phòng...</p>
          ) : floors.length ? (
            <div className="space-y-5 p-4 sm:p-6">
              {floors.map(([floor, units]) => (
                <div key={floor} className="grid gap-3 md:grid-cols-[100px_1fr]">
                  <div className="flex items-center rounded-xl bg-slate-100 px-3 py-2 text-sm font-bold text-slate-600">
                    Tầng {floor}
                  </div>
                  <div className="grid grid-cols-3 gap-2 sm:grid-cols-5 lg:grid-cols-8">
                    {units.map((room) => (
                      <button
                        title={room.roomCode + ' — ' + statusName(room.status)}
                        onClick={() => setSelected(room)}
                        className={
                          'group relative min-h-18 rounded-xl border p-2 text-left transition ' +
                          (tones[room.status] || tones.INACTIVE)
                        }
                        key={room.id}
                      >
                        <span className="block text-sm font-bold">{room.roomCode}</span>
                        <span className="mt-1 block truncate text-[10px] font-medium opacity-75">
                          {room.roomName || statusName(room.status)}
                        </span>
                        <span className="absolute bottom-2 right-2 h-2 w-2 rounded-full bg-current opacity-60" />
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="p-12 text-center text-sm text-slate-400">Chưa có phòng phù hợp.</p>
          )}
        </section>
        <aside className="rounded-2xl border bg-white p-5 shadow-sm">
          <h2 className="font-bold">Chú thích trạng thái</h2>
          <div className="mt-5 space-y-3">
            {statuses.slice(1).map(([value, label]) => (
              <div className="flex items-center justify-between text-sm" key={value}>
                <span>{label}</span>
                <b>{rooms.filter((room) => room.status === value).length}</b>
              </div>
            ))}
          </div>
          <div className="mt-6 rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-500">
            Màu phòng lấy từ trạng thái thực tế. Khi tạo phòng, chỉ cần chọn tầng để phòng xuất hiện tại đây.
          </div>
        </aside>
      </div>
      {open && (
        <Modal title={editing ? 'Sửa phòng' : 'Thêm phòng'} onClose={close}>
          <form onSubmit={save} className="grid gap-4 sm:grid-cols-2">
            {[
              ['roomCode', 'Mã phòng'],
              ['roomName', 'Tên phòng'],
              ['floorNumber', 'Tầng'],
              ['areaM2', 'Diện tích (m²)'],
              ['baseRent', 'Giá thuê'],
              ['maxOccupants', 'Sức chứa'],
            ].map(([key, label]) => (
              <Field key={key} label={label}>
                <input
                  required={key !== 'roomName'}
                  type={['floorNumber', 'areaM2', 'baseRent', 'maxOccupants'].includes(key) ? 'number' : 'text'}
                  step="0.01"
                  className={inputClass}
                  value={form[key]}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      [key]: ['floorNumber', 'areaM2', 'baseRent', 'maxOccupants'].includes(key)
                        ? Number(event.target.value)
                        : event.target.value,
                    })
                  }
                />
              </Field>
            ))}
            <Field label="Cho ở ghép">
              <input
                type="checkbox"
                checked={form.allowsSharing}
                onChange={(event) => setForm({ ...form, allowsSharing: event.target.checked })}
              />
            </Field>
            <Field label="Ghi chú">
              <textarea
                className={inputClass + ' sm:col-span-2'}
                value={form.notes || ''}
                onChange={(event) => setForm({ ...form, notes: event.target.value })}
              />
            </Field>
            <button className="primary-button sm:col-span-2">Lưu phòng</button>
          </form>
        </Modal>
      )}
      {selected && (
        <Modal title={'Phòng ' + selected.roomCode} onClose={() => setSelected(null)}>
          <RoomDetails room={selected} />
          <div className="mt-5 flex justify-end border-t pt-5">
            <button
              className="primary-button"
              onClick={() => {
                setSelected(null);
                startEdit(selected);
              }}
            >
              Sửa phòng
            </button>
          </div>
        </Modal>
      )}
    </section>
  );
}
