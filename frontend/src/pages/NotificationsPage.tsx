import { useEffect, useState } from 'react';
import { getApiMessage, notificationApi } from '../lib/api';
import type { AppNotification } from '../types/api';
import { Field, Modal, Pagination, inputClass } from '../components/Ui';
export function NotificationsPage() {
  const admin = false;
  const [data, setData] = useState<any>();
  const [count, setCount] = useState(0);
  const [detail, setDetail] = useState<AppNotification | null>(null);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const load = () =>
    notificationApi
      .tenantList({})
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));
  useEffect(() => {
    void load();
    notificationApi
      .unreadCount()
      .then(setCount)
      .catch(() => undefined);
  }, [page]);
  return (
    <section className="space-y-6">
      <div>
        <p className="text-sm font-semibold text-teal-600">THÔNG BÁO</p>
        <h1 className="mt-1 text-3xl font-bold">
          Thông báo của tôi <span className="text-teal-600">({count})</span>
        </h1>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="space-y-3">
        {data?.content?.map((n: AppNotification) => (
          <button
            key={n.id}
            className={`block w-full rounded-2xl border p-5 text-left ${n.read ? 'border-slate-200 bg-white' : 'border-teal-200 bg-teal-50'}`}
            onClick={() =>
              notificationApi
                .detail(n.id)
                .then((x) => {
                  setDetail(x);
                  if (!x.read)
                    notificationApi.markRead(n.id).then(() => {
                      setCount(Math.max(0, count - 1));
                      void load();
                    });
                })
                .catch((e) => setError(getApiMessage(e)))
            }
          >
            <div className="flex justify-between gap-3">
              <b>{n.title}</b>
              <span className="text-xs text-slate-400">{n.createdAt}</span>
            </div>
            <p className="mt-2 line-clamp-2 text-sm text-slate-600">{n.content}</p>
          </button>
        ))}
        {!data?.content?.length && (
          <div className="rounded-2xl bg-white p-12 text-center text-slate-400">Chưa có thông báo.</div>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>
      {detail && (
        <Modal title={detail.title} onClose={() => setDetail(null)}>
          <p className="whitespace-pre-wrap text-sm leading-6 text-slate-700">{detail.content}</p>
        </Modal>
      )}
    </section>
  );
}
export function AdminNotificationsPage() {
  const [data, setData] = useState<any>();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    recipientUserId: 0,
    notificationType: 'GENERAL',
    title: '',
    content: '',
    relatedEntityType: '',
    relatedEntityId: '',
  });
  const load = () =>
    notificationApi
      .adminList()
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));
  useEffect(() => {
    void load();
  }, []);
  const save = (e: React.FormEvent) => {
    e.preventDefault();
    notificationApi
      .create({
        ...form,
        recipientUserId: Number(form.recipientUserId),
        relatedEntityId: form.relatedEntityId ? Number(form.relatedEntityId) : undefined,
      })
      .then(() => {
        setOpen(false);
        void load();
      })
      .catch((e) => setError(getApiMessage(e)));
  };
  return (
    <section className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-sm font-semibold text-teal-600">THÔNG BÁO</p>
          <h1 className="mt-1 text-3xl font-bold">Thông báo đã gửi</h1>
        </div>
        <button
          onClick={() => setOpen(true)}
          className="rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white"
        >
          + Tạo thông báo
        </button>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-hidden rounded-2xl border bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-5 py-4">Tiêu đề</th>
              <th>Người nhận</th>
              <th>Loại</th>
              <th>Thời gian</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map((n: AppNotification) => (
              <tr className="border-t" key={n.id}>
                <td className="px-5 py-4">{n.title}</td>
                <td>{n.recipientUserId}</td>
                <td>{n.notificationType}</td>
                <td>{n.createdAt}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có thông báo.</p>}
      </div>
      {open && (
        <Modal title="Tạo thông báo" onClose={() => setOpen(false)}>
          <form onSubmit={save} className="space-y-4">
            <Field label="ID người nhận">
              <input
                required
                type="number"
                min="1"
                className={inputClass}
                value={form.recipientUserId}
                onChange={(e) => setForm({ ...form, recipientUserId: Number(e.target.value) })}
              />
            </Field>
            <Field label="Loại thông báo">
              <input
                required
                className={inputClass}
                value={form.notificationType}
                onChange={(e) => setForm({ ...form, notificationType: e.target.value })}
              />
            </Field>
            <Field label="Tiêu đề">
              <input
                required
                className={inputClass}
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </Field>
            <Field label="Nội dung">
              <textarea
                required
                className={inputClass}
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
              />
            </Field>
            <button className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white">Gửi thông báo</button>
          </form>
        </Modal>
      )}
    </section>
  );
}
