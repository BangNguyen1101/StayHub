import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react';
import { Clock3, ImagePlus, Search, UserRound, Wrench } from 'lucide-react';
import { getApiMessage, maintenanceApi } from '../lib/api';
import type { MaintenanceAssignee, MaintenanceAttachment, MaintenanceHistory, MaintenanceRequest } from '../types/api';
import { Badge, Field, Modal, inputClass } from '../components/Ui';

const issueTypes = [
  ['ELECTRICITY', 'Điện / bóng đèn'],
  ['WATER', 'Nước / vòi nước'],
  ['EQUIPMENT', 'Thiết bị trong phòng'],
  ['OTHER', 'Sự cố khác'],
] as const;
const statuses = [
  ['OPEN', 'Mới tiếp nhận'],
  ['IN_PROGRESS', 'Đang xử lý'],
  ['WAITING', 'Chờ thông tin/vật tư'],
  ['RESOLVED', 'Đã xử lý'],
  ['CLOSED', 'Đã đóng'],
  ['REJECTED', 'Từ chối'],
] as const;
const priorities = [
  ['LOW', 'Thấp'],
  ['NORMAL', 'Bình thường'],
  ['HIGH', 'Cao'],
  ['URGENT', 'Khẩn cấp'],
] as const;
const labelOf = (items: readonly (readonly [string, string])[], value?: string | null) =>
  items.find((item) => item[0] === value)?.[1] || value || '—';
const titleOf = (item: MaintenanceRequest) =>
  item.title.replace(/^(Điện \/ bóng đèn|Nước \/ vòi nước|Thiết bị trong phòng|Sự cố khác):\s*/, '');
const elapsed = (from: string, to?: string | null) => {
  const h = Math.max(0, Math.round((new Date(to || Date.now()).getTime() - new Date(from).getTime()) / 36e5));
  return h < 24 ? `${h} giờ` : `${Math.floor(h / 24)} ngày ${h % 24} giờ`;
};
const formatDate = (value?: string | null) =>
  value ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)) : '—';

function RequestCard({ request, onOpen }: { request: MaintenanceRequest; onOpen: (item: MaintenanceRequest) => void }) {
  return (
    <button
      onClick={() => onOpen(request)}
      className="w-full rounded-xl border border-slate-200 bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:border-teal-300 hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-2">
        <p className="font-semibold text-slate-800">{request.roomCode || `Phòng ${request.roomId}`}</p>
        <Badge value={request.priority} />
      </div>
      <p className="mt-1 text-xs text-slate-400">{request.requestCode}</p>
      <p className="mt-3 line-clamp-2 text-sm font-medium text-slate-700">{titleOf(request)}</p>
      <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
        <span className="flex items-center gap-1">
          <Clock3 size={13} /> {elapsed(request.createdAt, request.resolvedAt)}
        </span>
        <span className="flex items-center gap-1">
          <ImagePlus size={13} /> {request.attachments?.length || 0}
        </span>
      </div>
      <div className="mt-3 flex items-center justify-between border-t pt-3 text-xs">
        <span className="text-slate-500">{request.reporterName || 'Người thuê'}</span>
        <span className="font-medium text-teal-700">{request.assignedToName || 'Chưa giao'}</span>
      </div>
    </button>
  );
}

