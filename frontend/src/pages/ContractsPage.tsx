import { useEffect, useState, type FormEvent } from 'react';
import { contractApi, getApiMessage, roomApi, tenantManagementApi } from '../lib/api';
import { currentUser } from '../lib/auth';
import type { Contract, Room, TenantSummary } from '../types/api';
import { Badge, ConfirmDialog, Field, Modal, Pagination, inputClass } from '../components/Ui';
import { ContractDocumentsPanel, RenewContractDialog } from '../components/ContractDocuments';

const blank = {
  contractCode: '',
  tenantUserId: '',
  roomId: '',
  startDate: '',
  endDate: '',
  monthlyRent: '',
  depositAmount: '',
  billingCycleMonths: '1',
  dueDay: '1',
  notes: '',
  terms: '',
};

export function ContractsPage() {
  const admin = currentUser()?.role === 'ADMIN';
  const [data, setData] = useState<any>();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [detail, setDetail] = useState<any>();
  const [form, setForm] = useState({ ...blank });
  const [create, setCreate] = useState(false);
  const [confirm, setConfirm] = useState<any>();
  const [renewing, setRenewing] = useState<Contract>();
  const [error, setError] = useState('');
  const [optionError, setOptionError] = useState('');
  const [loadingOptions, setLoadingOptions] = useState(false);
  const [tenants, setTenants] = useState<TenantSummary[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);

  const load = () => {
    const request = admin ? contractApi.adminSearch({ status: status || undefined, page }) : contractApi.mine({ page });
    request.then(setData).catch((e) => setError(getApiMessage(e)));
  };
  useEffect(load, [page, status]);

  useEffect(() => {
    if (!create || !admin) return;
    let cancelled = false;
    setLoadingOptions(true);
    setOptionError('');
    Promise.all([
      tenantManagementApi.search({ active: true, page: 0, size: 200, sort: 'fullName,asc' }),
      roomApi.search({ page: 0, size: 200, sort: 'roomCode,asc' }),
    ])
      .then(([tenantPage, roomPage]) => {
        if (cancelled) return;
        setTenants(tenantPage.content);
        setRooms(roomPage.content.filter((room) => !['MAINTENANCE', 'INACTIVE'].includes(room.status)));
      })
      .catch((e) => {
        if (!cancelled) setOptionError(getApiMessage(e));
      })
      .finally(() => {
        if (!cancelled) setLoadingOptions(false);
      });
    return () => {
      cancelled = true;
    };
  }, [create, admin]);

  const openCreate = () => {
    setForm({ ...blank });
    setOptionError('');
    setCreate(true);
  };
  const selectRoom = (roomId: string) => {
    const selected = rooms.find((room) => room.id === Number(roomId));
    setForm((previous) => ({
      ...previous,
      roomId,
      monthlyRent: selected ? String(selected.baseRent) : previous.monthlyRent,
    }));
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    setOptionError('');
    contractApi
      .create({
        ...form,
        tenantUserId: Number(form.tenantUserId),
        roomId: Number(form.roomId),
        monthlyRent: Number(form.monthlyRent),
        depositAmount: Number(form.depositAmount),
        billingCycleMonths: Number(form.billingCycleMonths),
        dueDay: Number(form.dueDay),
      })
      .then(() => {
        setCreate(false);
        setForm({ ...blank });
        load();
      })
      .catch((e) => setOptionError(getApiMessage(e)));
  };
  const action = () => {
    const id = confirm.id;
    const request =
      confirm.kind === 'activate'
        ? contractApi.activate(id)
        : confirm.kind === 'submit'
          ? contractApi.submit(id)
          : confirm.kind === 'cancel'
            ? contractApi.cancel(id, 'Hủy theo yêu cầu quản trị')
            : confirm.kind === 'delete'
              ? contractApi.deleteOld(id)
              : contractApi.terminate(id, {
                  actualEndDate: new Date().toISOString().slice(0, 10),
                  note: 'Kết thúc trước hạn',
                });
    request
      .then(() => {
        setConfirm(null);
        load();
      })
      .catch((e) => setError(getApiMessage(e)));
  };

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-teal-600">QUẢN LÝ HỢP ĐỒNG</p>
          <h1 className="mt-1 text-3xl font-bold">Hợp đồng thuê</h1>
        </div>
        {admin && (
          <button onClick={openCreate} className="rounded-xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white">
            + Tạo hợp đồng
          </button>
        )}
      </div>
      <div className="flex gap-3">
        <select
          className={inputClass + ' max-w-xs'}
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">Tất cả trạng thái</option>
          {['DRAFT', 'PENDING', 'ACTIVE', 'EXPIRING', 'EXPIRED', 'TERMINATED', 'CANCELLED'].map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-5 py-4">Mã hợp đồng</th>
              <th>Phòng</th>
              <th>Người thuê</th>
              <th>Thời hạn</th>
              <th>Giá thuê</th>
              <th>Trạng thái</th>
              <th className="px-5">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map((contract: Contract) => (
              <tr key={contract.id} className="border-t">
                <td className="px-5 py-4 font-semibold">{contract.contractCode}</td>
                <td>{contract.roomCode}</td>
                <td>{contract.tenantName || contract.tenantUserId}</td>
                <td>
                  {contract.startDate} → {contract.endDate}
                </td>
                <td>{Number(contract.monthlyRent).toLocaleString('vi-VN')} đ</td>
                <td>
                  <Badge value={contract.status} />
                </td>
                <td className="px-5">
                  <button
                    className="mr-3 text-teal-700"
                    onClick={() =>
                      contractApi
                        .detail(contract.id, !admin)
                        .then(setDetail)
                        .catch((e) => setError(getApiMessage(e)))
                    }
                  >
                    {!admin && contract.status === 'PENDING' ? 'Xem & ký' : 'Chi tiết'}
                  </button>
                  {!admin && contract.status === 'EXPIRING' && (
                    <button
                      className="mr-3 rounded-lg bg-teal-600 px-3 py-1 font-semibold text-white"
                      onClick={() => setRenewing(contract)}
                    >
                      Gia hạn
                    </button>
                  )}
                  {admin && contract.status === 'DRAFT' && (
                    <button
                      className="mr-3 text-blue-700"
                      onClick={() => setConfirm({ id: contract.id, kind: 'submit' })}
                    >
                      Gửi duyệt
                    </button>
                  )}
                  {admin && contract.status === 'PENDING' && (
                    <button
                      className="mr-3 text-emerald-700"
                      onClick={() => setConfirm({ id: contract.id, kind: 'activate' })}
                    >
                      Kích hoạt
                    </button>
                  )}
                  {admin && ['ACTIVE', 'EXPIRING'].includes(contract.status) && (
                    <button
                      className="text-orange-700"
                      onClick={() => setConfirm({ id: contract.id, kind: 'terminate' })}
                    >
                      Kết thúc
                    </button>
                  )}
                  {admin && ['DRAFT', 'PENDING'].includes(contract.status) && (
                    <button
                      className="ml-3 text-red-700"
                      onClick={() => setConfirm({ id: contract.id, kind: 'cancel' })}
                    >
                      Xóa
                    </button>
                  )}
                  {admin && ['TERMINATED', 'EXPIRED', 'CANCELLED'].includes(contract.status) && (
                    <button
                      className="ml-3 text-red-700"
                      onClick={() => setConfirm({ id: contract.id, kind: 'delete' })}
                    >
                      Xóa hẳn
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có hợp đồng.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )}
      </div>
      {detail && (
        <Modal title={`Chi tiết ${detail.contractCode}`} onClose={() => setDetail(null)}>
          <div className="grid gap-3 text-sm sm:grid-cols-2">
            <p>
              <b>Phòng:</b> {detail.roomCode || 'Chưa có mã phòng'}
            </p>
            <p>
              <b>Người thuê:</b> {detail.tenantUserId}
            </p>
            <p>
              <b>Ngày bắt đầu:</b> {detail.startDate}
            </p>
            <p>
              <b>Ngày kết thúc:</b> {detail.endDate}
            </p>
            <p>
              <b>Tiền thuê:</b> {Number(detail.monthlyRent).toLocaleString('vi-VN')} đ
            </p>
            <p>
              <b>Tiền cọc:</b> {Number(detail.depositAmount).toLocaleString('vi-VN')} đ
            </p>
            <p className="sm:col-span-2">
              <b>Trạng thái:</b> <Badge value={detail.status} />
            </p>
          </div>
          <ContractDocumentsPanel
            contractId={detail.id}
            contractStatus={detail.status}
            admin={admin}
            onChanged={() => {
              load();
              contractApi
                .detail(detail.id, !admin)
                .then(setDetail)
                .catch((e) => setError(getApiMessage(e)));
            }}
          />
        </Modal>
      )}
      {renewing && (
        <RenewContractDialog contract={renewing} onClose={() => setRenewing(undefined)} onRenewed={load} />
      )}
      {create && (
        <Modal title="Tạo hợp đồng" onClose={() => setCreate(false)}>
          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
            <Field label="Mã hợp đồng">
              <input
                required
                maxLength={50}
                className={inputClass}
                value={form.contractCode}
                onChange={(e) => setForm({ ...form, contractCode: e.target.value })}
              />
            </Field>
            <Field label="Người thuê">
              <select
                required
                disabled={loadingOptions}
                className={inputClass}
                value={form.tenantUserId}
                onChange={(e) => setForm({ ...form, tenantUserId: e.target.value })}
              >
                <option value="">{loadingOptions ? 'Đang tải...' : 'Chọn người thuê'}</option>
                {tenants.map((tenant) => (
                  <option key={tenant.userId} value={tenant.userId}>
                    {tenant.fullName} — {tenant.username}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Phòng">
              <select
                required
                disabled={loadingOptions}
                className={inputClass}
                value={form.roomId}
                onChange={(e) => selectRoom(e.target.value)}
              >
                <option value="">{loadingOptions ? 'Đang tải...' : 'Chọn phòng'}</option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.roomCode}
                    {room.roomName ? ` — ${room.roomName}` : ''} · {Number(room.baseRent).toLocaleString('vi-VN')} đ
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Ngày bắt đầu">
              <input
                required
                type="date"
                className={inputClass}
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              />
            </Field>
            <Field label="Ngày kết thúc">
              <input
                required
                type="date"
                min={form.startDate || undefined}
                className={inputClass}
                value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
              />
            </Field>
            <Field label="Tiền thuê/tháng">
              <input
                required
                type="number"
                min={0}
                step="0.01"
                className={inputClass}
                value={form.monthlyRent}
                onChange={(e) => setForm({ ...form, monthlyRent: e.target.value })}
              />
            </Field>
            <Field label="Tiền cọc">
              <input
                required
                type="number"
                min={0}
                step="0.01"
                className={inputClass}
                value={form.depositAmount}
                onChange={(e) => setForm({ ...form, depositAmount: e.target.value })}
              />
            </Field>
            <Field label="Chu kỳ (tháng)">
              <input
                required
                type="number"
                min={1}
                step={1}
                className={inputClass}
                value={form.billingCycleMonths}
                onChange={(e) => setForm({ ...form, billingCycleMonths: e.target.value })}
              />
            </Field>
            <Field label="Ngày đến hạn">
              <input
                required
                type="number"
                min={1}
                max={28}
                step={1}
                className={inputClass}
                value={form.dueDay}
                onChange={(e) => setForm({ ...form, dueDay: e.target.value })}
              />
            </Field>
            {optionError && (
              <p role="alert" className="sm:col-span-2 text-sm text-red-600">
                {optionError}
              </p>
            )}
            <button
              disabled={loadingOptions || !tenants.length || !rooms.length}
              className="sm:col-span-2 rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
            >
              Tạo hợp đồng
            </button>
          </form>
        </Modal>
      )}
      {confirm && (
        <ConfirmDialog
          title={
            confirm.kind === 'delete'
              ? 'Xóa hẳn hợp đồng cũ'
              : confirm.kind === 'cancel'
                ? 'Xóa hợp đồng'
                : 'Xác nhận thay đổi'
          }
          message={
            confirm.kind === 'delete'
              ? 'Hợp đồng cũ sẽ bị xóa khỏi hệ thống. Chỉ có thể xóa khi chưa phát sinh hóa đơn. Bạn xác nhận?'
              : confirm.kind === 'cancel'
                ? 'Hợp đồng sẽ chuyển sang trạng thái Đã hủy và lịch sử vẫn được lưu. Bạn xác nhận?'
                : 'Bạn xác nhận thao tác với hợp đồng này?'
          }
          onCancel={() => setConfirm(null)}
          onConfirm={action}
        />
      )}
    </section>
  );
}
