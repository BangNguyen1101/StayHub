import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RoomDetails } from '../components/RoomDetails';
import { roomApi } from '../lib/api';

vi.mock('../lib/api', () => ({
  roomApi: { occupants: vi.fn() },
  billingApi: {
    adminInvoices: vi.fn().mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    }),
  },
  contractApi: { adminSearch: vi.fn(), occupants: vi.fn() },
  tenantManagementApi: { search: vi.fn() },
  getApiMessage: () => 'Không tải được người ở',
}));

describe('RoomDetails', () => {
  it('shows every current occupant assigned to the room', async () => {
    vi.mocked(roomApi.occupants).mockResolvedValue([
      {
        occupantId: 1,
        userId: 7,
        fullName: 'Nguyễn Văn A',
        contractId: 11,
        contractCode: 'HD-11',
        primaryTenant: true,
        moveInDate: '2026-10-01',
      },
      {
        occupantId: 2,
        userId: 8,
        fullName: 'Trần Văn B',
        contractId: 12,
        contractCode: 'HD-12',
        primaryTenant: true,
        moveInDate: '2026-10-02',
      },
      {
        occupantId: 3,
        fullName: 'Lê Văn C',
        contractId: 11,
        contractCode: 'HD-11',
        primaryTenant: false,
        moveInDate: '2026-10-03',
      },
    ]);
    render(
      <RoomDetails
        room={{
          id: 3,
          roomCode: 'P03',
          floorNumber: 1,
          areaM2: 20,
          baseRent: 3000000,
          maxOccupants: 3,
          allowsSharing: true,
          status: 'FULL',
        }}
      />,
    );

    expect(await screen.findByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getByText('Trần Văn B')).toBeInTheDocument();
    expect(screen.getByText('Lê Văn C')).toBeInTheDocument();
    expect(screen.getByText('Người đang ở (3)')).toBeInTheDocument();
  });
});