export function MaintenancePage() {
  const admin = location.pathname.startsWith('/admin');
  const [requests, setRequests] = useState<MaintenanceRequest[]>([]);
  const [assignees, setAssignees] = useState<MaintenanceAssignee[]>([]);
  const [keyword, setKeyword] = useState('');
  const [filter, setFilter] = useState('');
  const [error, setError] = useState('');
  const [detail, setDetail] = useState<MaintenanceRequest | null>(null);
  const [history, setHistory] = useState<MaintenanceHistory[]>([]);
  const [imageUrls, setImageUrls] = useState<Record<number, string>>({});
  const [open, setOpen] = useState(false);
  const [files, setFiles] = useState<File[]>([]);
  const [form, setForm] = useState({ issueType: 'OTHER', title: '', description: '', priority: 'NORMAL' });
  const [adminForm, setAdminForm] = useState<any>({
    status: 'OPEN',
    assignedToUserId: '',
    estimatedCost: '',
    actualCost: '',
    resolutionNote: '',
    rejectionReason: '',
    note: '',
  });
  const load = () =>
    (admin
      ? maintenanceApi.adminList({ keyword: keyword || undefined, status: filter || undefined, size: 100 })
      : maintenanceApi.tenantList({ keyword: keyword || undefined, status: filter || undefined, size: 100 })
    )
      .then((data) => setRequests(data.content))
      .catch((response) => setError(getApiMessage(response)));
  useEffect(() => {
    void load();
    if (admin)
      maintenanceApi
        .assignees()
        .then(setAssignees)
        .catch(() => undefined);
  }, [admin, filter]);
  useEffect(() => () => Object.values(imageUrls).forEach(URL.revokeObjectURL), [imageUrls]);
  const visible = useMemo(
    () =>
      requests.filter(
        (item) =>
          !keyword ||
          `${item.requestCode} ${item.roomCode} ${item.title} ${item.reporterName}`
            .toLowerCase()
            .includes(keyword.toLowerCase()),
      ),
    [requests, keyword],
  );
  const openDetail = (request: MaintenanceRequest) => {
    setError('');
    setDetail(request);
    setHistory([]);
    setImageUrls({});
    if (admin)
      maintenanceApi
        .history(request.id)
        .then(setHistory)
        .catch((response) => setError(getApiMessage(response)));
    request.attachments?.forEach((file) =>
      maintenanceApi
        .attachmentImage(file.id, admin)
        .then((blob) => setImageUrls((current) => ({ ...current, [file.id]: URL.createObjectURL(blob) })))
        .catch(() => undefined),
    );
    setAdminForm({
      status: request.status,
      assignedToUserId: request.assignedToUserId || '',
      estimatedCost: request.estimatedCost ?? '',
      actualCost: request.actualCost ?? '',
      resolutionNote: request.resolutionNote ?? '',
      rejectionReason: request.rejectionReason ?? '',
      note: '',
    });
  };
  const create = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    try {
      const created = await maintenanceApi.create({
        ...form,
        title: `${labelOf(issueTypes, form.issueType)}: ${form.title.trim()}`,
      });
      if (files.length) await maintenanceApi.uploadImages(created.id, files);
      setOpen(false);
      setFiles([]);
      setForm({ issueType: 'OTHER', title: '', description: '', priority: 'NORMAL' });
      await load();
    } catch (response) {
      setError(getApiMessage(response));
    }
  };
  const update = async () => {
    if (!detail) return;
    setError('');
    try {
      await maintenanceApi.update(detail.id, {
        ...adminForm,
        assignedToUserId: adminForm.assignedToUserId ? Number(adminForm.assignedToUserId) : null,
        estimatedCost: adminForm.estimatedCost === '' ? null : Number(adminForm.estimatedCost),
        actualCost: adminForm.actualCost === '' ? null : Number(adminForm.actualCost),
      });
      setDetail(null);
      await load();
    } catch (response) {
      setError(getApiMessage(response));
    }
  };
  const columns = admin
    ? ([
        [
          'Chưa phân công',
          'border-amber-300',
          visible.filter((item) => item.status === 'OPEN' && !item.assignedToUserId),
        ],
        [
          'Đang xử lý',
          'border-blue-300',
          visible.filter(
            (item) =>
              item.status === 'IN_PROGRESS' ||
              item.status === 'WAITING' ||
              (item.status === 'OPEN' && !!item.assignedToUserId),
          ),
        ],
        [
          'Hoàn tất',
          'border-emerald-300',
          visible.filter((item) => ['RESOLVED', 'CLOSED', 'REJECTED'].includes(item.status)),
        ],
      ] as const)
    : ([['Yêu cầu của tôi', 'border-teal-300', visible]] as const);
  return (
    <section className="space-y-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm font-bold tracking-wider text-teal-600">BẢO TRÌ THÔNG MINH</p>
          <h1 className="mt-1 text-3xl font-bold">{admin ? 'Quản lý sự cố & bảo trì' : 'Yêu cầu bảo trì của tôi'}</h1>
          <p className="mt-2 text-sm text-slate-500">
            {admin
              ? 'Phân loại, giao việc và theo dõi toàn bộ sự cố.'
              : 'Gửi ảnh và mô tả sự cố để chủ nhà xử lý nhanh hơn.'}
          </p>
        </div>
        {!admin && (
          <button
            onClick={() => {
              setError('');
              setOpen(true);
            }}
            className="primary-button flex items-center justify-center gap-2"
          >
            <Wrench size={17} /> Báo sự cố
          </button>
        )}
      </div>
      <div className="flex flex-col gap-3 rounded-2xl border bg-white p-3 shadow-sm md:flex-row">
        <div className="input-wrap m-0 flex-1 py-2">
          <Search size={16} />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Tìm mã yêu cầu, phòng, nội dung..."
          />
        </div>
        <select
          className={`${inputClass} w-full md:w-56`}
          value={filter}
          onChange={(event) => setFilter(event.target.value)}
        >
          <option value="">Tất cả trạng thái</option>
          {statuses.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className={`grid gap-5 ${admin ? 'xl:grid-cols-3' : ''}`}>
        {columns.map(([title, border, items]) => (
          <div key={title} className={`min-w-0 rounded-2xl border-t-4 ${border} bg-slate-100/70 p-4`}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-bold">{title}</h2>
              <span className="rounded-full bg-white px-2.5 py-1 text-xs font-bold text-slate-500">{items.length}</span>
            </div>
            <div className="space-y-3">
              {items.map((item) => (
                <RequestCard key={item.id} request={item} onOpen={openDetail} />
              ))}
              {!items.length && (
                <p className="rounded-xl border border-dashed bg-white/70 p-8 text-center text-sm text-slate-400">
                  Chưa có sự cố.
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
      {open && (
        <Modal title="Báo sự cố mới" onClose={() => setOpen(false)}>
          <form onSubmit={create} className="space-y-4">
            <Field label="Phân loại sự cố">
              <select
                className={inputClass}
                value={form.issueType}
                onChange={(event) => setForm({ ...form, issueType: event.target.value })}
              >
                {issueTypes.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Tiêu đề">
              <input
                required
                maxLength={280}
                className={inputClass}
                value={form.title}
                placeholder="Ví dụ: Vòi nước bị rò rỉ"
                onChange={(event) => setForm({ ...form, title: event.target.value })}
              />
            </Field>
            <Field label="Mô tả">
              <textarea
                required
                className={`${inputClass} min-h-24`}
                value={form.description}
                placeholder="Vị trí, mức độ và thời điểm phát hiện..."
                onChange={(event) => setForm({ ...form, description: event.target.value })}
              />
            </Field>
            <Field label="Mức độ ưu tiên">
              <select
                className={inputClass}
                value={form.priority}
                onChange={(event) => setForm({ ...form, priority: event.target.value })}
              >
                {priorities.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Ảnh sự cố (tối đa 5 ảnh, mỗi ảnh 5 MB)">
              <input
                className="block w-full text-sm"
                accept="image/jpeg,image/png,image/webp"
                multiple
                type="file"
                onChange={(event: ChangeEvent<HTMLInputElement>) =>
                  setFiles(Array.from(event.target.files || []).slice(0, 5))
                }
              />
              {files.length > 0 && (
                <p className="mt-2 text-xs text-teal-700">Đã chọn: {files.map((file) => file.name).join(', ')}</p>
              )}
            </Field>
            <button className="primary-button w-full">Gửi yêu cầu</button>
          </form>
        </Modal>
      )}
      {detail && (
        <Modal title={`Sự cố ${detail.requestCode}`} onClose={() => setDetail(null)}>
          <div className="space-y-5 text-sm">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge value={detail.status} />
                <Badge value={detail.priority} />
                <span className="text-slate-400">{labelOf(issueTypes, detail.issueType)}</span>
              </div>
              <h3 className="mt-3 text-lg font-bold">{titleOf(detail)}</h3>
              <p className="mt-2 whitespace-pre-wrap text-slate-600">{detail.description}</p>
            </div>
            <div className="grid gap-2 rounded-xl bg-slate-50 p-3 sm:grid-cols-2">
              <p>
                Phòng: <b>{detail.roomCode || `Phòng ${detail.roomId}`}</b>
              </p>
              <p>
                Người báo: <b>{detail.reporterName || '—'}</b>
              </p>
              <p className="flex items-center gap-1">
                <Clock3 size={14} /> Thời gian xử lý: <b>{elapsed(detail.createdAt, detail.resolvedAt)}</b>
              </p>
              <p className="flex items-center gap-1">
                <UserRound size={14} /> Phụ trách: <b>{detail.assignedToName || 'Chưa phân công'}</b>
              </p>
            </div>
            {detail.attachments?.length > 0 && (
              <div>
                <p className="mb-2 font-semibold">Ảnh sự cố</p>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  {detail.attachments.map((file: MaintenanceAttachment) =>
                    imageUrls[file.id] ? (
                      <a key={file.id} href={imageUrls[file.id]} target="_blank" rel="noreferrer">
                        <img
                          className="h-28 w-full rounded-xl border object-cover"
                          src={imageUrls[file.id]}
                          alt={file.originalFileName}
                        />
                      </a>
                    ) : (
                      <div
                        key={file.id}
                        className="grid h-28 place-items-center rounded-xl bg-slate-100 text-xs text-slate-400"
                      >
                        Đang tải ảnh...
                      </div>
                    ),
                  )}
                </div>
              </div>
            )}
            {admin && (
              <>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Giao người xử lý">
                    <select
                      className={inputClass}
                      value={adminForm.assignedToUserId}
                      onChange={(event) => setAdminForm({ ...adminForm, assignedToUserId: event.target.value })}
                    >
                      <option value="">Chưa phân công</option>
                      {assignees.map((user) => (
                        <option key={user.id} value={user.id}>
                          {user.fullName}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Trạng thái">
                    <select
                      className={inputClass}
                      value={adminForm.status}
                      onChange={(event) => setAdminForm({ ...adminForm, status: event.target.value })}
                    >
                      {statuses.map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Chi phí dự kiến">
                    <input
                      className={inputClass}
                      min="0"
                      type="number"
                      value={adminForm.estimatedCost}
                      onChange={(event) => setAdminForm({ ...adminForm, estimatedCost: event.target.value })}
                    />
                  </Field>
                  <Field label="Chi phí thực tế">
                    <input
                      className={inputClass}
                      min="0"
                      type="number"
                      value={adminForm.actualCost}
                      onChange={(event) => setAdminForm({ ...adminForm, actualCost: event.target.value })}
                    />
                  </Field>
                </div>
                <Field label="Kết quả xử lý">
                  <textarea
                    className={inputClass}
                    value={adminForm.resolutionNote}
                    onChange={(event) => setAdminForm({ ...adminForm, resolutionNote: event.target.value })}
                  />
                </Field>
                <Field label="Ghi chú lịch sử">
                  <textarea
                    className={inputClass}
                    value={adminForm.note}
                    onChange={(event) => setAdminForm({ ...adminForm, note: event.target.value })}
                  />
                </Field>
                <button type="button" className="primary-button w-full" onClick={update}>
                  Cập nhật xử lý
                </button>
                <div className="border-t pt-4">
                  <p className="font-semibold">Lịch sử bảo trì phòng {detail.roomCode}</p>
                  {history.length ? (
                    history.map((item) => (
                      <p className="mt-2 text-xs text-slate-600" key={item.id}>
                        {formatDate(item.changedAt)} · {item.oldStatus ? labelOf(statuses, item.oldStatus) : 'Tạo mới'}{' '}
                        → {labelOf(statuses, item.newStatus)}
                        {item.note ? ` — ${item.note}` : ''}
                      </p>
                    ))
                  ) : (
                    <p className="mt-2 text-xs text-slate-400">Chưa có lịch sử.</p>
                  )}
                </div>
              </>
            )}
          </div>
        </Modal>
      )}
    </section>
  );
}
