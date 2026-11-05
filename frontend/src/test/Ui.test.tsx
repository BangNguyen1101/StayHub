import { render, screen } from '@testing-library/react';
import { Badge, Pagination } from '../components/Ui';
import { describe, expect, it } from 'vitest';

describe('shared UI', () => {
  it('renders a status badge', () => {
    render(<Badge value="PARTIALLY_PAID" />);
    expect(screen.getByText('Thanh toán một phần')).toBeInTheDocument();
  });
  it('handles pagination boundaries', () => {
    render(<Pagination page={0} totalPages={2} onChange={() => undefined} />);
    expect(screen.getByText('Trước')).toBeDisabled();
    expect(screen.getByText('Sau')).not.toBeDisabled();
  });
});
