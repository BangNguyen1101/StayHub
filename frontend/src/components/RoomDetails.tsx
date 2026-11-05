import { useEffect, useState } from 'react';
import { billingApi, contractApi, getApiMessage, roomApi, tenantManagementApi } from '../lib/api';
import type { InvoiceFull, Room, RoomOccupant, TenantSummary } from '../types/api';
import { Badge, Field, Modal, inputClass } from './Ui';

export function RoomDetails({ room }: { room: Room }) {
  const [occupants, setOccupants] = useState<RoomOccupant[]>([]);
  const [invoices, setInvoices] = useState<InvoiceFull[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<RoomOccupant>();
  const [availableTenants, setAvailableTenants] = useState<TenantSummary[]>([]);
  const [loadingTenants, setLoadingTenants] = useState(false);
  const [selectedTenant, setSelectedTenant] = useState<TenantSummary>();
  const [removing, setRemoving] = useState(false);

  const load = () => {
    setLoading(true);
    roomApi
      .occupants(room.id)
      .then(async (occupants) => {
        setOccupants(occupants);
        const contractIds = [...new Set(occupants.map((occupant) => occupant.contractId))];
        const pages = await Promise.all(
          contractIds.map((contractId) => billingApi.adminInvoices({ contractId, page: 0, size: 10 })),
        );
        setInvoices(pages.flatMap((page) => page.content));
      })
      .catch((err) => setError(getApiMessage(err)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    void load();
  }, [room.id]);

  const openEdit = (occupant: RoomOccupant) => {
    setEditing(occupant);
    setSelectedTenant(undefined);
  };
  useEffect(() => {
    if (!editing) return;
    let cancelled = false;
    setLoadingTenants(true);
    Promise.all([
      tenantManagementApi.search({ active: true, page: 0, size: 200 }),
      contractApi.adminSearch({ page: 0, size: 200 }),
    ])
      .then(async ([tenantPage, contractPage]) => {
        const activeContracts = contractPage.content.filter((contract) =>
          ['ACTIVE', 'EXPIRING'].includes(contract.status),
        );
        const occupantLists = await Promise.all(activeContracts.map((contract) => contractApi.occupants(contract.id)));
        const today = new Date().toISOString().slice(0, 10);
        const assignedUsers = new Set(
          occupantLists
            .flat()
            .filter(
              (occupant) =>
                occupant.userId &&
                occupant.moveInDate <= today &&
                (!occupant.moveOutDate || occupant.moveOutDate >= today),
            )
            .map((occupant) => occupant.userId),
        );
        if (!cancelled) setAvailableTenants(tenantPage.content.filter((tenant) => !assignedUsers.has(tenant.userId)));
      })
      .catch((err) => {
        if (!cancelled) setError(getApiMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoadingTenants(false);
      });
    return () => {
      cancelled = true;
    };
  }, [editing?.occupantId]);
  const chooseTenant = (userId: string) => {
    const tenant = availableTenants.find((item) => item.userId === Number(userId));
    setSelectedTenant(tenant);
  };
  const save = (event: React.FormEvent) => {
    event.preventDefault();
    if (!editing) return;
    setError('');
    if (!selectedTenant) return;
    contractApi
      .updateOccupant(editing.occupantId, {
        userId: selectedTenant.userId,
        fullName: selectedTenant.fullName,
        phoneNumber: selectedTenant.phoneNumber || '',
        identityDocumentNumber: '',
        moveInDate: editing.moveInDate,
        moveOutDate: null,
      })
      .then(() => {
        setEditing(undefined);
        void load();
      })
      .catch((err) => setError(getApiMessage(err)));
  };
  const remove = () => {
    if (!editing || editing.primaryTenant) return;
    if (!window.confirm(`Xoá ${editing.fullName} khỏi phòng ${room.roomCode}?`)) return;
    setError('');
    setRemoving(true);
    contractApi
      .removeOccupant(editing.occupantId)
      .then(() => {
        setEditing(undefined);
        void load();
      })
      .catch((err) => setError(getApiMessage(err)))
      .finally(() => setRemoving(false));
  };

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4 text-sm">
        <b>
          Tên phòng:<span className="ml-2 font-normal">{room.roomName || '-'}</span>
        </b>
        <b>
          Trạng thái:
          <span className="ml-2">
            <Badge value={room.status} />
          </span>
        </b>
        <b>
          Diện tích:<span className="ml-2 font-normal">{room.areaM2} m²</span>
        </b>
        <b>
          Giá thuê:<span className="ml-2 font-normal">{Number(room.baseRent).toLocaleString('vi-VN')} đ</span>
        </b>
        <b>
          Sức chứa:
          <span className="ml-2 font-normal">
            {occupants.length}/{room.maxOccupants}
          </span>
        </b>
        <b>
          Ở ghép:<span className="ml-2 font-normal">{room.allowsSharing ? 'Có' : 'Không'}</span>
        </b>
      </div>
      <div>
        <h3 className="mb-3 font-semibold">Người đang ở ({occupants.length})</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Đang tải danh sách...</p>
        ) : error ? (
          <p role="alert" className="text-sm text-red-600">
            {error}
          </p>
        ) : occupants.length ? (
          <div className="space-y-2">
            {occupants.map((occupant) => (
              <div key={occupant.occupantId} className="rounded-xl border p-3 text-sm">
                <div className="flex justify-between gap-3">
                  <b>{occupant.fullName}</b>
                  <span className="text-slate-500">{occupant.primaryTenant ? 'Người thuê chính' : 'Người ở cùng'}</span>
                </div>
                <p className="mt-1 text-slate-500">
                  Hợp đồng: {occupant.contractCode} · Vào ở: {occupant.moveInDate}
                </p>
                <p className="text-slate-500">
                  Điện thoại: {occupant.phoneNumber || '-'} · CCCD: {occupant.identityDocumentNumber || '-'}
                </p>
                <button className="mt-2 text-sm font-semibold text-teal-700" onClick={() => openEdit(occupant)}>
                  Sửa người thuê
                </button>
              </div>
            ))}
          </div>
        ) : (
          <p className="rounded-xl bg-slate-50 p-3 text-sm text-slate-500">
            Chưa có người đang ở theo hợp đồng đã kích hoạt.
          </p>
        )}
      </div>
      <div>
        <h3 className="mb-3 font-semibold">Hóa đơn liên quan ({invoices.length})</h3>
        {loading ? (
          <p className="text-sm text-slate-500">Đang tải hóa đơn...</p>
        ) : invoices.length ? (
          <div className="space-y-2">
            {invoices.slice(0, 5).map((invoice) => (
              <div key={invoice.id} className="flex items-center justify-between rounded-xl border p-3 text-sm">
                <div>
                  <b>{invoice.invoiceCode}</b>
                  <p className="mt-1 text-xs text-slate-500">
                    Kỳ {invoice.billingPeriod} · {Number(invoice.totalAmount).toLocaleString('vi-VN')} đ
                  </p>
                </div>
                <Badge value={invoice.status} />
              </div>
            ))}
          </div>
        ) : (
          <p className="rounded-xl bg-slate-50 p-3 text-sm text-slate-500">Chưa có hóa đơn cho hợp đồng hiện tại.</p>
        )}
      </div>
      {editing && (
        <Modal title={`Sửa người thuê — ${editing.fullName}`} onClose={() => setEditing(undefined)}>
          <form onSubmit={save} className="space-y-4">
            <Field label="Chọn người thuê chưa có phòng">
              <select
                required
                disabled={loadingTenants}
                className={inputClass}
                value={selectedTenant?.userId || ''}
                onChange={(event) => chooseTenant(event.target.value)}
              >
                <option value="">{loadingTenants ? 'Đang tải danh sách...' : 'Chọn người thuê'}</option>
                {availableTenants.map((tenant) => (
                  <option key={tenant.userId} value={tenant.userId}>
                    {tenant.fullName} — {tenant.username}
                  </option>
                ))}
              </select>
              <span className="mt-1 block text-xs text-slate-500">
                Chỉ hiện tài khoản người thuê chưa được gán vào phòng đang hiệu lực.
              </span>
            </Field>
            {error && (
              <p role="alert" className="text-sm text-red-600">
                {error}
              </p>
            )}
            <button
              disabled={!selectedTenant || loadingTenants}
              className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
            >
              Lưu người thuê đã chọn
            </button>
          </form>
          <div className="mt-4 border-t pt-4">
            {editing.primaryTenant ? (
              <p className="text-xs text-slate-500">
                Không thể xoá người thuê chính. Muốn trả phòng, hãy kết thúc hợp đồng {editing.contractCode}.
              </p>
            ) : (
              <button
                type="button"
                onClick={remove}
                disabled={removing}
                className="w-full rounded-xl border border-red-200 py-2.5 font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50"
              >
                {removing ? 'Đang xoá...' : 'Xoá người này khỏi phòng'}
              </button>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
