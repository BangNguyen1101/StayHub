import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AiAssistantWidget } from '../components/AiAssistantWidget';
import { assistantApi } from '../lib/api';

vi.mock('../lib/api', () => ({
  assistantApi: { chat: vi.fn() },
  getApiMessage: () => 'Lỗi',
}));

// jsdom does not implement scrolling.
Element.prototype.scrollIntoView = vi.fn();

describe('AiAssistantWidget', () => {
  it('shows action buttons and follow-up suggestions, and keeps the conversation context', async () => {
    vi.mocked(assistantApi.chat).mockResolvedValue({
      answer: 'Bạn còn **1.250.000 đ** chưa trả.',
      intent: 'TENANT_INVOICES',
      answeredAt: '2026-11-05T14:00:00',
      actions: [{ label: 'Thanh toán ngay', path: '/tenant/invoices' }],
      suggestions: ['Hạn thanh toán hóa đơn của tôi?'],
      mode: 'AI',
    });

    render(
      <MemoryRouter initialEntries={['/tenant']}>
        <Routes>
          <Route path="/tenant" element={<AiAssistantWidget role="TENANT" />} />
          <Route path="/tenant/invoices" element={<p>Trang hoá đơn</p>} />
        </Routes>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Mở trợ lý AI' }));
    fireEvent.change(screen.getByLabelText('Nhập câu hỏi'), { target: { value: 'Tôi còn nợ bao nhiêu?' } });
    fireEvent.click(screen.getByRole('button', { name: 'Gửi câu hỏi' }));

    expect((await screen.findByText('1.250.000 đ')).tagName).toBe('STRONG');
    expect(assistantApi.chat).toHaveBeenLastCalledWith('Tôi còn nợ bao nhiêu?', []);

    // A tapped follow-up sends the previous turns along as context.
    fireEvent.click(screen.getByRole('button', { name: 'Hạn thanh toán hóa đơn của tôi?' }));
    await waitFor(() =>
      expect(assistantApi.chat).toHaveBeenLastCalledWith('Hạn thanh toán hóa đơn của tôi?', [
        { role: 'user', text: 'Tôi còn nợ bao nhiêu?' },
        { role: 'assistant', text: 'Bạn còn **1.250.000 đ** chưa trả.' },
      ]),
    );

    fireEvent.click((await screen.findAllByRole('button', { name: /Thanh toán ngay/ }))[0]);
    expect(await screen.findByText('Trang hoá đơn')).toBeInTheDocument();
  });
});
