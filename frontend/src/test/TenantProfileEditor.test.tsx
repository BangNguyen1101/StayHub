import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TenantProfileEditor } from '../components/TenantProfileEditor';
import { tenantManagementApi } from '../lib/api';

vi.mock('../lib/api', () => ({
  tenantManagementApi: { update: vi.fn() },
  getApiMessage: () => 'Không lưu được hồ sơ',
}));

describe('TenantProfileEditor', () => {
  it('saves address and CCCD without losing existing profile fields or leading zeroes', async () => {
    const tenant = {
      userId: 7,
      username: 'tenant',
      email: 'tenant@example.com',
      fullName: 'Người thuê',
      active: true,
      identityDocumentType: 'CCCD',
      identityIssuedPlace: 'Hà Nội',
      notes: 'Ghi chú hiện có',
    };
    vi.mocked(tenantManagementApi.update).mockResolvedValue(tenant);
    const saved = vi.fn();
    render(<TenantProfileEditor tenant={tenant} onSaved={saved} />);
    fireEvent.change(screen.getByLabelText('Địa chỉ liên hệ'), { target: { value: 'Địa chỉ mới' } });
    fireEvent.change(screen.getByLabelText('Số CCCD / giấy tờ'), { target: { value: '001234567890' } });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thông tin' }));
    await waitFor(() => expect(saved).toHaveBeenCalledWith(tenant));
    expect(tenantManagementApi.update).toHaveBeenCalledWith(
      7,
      expect.objectContaining({
        contactAddress: 'Địa chỉ mới',
        identityDocumentNumber: '001234567890',
        identityIssuedDate: null,
        identityIssuedPlace: 'Hà Nội',
        notes: 'Ghi chú hiện có',
      }),
    );
  });
});
