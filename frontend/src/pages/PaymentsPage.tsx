import { useEffect, useState } from 'react';
import { getApiMessage, paymentApi } from '../lib/api';
import { currentUser } from '../lib/auth';
import type { Payment } from '../types/api';
import { Field, Modal, Pagination, inputClass } from '../components/Ui';
export function PaymentsPage() {
  const admin = currentUser()?.role === 'ADMIN';
  const [data, setData] = useState<any>();
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [invoiceId, setInvoiceId] = useState(0);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ transactionCode: '', amount: 0, paymentMethod: 'BANK_TRANSFER' });
  const load = () => {
    (admin ? paymentApi.adminSearch() : paymentApi.tenantSearch())
      .then(setData)
      .catch((e) => setError(getApiMessage(e)));
  };
  useEffect(() => {
    void load();
  }, [page]);
  const save = (e: React.FormEvent) => {
    e.preventDefault();
    paymentApi
      .manual(invoiceId, form)
      .then(() => {
        setOpen(false);
        void load();
      })
      .catch((e) => setError(getApiMessage(e)));
  };
  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-600">THANH TOÁN</p>
          <h1 className="mt-1 text-3xl font-bold">Lịch sử thanh toán</h1>
        </div>
        {admin && (
          <button
            onClick={() => setOpen(true)}
            className="rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white"
          >
            + Ghi nhận thủ công
          </button>
        )}
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-5 py-4">Mã giao dịch</th>
              <th>Hóa đơn</th>
              <th>Số tiền</th>
              <th>Phương thức</th>
              <th>Trạng thái</th>
              <th>Thời điểm xác nhận</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map((p: Payment) => (
              <tr className="border-t" key={p.id}>
                <td className="px-5 py-4 font-semibold">{p.transactionCode}</td>
                <td>{p.invoiceId}</td>
                <td>
                  {Number(p.amount).toLocaleString('vi-VN')} {p.currencyCode}
                </td>
                <td>{p.paymentMethod}</td>
                <td>{p.status}</td>
                <td>{p.confirmedAt || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có giao dịch.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )}
      </div>
      {open && (
        <Modal title="Ghi nhận thanh toán thủ công" onClose={() => setOpen(false)}>
          <form onSubmit={save} className="space-y-4">
            <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-800">
              Chỉ ADMIN được ghi nhận. Không nhập số tiền vượt công nợ; Backend sẽ kiểm tra giao dịch trùng và số dư.
            </p>
            <Field label="ID hóa đơn">
              <input
                required
                type="number"
                min="1"
                className={inputClass}
                value={invoiceId}
                onChange={(e) => setInvoiceId(Number(e.target.value))}
              />
            </Field>
            <Field label="Mã giao dịch">
              <input
                required
                className={inputClass}
                value={form.transactionCode}
                onChange={(e) => setForm({ ...form, transactionCode: e.target.value })}
              />
            </Field>
            <Field label="Số tiền">
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                className={inputClass}
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: Number(e.target.value) })}
              />
            </Field>
            <Field label="Phương thức">
              <input
                required
                className={inputClass}
                value={form.paymentMethod}
                onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
              />
            </Field>
            <button className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white">Ghi nhận</button>
          </form>
        </Modal>
      )}
    </section>
  );
}
