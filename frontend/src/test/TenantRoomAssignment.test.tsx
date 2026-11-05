import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TenantRoomAssignment } from '../components/TenantRoomAssignment';
import { contractApi } from '../lib/api';

vi.mock('../lib/api', () => ({
  contractApi: { adminSearch: vi.fn(), addOccupant: vi.fn() },
  getApiMessage: () => 'Không gán được người thuê',
}));

describe('TenantRoomAssignment', () => {
  it('adds the tenant as a co-occupant of an existing active contract', async () => {
    vi.mocked(contractApi.adminSearch).mockResolvedValue({
      content: [
        {
          id: 10,
          contractCode: 'HD012',
          tenantUserId: 3,
          tenantName: 'Người thuê chính',
          roomId: 12,
          roomCode: 'P012',
          startDate: '2026-11-01',
          endDate: '2027-11-01',
          monthlyRent: 2500000,
          status: 'ACTIVE',
        },
      ],
      page: 0,
      size: 200,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    vi.mocked(contractApi.addOccupant).mockResolvedValue({});

    render(
      <TenantRoomAssignment
        tenant={{
          userId: 7,
          username: 'tenant',
          fullName: 'Người thuê',
          email: 'tenant@example.com',
          phoneNumber: '0900000000',
          active: true,
        }}
      />,
    );

    expect(contractApi.adminSearch).toHaveBeenCalledWith(expect.objectContaining({ status: 'ACTIVE' }));
    await screen.findByRole('option', { name: /P012/ });
    fireEvent.change(screen.getByLabelText('Hợp đồng và phòng'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Ngày bắt đầu ở'), { target: { value: '2026-11-10' } });
    fireEvent.click(screen.getByRole('button', { name: 'Gán vào phòng' }));

    await waitFor(() =>
      expect(contractApi.addOccupant).toHaveBeenCalledWith(10, {
        userId: 7,
        fullName: 'Người thuê',
        phoneNumber: '0900000000',
        primaryTenant: false,
        moveInDate: '2026-11-10',
      }),
    );
    expect(await screen.findByRole('status')).toHaveTextContent('Đã gán Người thuê vào phòng P012');
  });
});
