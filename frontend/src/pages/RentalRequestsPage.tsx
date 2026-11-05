import { useEffect, useState } from 'react';
import { getApiMessage, rentalApi } from '../lib/api';
import { currentUser } from '../lib/auth';
import type { RentalRequestSummary } from '../types/api';
import { Badge, ConfirmDialog, Modal, Pagination, inputClass } from '../components/Ui';
export function RentalRequestsPage() {
  const admin = currentUser()?.role === 'ADMIN';
  const [data, setData] = useState<any>();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [detail, setDetail] = useState<any>();
  const [confirm, setConfirm] = useState<any>();
  const load = () => {
    const fn = admin
      ? rentalApi.adminSearch({ status: status || undefined })
      : rentalApi.mine({ status: status || undefined });
    fn.then(setData).catch((e) => setError(getApiMessage(e)));
  };
  useEffect(load, [page, status]);
  const action = (kind: string) => {
    const fn =
      kind === 'approve'
        ? rentalApi.approve(confirm.id)
        : kind === 'reject'
          ? rentalApi.reject(confirm.id, 'Yêu cầu không phù hợp')
          : rentalApi.requestInfo(confirm.id, 'Vui lòng bổ sung thông tin');
    fn.then(() => {
      setConfirm(null);
      load();
    }).catch((e) => setError(getApiMessage(e)));
  };
  return (
    <section className="space-y-6">
      <div>
        <p className="text-sm font-semibold text-teal-600">{admin ? 'QUẢN LÝ YÊU CẦU' : 'YÊU CẦU THUÊ CỦA TÔI'}</p>
        <h1 className="mt-1 text-3xl font-bold">Đăng ký thuê phòng</h1>
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
          {['PENDING', 'NEEDS_INFO', 'APPROVED', 'REJECTED', 'CANCELLED'].map((x) => (
            <option key={x}>{x}</option>
          ))}
        </select>
      </div>
      {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-5 py-4">Mã đơn</th>
              <th>Phòng</th>
              <th>Người liên hệ</th>
              <th>Ngày nhận</th>
              <th>Trạng thái</th>
              <th className="px-5">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {data?.content?.map((r: RentalRequestSummary) => (
              <tr className="border-t" key={r.id}>
                <td className="px-5 py-4 font-semibold">{r.requestCode}</td>
                <td>{r.roomCode}</td>
                <td>{r.contactName}</td>
                <td>{r.expectedMoveInDate}</td>
                <td>
                  <Badge value={r.status} />
                </td>
                <td className="px-5">
                  <button
                    className="mr-3 text-teal-700"
                    onClick={() => {
                      (admin ? rentalApi.adminDetail(r.id) : rentalApi.detail(r.id))
                        .then(setDetail)
                        .catch((e) => setError(getApiMessage(e)));
                    }}
                  >
                    Chi tiết
                  </button>
                  {admin && r.status === 'PENDING' && (
                    <>
                      <button
                        className="mr-3 text-emerald-700"
                        onClick={() => setConfirm({ id: r.id, kind: 'approve' })}
                      >
                        Duyệt
                      </button>
                      <button className="text-red-700" onClick={() => setConfirm({ id: r.id, kind: 'reject' })}>
                        Từ chối
                      </button>
                    </>
                  )}
                  {!admin && ['PENDING', 'NEEDS_INFO'].includes(r.status) && (
                    <button className="text-red-700" onClick={() => setConfirm({ id: r.id, kind: 'cancel' })}>
                      Hủy
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data?.content?.length && <p className="p-10 text-center text-slate-400">Chưa có yêu cầu.</p>}
        {data && (
          <div className="p-5">
            <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </div>
        )}
      </div>
      {detail && (
        <Modal title={`Chi tiết ${detail.requestCode}`} onClose={() => setDetail(null)}>
          <div className="grid gap-3 text-sm sm:grid-cols-2">
            <p>
              <b>Phòng:</b> {detail.roomCode}
            </p>
            <p>
              <b>Trạng thái:</b> <Badge value={detail.status} />
            </p>
            <p>
              <b>Họ tên:</b> {detail.contactName}
            </p>
            <p>
              <b>Số điện thoại:</b> {detail.contactPhone}
            </p>
            <p>
              <b>Số người:</b> {detail.expectedOccupantCount}
            </p>
            <p>
              <b>Ngày nhận:</b> {detail.expectedMoveInDate}
            </p>
            <p className="sm:col-span-2">
              <b>Ghi chú:</b> {detail.note || '-'}
            </p>
          </div>
        </Modal>
      )}
      {confirm && (
        <ConfirmDialog
          title={
            confirm.kind === 'approve' ? 'Duyệt yêu cầu' : confirm.kind === 'cancel' ? 'Hủy yêu cầu' : 'Xử lý yêu cầu'
          }
          message="Bạn xác nhận thực hiện thao tác này?"
          onCancel={() => setConfirm(null)}
          onConfirm={() =>
            confirm.kind === 'cancel'
              ? rentalApi
                  .cancel(confirm.id)
                  .then(() => {
                    setConfirm(null);
                    load();
                  })
                  .catch((e) => setError(getApiMessage(e)))
              : action(confirm.kind)
          }
        />
      )}
    </section>
  );
}
