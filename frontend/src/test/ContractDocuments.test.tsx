import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ContractDocumentsPanel, RenewContractDialog, addMonths } from '../components/ContractDocuments';
import { contractDocumentApi } from '../lib/api';

vi.mock('../lib/api', () => ({
  contractDocumentApi: { list: vi.fn(), requestRenewal: vi.fn(), sign: vi.fn(), issue: vi.fn() },
  getApiMessage: () => 'Lỗi',
}));
vi.mock('../lib/auth', () => ({ currentUser: () => ({ userId: 2, role: 'TENANT' }) }));

const awaiting = {
  id: 7,
  contractId: 10,
  contractCode: 'HD03',
  documentType: 'CONTRACT' as const,
  status: 'AWAITING_SIGNATURE' as const,
  contentHash: 'a'.repeat(64),
  signerUserId: 2,
  signerName: 'Nguyễn Lương Bằng',
  createdAt: '2026-11-05T05:00:00',
};

describe('addMonths', () => {
  it('matches Java plusMonths, clamping to the end of shorter months', () => {
    expect(addMonths('2027-01-31', 1)).toBe('2027-02-28');
    expect(addMonths('2027-11-04', 6)).toBe('2028-05-04');
    expect(addMonths('2027-08-31', 6)).toBe('2028-02-29');
  });
});

describe('ContractDocumentsPanel', () => {
  it('lets the signer open the signing form for a document awaiting signature', async () => {
    vi.mocked(contractDocumentApi.list).mockResolvedValue([awaiting]);

    render(<ContractDocumentsPanel contractId={10} contractStatus="PENDING" admin={false} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Ký ngay' }));
    expect(screen.getByLabelText('Khung ký tên')).toBeInTheDocument();
    // Signing stays disabled until the tenant has drawn a signature and agreed.
    expect(screen.getByRole('button', { name: 'Xác nhận ký' })).toBeDisabled();
  });

  it('shows who still has to sign when the viewer is the admin', async () => {
    vi.mocked(contractDocumentApi.list).mockResolvedValue([awaiting]);

    render(<ContractDocumentsPanel contractId={10} contractStatus="PENDING" admin />);

    expect(await screen.findByText('Đang chờ Nguyễn Lương Bằng ký.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ký ngay' })).not.toBeInTheDocument();
  });
});

describe('RenewContractDialog', () => {
  it('creates a renewal appendix for the chosen duration, then asks for the signature', async () => {
    vi.mocked(contractDocumentApi.requestRenewal).mockResolvedValue({
      ...awaiting,
      id: 8,
      documentType: 'RENEWAL',
      previousEndDate: '2026-11-20',
      newEndDate: '2027-11-20',
      renewalMonths: 12,
    });

    render(
      <RenewContractDialog
        contract={{
          id: 10,
          contractCode: 'HD03',
          tenantUserId: 2,
          tenantName: 'Nguyễn Lương Bằng',
          roomId: 5,
          roomCode: 'A102',
          startDate: '2025-11-21',
          endDate: '2026-11-20',
          monthlyRent: 2400000,
          status: 'EXPIRING',
        }}
        onClose={() => {}}
        onRenewed={() => {}}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: '12 tháng' }));
    expect(screen.getByText('20/11/2027')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục ký phụ lục gia hạn' }));

    await waitFor(() => expect(contractDocumentApi.requestRenewal).toHaveBeenCalledWith(10, 12));
    expect(await screen.findByLabelText('Khung ký tên')).toBeInTheDocument();
  });
});
