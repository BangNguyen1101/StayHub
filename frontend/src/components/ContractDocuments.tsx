import { useEffect, useState } from 'react';
import { contractDocumentApi, getApiMessage } from '../lib/api';
import { currentUser } from '../lib/auth';
import type { Contract, ContractDocument, ContractDocumentVerify } from '../types/api';
import { Badge, Modal, inputClass } from './Ui';
import { SignaturePad } from './SignaturePad';

const formatDate = (value?: string | null) => (value ? value.slice(0, 10).split('-').reverse().join('/') : '-');
/** Backend timestamps are UTC without an offset; show them in local (Vietnam) time. */
const formatDateTime = (value?: string | null) =>
  value
    ? new Date(value.endsWith('Z') ? value : `${value}Z`).toLocaleString('vi-VN', { hour12: false })
    : '-';
const shortHash = (hash?: string | null) => (hash ? `${hash.slice(0, 12)}…${hash.slice(-8)}` : '-');

/** Same rule as Java LocalDate.plusMonths: keep the day, clamped to the target month's length. */
export function addMonths(isoDate: string, months: number) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const target = new Date(Date.UTC(year, month - 1 + months, 1));
  const lastDay = new Date(Date.UTC(target.getUTCFullYear(), target.getUTCMonth() + 1, 0)).getUTCDate();
  target.setUTCDate(Math.min(day, lastDay));
  return target.toISOString().slice(0, 10);
}

/** Error bodies of blob requests arrive as Blob, so getApiMessage cannot read them directly. */
async function blobErrorMessage(error: unknown) {
  const data = (error as { response?: { data?: unknown } })?.response?.data;
  if (data instanceof Blob) {
    try {
      return (JSON.parse(await data.text()) as { message?: string }).message || getApiMessage(error);
    } catch {
      return getApiMessage(error);
    }
  }
  return getApiMessage(error);
}

