import { type FormEvent, useEffect, useState } from 'react';
import { billingApi, contractApi, getApiMessage, paymentApi, roomApi, utilityApi } from '../lib/api';
import { currentUser } from '../lib/auth';
import type { Contract, ContractOccupant, InvoiceFull, PaymentQr, Room, ServiceItem } from '../types/api';
import { Badge, Field, inputClass, Modal, Pagination } from '../components/Ui';

const today = new Date().toISOString().slice(0, 10);
const period = today.slice(0, 7);
const money = (value: number) => `${Number(value).toLocaleString('vi-VN')} đ`;
/** Real invoice status, so drafts are not shown as "Chưa thanh toán". */
const statusOf = (invoice: InvoiceFull) => (Number(invoice.outstandingAmount) <= 0 ? 'PAID' : invoice.status);

export function BillingPage() {
  const admin = currentUser()?.role === 'ADMIN';
  const [data, setData] = useState<any>();
  const [rooms, setRooms] = useState<Room[]>([]);
  const [contracts, setContracts] = useState<Contract[]>([]);
  const [occupants, setOccupants] = useState<ContractOccupant[]>([]);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [qr, setQr] = useState<PaymentQr>();
  const [detail, setDetail] = useState<InvoiceFull>();
  const [confirmingPayment, setConfirmingPayment] = useState<InvoiceFull>();
  const [manualTransactionCode, setManualTransactionCode] = useState('');
  const [savingManualPayment, setSavingManualPayment] = useState(false);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState<any>({
    roomId: '',
    contractId: 0,
    payerUserId: '',
    billingPeriod: period,
    issueDate: today,
    dueDate: today,
    electricityUsage: '',
    electricityAmount: '',
    waterUsage: '',
    waterAmount: '',
    electricityFeeCode: '',
    waterFeeCode: '',
    note: '',
  });
  const [lines, setLines] = useState<any[]>([]);

  const load = () => {
    (admin ? billingApi.adminInvoices({}) : billingApi.tenantInvoices())
      .then(setData)
      .catch((response) => setError(getApiMessage(response)));
  };

  useEffect(() => {
    void load();
  }, []);
  useEffect(() => {
    if (!admin) return;
    roomApi.search({}).then((response) => setRooms(response.content));
    contractApi.adminSearch({ status: 'ACTIVE' }).then((response) => setContracts(response.content));
    utilityApi
      .services({ active: true })
      .then((response) => setServices(response.content))
      .catch(() => undefined);
  }, [admin]);

  useEffect(() => {
    if (admin || !qr) return;
    let disposed = false;
    const refreshPayment = () =>
      billingApi
        .tenantInvoice(qr.invoiceId)
        .then((invoice) => {
          if (disposed || (Number(invoice.outstandingAmount) > 0 && invoice.status !== 'PAID')) return;
          setQr(undefined);
          setDetail(invoice);
          billingApi
            .tenantInvoices()
            .then(setData)
            .catch(() => undefined);
        })
        .catch(() => undefined);
    void refreshPayment();
    const timer = window.setInterval(refreshPayment, 5000);
    return () => {
      disposed = true;
      window.clearInterval(timer);
    };
  }, [admin, qr?.invoiceId]);

  const chooseRoom = (roomId: string) => {
    const contract = contracts.find((item) => item.roomId === Number(roomId));
    setForm({ ...form, roomId, contractId: contract?.id || 0, payerUserId: '' });
    if (contract)
      contractApi
        .occupants(contract.id)
        .then(setOccupants)
        .catch((response) => setError(getApiMessage(response)));
  };
  const save = (event: FormEvent) => {
    event.preventDefault();
    const { electricityAmount: _electricityAmount, waterAmount: _waterAmount, ...rest } = form;
    // Only usage is sent; the server computes utility amounts itself.
    billingApi
      .create({
        ...rest,
        electricityUsage: form.electricityUsage === '' ? undefined : Number(form.electricityUsage),
        waterUsage: form.waterUsage === '' ? undefined : Number(form.waterUsage),
        contractId: Number(form.contractId),
        payerUserId: form.payerUserId ? Number(form.payerUserId) : undefined,
        services: lines,
      })
      .then(() => {
        setOpen(false);
        void load();
      })
      .catch((response) => setError(getApiMessage(response)));
  };
  const confirmManualPayment = (event: FormEvent) => {
    event.preventDefault();
    if (!confirmingPayment) return;
    setSavingManualPayment(true);
    const recordPayment = (invoice: InvoiceFull) =>
      paymentApi.manual(invoice.id, {
        transactionCode: manualTransactionCode,
        amount: invoice.outstandingAmount,
        paymentMethod: 'BANK_TRANSFER',
      });
    const request =
      confirmingPayment.status === 'DRAFT'
        ? billingApi.issue(confirmingPayment.id).then(recordPayment)
        : recordPayment(confirmingPayment);
    request
      .then(() => {
        setConfirmingPayment(undefined);
        setDetail(undefined);
        void load();
      })
      .catch((response) => setError(getApiMessage(response)))
      .finally(() => setSavingManualPayment(false));
  };
  const totalEstimate = Number(
    (contracts.find((contract) => contract.id === Number(form.contractId))?.monthlyRent || 0) +
      Number(form.electricityAmount || 0) +
      Number(form.waterAmount || 0) +
      lines.reduce(
        (sum, line) => sum + Number(services.find((service) => service.id === line.serviceId)?.defaultUnitPrice || 0),
        0,
      ),
  );
  const invoices = (data?.content || []) as InvoiceFull[];
  const unpaidInvoices = invoices.filter((invoice) => Number(invoice.outstandingAmount) > 0);
  const paidInvoices = invoices.filter((invoice) => Number(invoice.outstandingAmount) <= 0);
  const draftCount = invoices.filter((invoice) => invoice.status === 'DRAFT').length;
  const issue = (id: number) =>
    billingApi
      .issue(id)
      .then(() => load())
      .catch((response) => setError(getApiMessage(response)));
  const issueAllDrafts = () => {
    if (!window.confirm('Phát hành tất cả hoá đơn nháp? Người thuê sẽ nhận được thông báo thanh toán.')) return;
    setError('');
    billingApi
      .issueDrafts()
      .then(() => load())
      .catch((response) => setError(getApiMessage(response)));
  };

  return (
    <section className="space-y-6">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-sm font-semibold text-teal-600">HÓA ĐƠN & CÔNG NỢ</p>
          <h1 className="mt-1 text-3xl font-bold">Quản lý hóa đơn</h1>
        </div>
        {admin && (
          <div className="flex flex-wrap gap-2">
            {draftCount > 0 && (
              <button
                onClick={issueAllDrafts}
                className="rounded-xl border border-teal-600 px-4 py-2.5 font-semibold text-teal-700 hover:bg-teal-50"
              >
                Phát hành {draftCount} hoá đơn nháp
              </button>
            )}
            <button onClick={() => setOpen(true)} className="rounded-xl bg-teal-600 px-4 py-2.5 font-semibold text-white">
              + Tạo hóa đơn
            </button>
          </div>
        )}
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-x-auto rounded-2xl border bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50">
            <tr>
              <th className="px-5 py-4">Mã</th>
              <th>Phòng</th>
              <th>Kỳ</th>
              <th>Tổng</th>
              <th>Còn nợ</th>
              <th>Trạng thái</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {unpaidInvoices.map((invoice) => (
              <tr className="border-t" key={invoice.id}>
                <td className="px-5 py-4 font-semibold">{invoice.invoiceCode}</td>
                <td>{invoice.roomCode || 'Chưa có mã phòng'}</td>
                <td>{invoice.billingPeriod}</td>
                <td>{money(invoice.totalAmount)}</td>
                <td>{money(invoice.outstandingAmount)}</td>
                <td>
                  <Badge value={statusOf(invoice)} />
                </td>
                <td>
                  <button
                    className="text-teal-700"
                    onClick={() =>
                      (admin ? billingApi.adminInvoice(invoice.id) : billingApi.tenantInvoice(invoice.id))
                        .then(setDetail)
                        .catch((response) => setError(getApiMessage(response)))
                    }
                  >
                    Chi tiết
                  </button>
                  {admin && invoice.status === 'DRAFT' && (
                    <button className="ml-3 font-semibold text-blue-700" onClick={() => void issue(invoice.id)}>
                      Phát hành
                    </button>
                  )}
                  {admin && (invoice.status === 'DRAFT' || invoice.status === 'CANCELLED') && (
                    <button
                      className="ml-3 text-red-700"
                      onClick={() =>
                        billingApi
                          .delete(invoice.id)
                          .then(load)
                          .catch((response) => setError(getApiMessage(response)))
                      }
                    >
                      Xóa
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!unpaidInvoices.length && <p className="p-10 text-center text-slate-400">Không có hóa đơn chưa thanh toán.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={() => undefined} />
          </div>
        )}
      </div>
      <div className="rounded-2xl border bg-white p-5">
        <h2 className="font-bold">Hóa đơn đã thanh toán</h2>
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          {paidInvoices.map((invoice) => (
            <button
              key={invoice.id}
              className="rounded-xl bg-slate-50 p-4 text-left text-sm hover:bg-teal-50"
              onClick={() =>
                (admin ? billingApi.adminInvoice(invoice.id) : billingApi.tenantInvoice(invoice.id))
                  .then(setDetail)
                  .catch((response) => setError(getApiMessage(response)))
              }
            >
              <b>{invoice.roomCode || 'Chưa có mã phòng'}</b>
              <p>
                Mã {invoice.invoiceCode} · {money(invoice.totalAmount)}
              </p>
              <span className="mt-2 inline-block">
                <Badge value="PAID" />
              </span>
            </button>
          ))}
          {!paidInvoices.length && <p className="text-sm text-slate-400">Chưa có hóa đơn đã thanh toán.</p>}
        </div>
      </div>

      {open && (
        <Modal title="Tạo hóa đơn" onClose={() => setOpen(false)}>
          <form onSubmit={save} className="space-y-4">
            <Field label="Phòng">
              <select
                required
                className={inputClass}
                value={form.roomId}
                onChange={(event) => chooseRoom(event.target.value)}
              >
                <option value="">Chọn phòng</option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.roomCode}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Người nhận hóa đơn">
              <select
                required
                className={inputClass}
                value={form.payerUserId}
                onChange={(event) => setForm({ ...form, payerUserId: event.target.value })}
              >
                <option value="">Chọn người trong phòng</option>
                {occupants.map((occupant) => (
                  <option key={occupant.id} value={occupant.userId || ''}>
                    {occupant.fullName}
                    {occupant.primaryTenant ? ' (chính)' : ''}
                  </option>
                ))}
              </select>
            </Field>
            <p className="text-xs text-slate-500">
              Kỳ hóa đơn và ngày lập được hệ thống tự động lấy theo tháng hiện tại.
            </p>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Số điện (kWh)">
                <input
                  inputMode="decimal"
                  className={inputClass}
                  value={form.electricityUsage}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      electricityUsage: event.target.value,
                      electricityAmount: event.target.value ? Number(event.target.value) * 3500 : 0,
                    })
                  }
                />
              </Field>
              <Field label="Tiền điện (3.500đ/số)">
                <input
                  readOnly
                  className={`${inputClass} bg-slate-100`}
                  value={form.electricityAmount ? Number(form.electricityAmount).toLocaleString('vi-VN') : '0'}
                />
              </Field>
              <Field label="Số nước (m³)">
                <input
                  inputMode="decimal"
                  className={inputClass}
                  value={form.waterUsage}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      waterUsage: event.target.value,
                      waterAmount: event.target.value ? Number(event.target.value) * 30000 : 0,
                    })
                  }
                />
              </Field>
              <Field label="Tiền nước (30.000đ/khối)">
                <input
                  readOnly
                  className={`${inputClass} bg-slate-100`}
                  value={form.waterAmount ? Number(form.waterAmount).toLocaleString('vi-VN') : '0'}
                />
              </Field>
            </div>
            <div className="rounded-xl bg-slate-50 p-3">
              <b>Dịch vụ theo phòng</b>
              <p className="text-xs text-slate-500">
                Chọn các dịch vụ áp dụng (nóng lạnh, vệ sinh, Internet...). Đơn giá lấy từ danh mục dịch vụ.
              </p>
              {services
                .filter((service) => service.active)
                .map((service) => (
                  <label key={service.id} className="mt-2 flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      checked={lines.some((line) => line.serviceId === service.id)}
                      onChange={(event) =>
                        setLines(
                          event.target.checked
                            ? [...lines, { serviceId: service.id, quantity: 1 }]
                            : lines.filter((line) => line.serviceId !== service.id),
                        )
                      }
                    />
                    {service.serviceName} — {money(service.defaultUnitPrice)}/người
                  </label>
                ))}
            </div>
            <div className="rounded-xl border border-teal-100 bg-teal-50 p-3">
              <b>Tổng tiền tạm tính</b>
              <p className="mt-2 text-lg font-bold text-teal-800">{money(totalEstimate)}</p>
              <p className="text-xs text-teal-700">
                Bao gồm tiền phòng, điện, nước và các dịch vụ đã chọn. Số tiền chính thức do máy chủ tính.
              </p>
            </div>
            <button
              disabled={!form.contractId || !form.payerUserId}
              className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
            >
              Tạo DRAFT
            </button>
          </form>
        </Modal>
      )}
      {detail && (
        <Modal title={`Chi tiết hóa đơn ${detail.invoiceCode}`} onClose={() => setDetail(undefined)}>
          <div className="space-y-3 text-sm">
            <p>
              <b>Phòng:</b> {detail.roomCode || 'Chưa có mã phòng'}
            </p>
            <p>
              <b>Kỳ:</b> {detail.billingPeriod}
            </p>
            <p>
              <b>Trạng thái:</b> <Badge value={statusOf(detail)} />
            </p>
            <p>
              <b>Tiền phòng:</b> {money(detail.rentAmount)}
            </p>
            <p className="font-semibold">
              Tiền điện:{' '}
              {money(
                detail.items
                  ?.filter((item) => item.itemType === 'ELECTRICITY')
                  .reduce((sum, item) => sum + Number(item.amount), 0) || 0,
              )}
            </p>
            {detail.items
              ?.filter((item) => item.itemType === 'ELECTRICITY')
              .map((item) => (
                <p key={item.id} className="pl-3 text-slate-600">
                  - Số điện: {Number(item.quantity).toLocaleString('vi-VN')} {item.unitName}
                </p>
              ))}
            <p className="font-semibold">
              Tiền nước:{' '}
              {money(
                detail.items
                  ?.filter((item) => item.itemType === 'WATER')
                  .reduce((sum, item) => sum + Number(item.amount), 0) || 0,
              )}
            </p>
            {detail.items
              ?.filter((item) => item.itemType === 'WATER')
              .map((item) => (
                <p key={item.id} className="pl-3 text-slate-600">
                  - Khối nước: {Number(item.quantity).toLocaleString('vi-VN')} {item.unitName}
                </p>
              ))}
            {detail.items
              ?.filter((item) => item.itemType === 'SERVICE')
              .map((item) => (
                <p key={item.id} className="text-slate-600">
                  - {item.description}: {money(item.amount)}
                </p>
              ))}
            <p className="border-t pt-3 text-lg font-bold">Tổng: {money(detail.totalAmount)}</p>
            <p>
              Còn nợ: <b>{money(detail.outstandingAmount)}</b>
            </p>
            {admin && detail.outstandingAmount > 0 && detail.status !== 'CANCELLED' && (
              <button
                className="mt-4 w-full rounded-xl bg-teal-600 py-3 font-semibold text-white"
                onClick={() => {
                  setManualTransactionCode(`MANUAL-${detail.id}-${Date.now()}`);
                  setConfirmingPayment(detail);
                }}
              >
                Xác nhận đã thanh toán
              </button>
            )}
            {!admin && detail.outstandingAmount > 0 && (
              <button
                className="mt-4 w-full rounded-xl bg-pink-600 py-3 font-semibold text-white"
                onClick={() =>
                  paymentApi
                    .qr(detail.id)
                    .then(setQr)
                    .catch((response) => setError(getApiMessage(response)))
                }
              >
                Thanh toán qua VietQR
              </button>
            )}
          </div>
        </Modal>
      )}
      {confirmingPayment && (
        <Modal
          title="Xác nhận thanh toán thủ công"
          onClose={() => !savingManualPayment && setConfirmingPayment(undefined)}
        >
          <form onSubmit={confirmManualPayment} className="space-y-4">
            <p className="text-sm text-slate-600">
              Xác nhận bạn đã kiểm tra và nhận đủ tiền cho hóa đơn <b>{confirmingPayment.invoiceCode}</b>. Thao tác này
              sẽ chuyển hóa đơn sang <b>Đã thanh toán</b>.
            </p>
            <div className="rounded-xl bg-teal-50 p-3 text-sm">
              <p>Số tiền xác nhận</p>
              <p className="mt-1 text-lg font-bold text-teal-800">{money(confirmingPayment.outstandingAmount)}</p>
            </div>
            <Field label="Mã giao dịch / mã xác nhận">
              <input
                required
                maxLength={100}
                className={inputClass}
                value={manualTransactionCode}
                onChange={(event) => setManualTransactionCode(event.target.value)}
              />
              <span className="mt-1 block text-xs text-slate-500">
                Có thể thay bằng mã giao dịch trên sao kê. Mã này phải là duy nhất.
              </span>
            </Field>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                className="rounded-xl border px-4 py-2.5 text-sm"
                disabled={savingManualPayment}
                onClick={() => setConfirmingPayment(undefined)}
              >
                Hủy
              </button>
              <button
                disabled={savingManualPayment}
                className="rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
              >
                {savingManualPayment ? 'Đang xác nhận...' : 'Xác nhận đã thanh toán'}
              </button>
            </div>
          </form>
        </Modal>
      )}
      {qr && (
        <Modal title="Thanh toán hóa đơn qua VietQR" onClose={() => setQr(undefined)}>
          <p className="mb-3 text-sm text-slate-600">Quét mã VietQR bên dưới để thanh toán.</p>
          <img src={qr.qrUrl} alt="QR thanh toán" className="mx-auto h-72 w-72 object-contain" />
          <p className="mt-3 text-sm">
            Số tiền: {money(qr.amount)}
            <br />
            Nội dung: {qr.transferContent}
          </p>
          <p className="mt-3 text-xs text-slate-500">
            Hệ thống kiểm tra thanh toán mỗi 5 giây và tự đóng mã QR sau khi giao dịch được backend xác nhận.
          </p>
        </Modal>
      )}
    </section>
  );
}