/** Opens the PDF in a new tab; the tab is opened first so mobile browsers do not block it. */
async function openPdf(document: ContractDocument, admin: boolean, onError: (message: string) => void) {
  const tab = window.open('', '_blank');
  try {
    const blob = await contractDocumentApi.pdf(document.id, admin);
    const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
    if (tab) tab.location.href = url;
    else window.location.href = url;
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (error) {
    tab?.close();
    onError(await blobErrorMessage(error));
  }
}

function documentTitle(document: ContractDocument) {
  return document.documentType === 'RENEWAL'
    ? `Phụ lục gia hạn ${document.renewalMonths} tháng (đến ${formatDate(document.newEndDate)})`
    : 'Hợp đồng thuê phòng';
}

export function ContractDocumentsPanel({
  contractId,
  contractStatus,
  admin,
  onChanged,
}: {
  contractId: number;
  contractStatus: string;
  admin: boolean;
  onChanged?: () => void;
}) {
  const [documents, setDocuments] = useState<ContractDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [signing, setSigning] = useState<ContractDocument>();
  const [verification, setVerification] = useState<Record<number, ContractDocumentVerify>>({});
  const me = currentUser()?.userId;

  const load = () => {
    setLoading(true);
    contractDocumentApi
      .list(contractId, admin)
      .then(setDocuments)
      .catch((e) => setError(getApiMessage(e)))
      .finally(() => setLoading(false));
  };
  useEffect(load, [contractId, admin]);

  const hasLease = documents.some((d) => d.documentType === 'CONTRACT' && d.status !== 'VOID');
  const issue = () => {
    setError('');
    contractDocumentApi
      .issue(contractId)
      .then(() => load())
      .catch((e) => setError(getApiMessage(e)));
  };
  const verifyStored = (document: ContractDocument) =>
    contractDocumentApi
      .verify(document.id, admin)
      .then((result) => setVerification((previous) => ({ ...previous, [document.id]: result })))
      .catch((e) => setError(getApiMessage(e)));
  const verifyFile = (document: ContractDocument, file?: File) => {
    if (!file) return;
    contractDocumentApi
      .verifyFile(document.id, file, admin)
      .then((result) => setVerification((previous) => ({ ...previous, [document.id]: result })))
      .catch((e) => setError(getApiMessage(e)));
  };

  return (
    <div className="mt-6 border-t pt-5">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="font-semibold">Hợp đồng điện tử</h3>
        {admin && contractStatus === 'PENDING' && !loading && !hasLease && (
          <button onClick={issue} className="rounded-lg bg-teal-600 px-3 py-1.5 text-sm font-semibold text-white">
            Gửi người thuê ký
          </button>
        )}
      </div>
      {error && <p className="mb-3 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {loading ? (
        <p className="text-sm text-slate-500">Đang tải tài liệu...</p>
      ) : !documents.length ? (
        <p className="rounded-xl bg-slate-50 p-3 text-sm text-slate-500">
          {contractStatus === 'DRAFT'
            ? 'Hợp đồng sẽ được gửi cho người thuê ký khi bấm "Gửi duyệt".'
            : 'Chưa có tài liệu điện tử cho hợp đồng này.'}
        </p>
      ) : (
        <div className="space-y-3">
          {documents.map((document) => {
            const result = verification[document.id];
            const canSign = !admin && document.status === 'AWAITING_SIGNATURE' && document.signerUserId === me;
            return (
              <div key={document.id} className="rounded-xl border p-4 text-sm">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <b>{documentTitle(document)}</b>
                    <p className="mt-1 text-xs text-slate-500">
                      Mã tài liệu #{document.id} · Tạo lúc {formatDateTime(document.createdAt)}
                    </p>
                  </div>
                  <Badge value={document.status} />
                </div>
                {document.status === 'SIGNED' && (
                  <div className="mt-2 space-y-0.5 text-xs text-slate-600">
                    <p>
                      Ký bởi <b>{document.signerName}</b> lúc {formatDateTime(document.signedAt)}
                      {document.signerIp ? ` · IP ${document.signerIp}` : ''}
                    </p>
                    <p className="break-all font-mono" title={document.signedPdfHash || ''}>
                      SHA-256: {shortHash(document.signedPdfHash)}
                    </p>
                  </div>
                )}
                {document.status === 'AWAITING_SIGNATURE' && !canSign && (
                  <p className="mt-2 text-xs text-amber-700">Đang chờ {document.signerName} ký.</p>
                )}
                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <button
                    className="font-semibold text-teal-700"
                    onClick={() => void openPdf(document, admin, setError)}
                  >
                    {document.status === 'SIGNED' ? 'Tải PDF đã ký' : 'Xem bản PDF'}
                  </button>
                  {canSign && (
                    <button
                      className="rounded-lg bg-teal-600 px-3 py-1.5 font-semibold text-white"
                      onClick={() => setSigning(document)}
                    >
                      Ký ngay
                    </button>
                  )}
                  {document.status === 'SIGNED' && (
                    <>
                      <button className="font-semibold text-blue-700" onClick={() => void verifyStored(document)}>
                        Kiểm tra toàn vẹn
                      </button>
                      <label className="cursor-pointer font-semibold text-slate-600">
                        Đối chiếu file PDF…
                        <input
                          type="file"
                          accept="application/pdf"
                          className="hidden"
                          onChange={(event) => {
                            verifyFile(document, event.target.files?.[0]);
                            event.target.value = '';
                          }}
                        />
                      </label>
                    </>
                  )}
                </div>
                {result && (
                  <p
                    role="status"
                    className={`mt-3 rounded-lg p-2 text-xs ${
                      result.valid ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'
                    }`}
                  >
                    {result.valid
                      ? '✔ File khớp với mã băm đã lưu khi ký — tài liệu chưa bị chỉnh sửa.'
                      : `✘ File KHÔNG khớp mã băm đã lưu (${shortHash(result.actualHash)}) — tài liệu đã bị thay đổi hoặc không phải bản gốc.`}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}
      {signing && (
        <Modal title={`Ký ${documentTitle(signing).toLowerCase()}`} onClose={() => setSigning(undefined)}>
          <SignDocumentForm
            document={signing}
            onSigned={() => {
              setSigning(undefined);
              load();
              onChanged?.();
            }}
          />
        </Modal>
      )}
    </div>
  );
}

export function SignDocumentForm({ document, onSigned }: { document: ContractDocument; onSigned: () => void }) {
  const [signature, setSignature] = useState<string | null>(null);
  const [agreed, setAgreed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!signature || !agreed) return;
    setBusy(true);
    setError('');
    contractDocumentApi
      .sign(document.id, signature)
      .then(onSigned)
      .catch((e) => setError(getApiMessage(e)))
      .finally(() => setBusy(false));
  };

  return (
    <form onSubmit={submit} className="space-y-4">
      <p className="text-sm text-slate-600">
        Vui lòng đọc kỹ bản PDF trước khi ký. Sau khi ký, file PDF được khoá lại bằng mã băm SHA-256 và không thể
        chỉnh sửa.
      </p>
      <button
        type="button"
        className="w-full rounded-xl border border-teal-200 py-2.5 font-semibold text-teal-700 hover:bg-teal-50"
        onClick={() => void openPdf(document, false, setError)}
      >
        Mở bản PDF để đọc
      </button>
      <SignaturePad onChange={setSignature} />
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" className="mt-1" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} />
        <span>Tôi đã đọc, hiểu và đồng ý với toàn bộ nội dung của tài liệu này.</span>
      </label>
      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
      <button
        disabled={!signature || !agreed || busy}
        className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
      >
        {busy ? 'Đang ký...' : 'Xác nhận ký'}
      </button>
    </form>
  );
}

const QUICK_MONTHS = [3, 6, 12];

/** One-tap renewal: choose a duration, then sign the generated appendix right away. */
export function RenewContractDialog({
  contract,
  onClose,
  onRenewed,
}: {
  contract: Contract;
  onClose: () => void;
  onRenewed: () => void;
}) {
  const [months, setMonths] = useState(6);
  const [document, setDocument] = useState<ContractDocument>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const validMonths = Number.isInteger(months) && months >= 1 && months <= 36;

  const create = () => {
    if (!validMonths) return;
    setBusy(true);
    setError('');
    contractDocumentApi
      .requestRenewal(contract.id, months)
      .then(setDocument)
      .catch((e) => setError(getApiMessage(e)))
      .finally(() => setBusy(false));
  };

  return (
    <Modal title={`Gia hạn hợp đồng ${contract.contractCode}`} onClose={onClose}>
      {done ? (
        <div className="space-y-4">
          <p role="status" className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700">
            Đã gia hạn thành công đến ngày <b>{formatDate(document?.newEndDate)}</b>. Bản PDF phụ lục đã ký được lưu
            trong mục "Chi tiết" của hợp đồng.
          </p>
          <button onClick={onClose} className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white">
            Đóng
          </button>
        </div>
      ) : document ? (
        <SignDocumentForm
          document={document}
          onSigned={() => {
            setDone(true);
            onRenewed();
          }}
        />
      ) : (
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Hợp đồng phòng <b>{contract.roomCode}</b> hết hạn ngày <b>{formatDate(contract.endDate)}</b>. Chọn thời gian
            gia hạn, giá thuê giữ nguyên {Number(contract.monthlyRent).toLocaleString('vi-VN')} đ/tháng.
          </p>
          <div className="flex flex-wrap gap-2">
            {QUICK_MONTHS.map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setMonths(value)}
                className={`rounded-xl border px-4 py-2 text-sm font-semibold ${
                  months === value ? 'border-teal-600 bg-teal-50 text-teal-700' : 'border-slate-200 text-slate-600'
                }`}
              >
                {value} tháng
              </button>
            ))}
            <label className="flex items-center gap-2 text-sm text-slate-600">
              hoặc
              <input
                type="number"
                min={1}
                max={36}
                className={inputClass + ' w-24'}
                value={Number.isNaN(months) ? '' : months}
                onChange={(e) => setMonths(Number.parseInt(e.target.value, 10))}
              />
              tháng
            </label>
          </div>
          {validMonths ? (
            <p className="rounded-xl bg-slate-50 p-3 text-sm">
              Ngày hết hạn mới: <b>{formatDate(addMonths(contract.endDate, months))}</b>
            </p>
          ) : (
            <p className="text-sm text-red-600">Số tháng gia hạn phải từ 1 đến 36.</p>
          )}
          {error && (
            <p role="alert" className="text-sm text-red-600">
              {error}
            </p>
          )}
          <button
            onClick={create}
            disabled={!validMonths || busy}
            className="w-full rounded-xl bg-teal-600 py-2.5 font-semibold text-white disabled:opacity-50"
          >
            {busy ? 'Đang tạo phụ lục...' : 'Tiếp tục ký phụ lục gia hạn'}
          </button>
        </div>
      )}
    </Modal>
  );
}
