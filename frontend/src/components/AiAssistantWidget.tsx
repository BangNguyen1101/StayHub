import { ArrowRight, Bot, LoaderCircle, MessageCircle, RotateCcw, Send, Sparkles, X } from 'lucide-react';
import { FormEvent, Fragment, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { assistantApi, getApiMessage } from '../lib/api';
import type { AssistantAction, AssistantTurn, Role } from '../types/api';

type ChatMessage = {
  id: number;
  sender: 'assistant' | 'user';
  text: string;
  actions?: AssistantAction[];
  suggestions?: string[];
  mode?: 'AI' | 'BASIC';
};

/** Number of earlier messages sent along so follow-up questions keep their context. */
const HISTORY_LIMIT = 10;

const adminSuggestions = [
  'Tổng quan tình hình nhà trọ',
  'Tháng này còn bao nhiêu tiền chưa thu?',
  'Doanh thu tháng này bao nhiêu?',
  'Tình trạng các phòng hiện nay?',
  'Có sự cố bảo trì nào?',
  'Phòng nào sắp hết hợp đồng?',
];
const tenantSuggestions = [
  'Tôi còn hóa đơn nào chưa trả?',
  'Tiền điện nước gần đây của tôi?',
  'Hợp đồng của tôi còn hạn đến bao giờ?',
  'Hạn thanh toán hóa đơn của tôi?',
  'Lịch sử thanh toán của tôi?',
  'Yêu cầu bảo trì của tôi đang thế nào?',
];

const greeting = (role: Role): ChatMessage => ({
  id: Date.now(),
  sender: 'assistant',
  text:
    role === 'ADMIN'
      ? 'Xin chào! Tôi có thể tra cứu tổng quan, phòng, doanh thu, công nợ, điện nước, bảo trì và hợp đồng từ dữ liệu thực tế. Bạn cứ hỏi tự nhiên, ví dụ "phòng A102 đang có ai ở?".'
      : 'Xin chào! Tôi có thể tra cứu hóa đơn, điện nước, hợp đồng, thanh toán và yêu cầu bảo trì của bạn.',
});

/** Renders **bold** segments; everything else stays plain text (no HTML injection). */
function RichText({ text }: { text: string }) {
  return (
    <>
      {text.split(/(\*\*[^*]+\*\*)/g).map((part, index) =>
        part.startsWith('**') && part.endsWith('**') && part.length > 4 ? (
          <strong key={index} className="font-semibold text-slate-900">
            {part.slice(2, -2)}
          </strong>
        ) : (
          <Fragment key={index}>{part}</Fragment>
        ),
      )}
    </>
  );
}

export function AiAssistantWidget({ role }: { role: Role }) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([greeting(role)]);
  const endRef = useRef<HTMLDivElement>(null);
  const suggestions = role === 'ADMIN' ? adminSuggestions : tenantSuggestions;
  const lastMessage = messages[messages.length - 1];

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, open]);

  const send = async (text: string) => {
    const value = text.trim();
    if (!value || loading) return;
    // The greeting is UI only; earlier real turns give the assistant its context.
    const history: AssistantTurn[] = messages
      .slice(1)
      .slice(-HISTORY_LIMIT)
      .map((message) => ({ role: message.sender, text: message.text.slice(0, 4000) }));
    setMessages((items) => [...items, { id: Date.now(), sender: 'user', text: value }]);
    setInput('');
    setLoading(true);
    try {
      const response = await assistantApi.chat(value, history);
      setMessages((items) => [
        ...items,
        {
          id: Date.now() + 1,
          sender: 'assistant',
          text: response.answer,
          actions: response.actions,
          suggestions: response.suggestions,
          mode: response.mode,
        },
      ]);
    } catch (error) {
      setMessages((items) => [...items, { id: Date.now() + 1, sender: 'assistant', text: getApiMessage(error) }]);
    } finally {
      setLoading(false);
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void send(input);
  };

  const go = (action: AssistantAction) => {
    navigate(action.path);
    // Keep the chat open on wide screens; on phones it would cover the page just opened.
    if (window.innerWidth < 640) setOpen(false);
  };

  return (
    <div className="fixed bottom-5 right-5 z-[70] sm:bottom-7 sm:right-7">
      {open && (
        <section
          aria-label="Trợ lý AI quản lý nhà trọ"
          className="mb-3 flex h-[min(620px,calc(100vh-7rem))] w-[min(390px,calc(100vw-2rem))] flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl"
        >
          <header className="flex items-center justify-between bg-gradient-to-r from-teal-600 to-emerald-600 px-4 py-3.5 text-white">
            <div className="flex items-center gap-3">
              <span className="grid h-9 w-9 place-items-center rounded-xl bg-white/15">
                <Bot size={20} />
              </span>
              <div>
                <h2 className="text-sm font-bold">Trợ lý AI nhà trọ</h2>
                <p className="text-[11px] text-teal-50">Dữ liệu trực tiếp từ hệ thống</p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => {
                  setMessages([greeting(role)]);
                  setInput('');
                }}
                aria-label="Đặt lại đoạn chat"
                title="Đặt lại đoạn chat"
                className="rounded-lg p-1.5 hover:bg-white/15"
              >
                <RotateCcw size={17} />
              </button>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Đóng trợ lý"
                className="rounded-lg p-1.5 hover:bg-white/15"
              >
                <X size={19} />
              </button>
            </div>
          </header>
          <div className="flex-1 space-y-3 overflow-y-auto bg-slate-50 p-4">
            {messages.map((message) => (
              <div key={message.id} className={`flex ${message.sender === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className="max-w-[88%] space-y-2">
                  <div
                    className={`whitespace-pre-wrap rounded-2xl px-3.5 py-2.5 text-sm leading-5 ${message.sender === 'user' ? 'rounded-br-md bg-teal-600 text-white' : 'rounded-bl-md border bg-white text-slate-700 shadow-sm'}`}
                  >
                    {message.sender === 'assistant' ? <RichText text={message.text} /> : message.text}
                  </div>
                  {message.mode && (
                    <p className="flex items-center gap-1 pl-1 text-[10px] text-slate-400">
                      {message.mode === 'AI' ? (
                        <>
                          <Sparkles size={11} /> Trả lời bởi AI từ dữ liệu hệ thống
                        </>
                      ) : (
                        'Chế độ cơ bản (chưa bật AI)'
                      )}
                    </p>
                  )}
                  {!!message.actions?.length && (
                    <div className="flex flex-wrap gap-2">
                      {message.actions.map((action) => (
                        <button
                          type="button"
                          key={action.path}
                          onClick={() => go(action)}
                          className="inline-flex items-center gap-1 rounded-xl bg-teal-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-teal-700"
                        >
                          {action.label}
                          <ArrowRight size={13} />
                        </button>
                      ))}
                    </div>
                  )}
                  {message === lastMessage && !loading && !!message.suggestions?.length && (
                    <div className="flex flex-wrap gap-1.5 pt-0.5">
                      {message.suggestions.map((item) => (
                        <button
                          type="button"
                          key={item}
                          onClick={() => void send(item)}
                          className="rounded-full border border-teal-200 bg-white px-3 py-1 text-xs text-teal-800 hover:bg-teal-50"
                        >
                          {item}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}
            {messages.length === 1 && (
              <div className="space-y-2 pt-1">
                {suggestions.map((item) => (
                  <button
                    type="button"
                    key={item}
                    onClick={() => void send(item)}
                    className="block w-full rounded-xl border border-teal-100 bg-teal-50 px-3 py-2 text-left text-xs font-medium text-teal-800 hover:bg-teal-100"
                  >
                    {item}
                  </button>
                ))}
              </div>
            )}
            {loading && (
              <div className="flex justify-start">
                <div className="flex items-center gap-2 rounded-2xl rounded-bl-md border bg-white px-3.5 py-2.5 text-xs text-slate-500">
                  <LoaderCircle size={15} className="animate-spin" />
                  Đang tra cứu dữ liệu...
                </div>
              </div>
            )}
            <div ref={endRef} />
          </div>
          <form onSubmit={submit} className="flex gap-2 border-t bg-white p-3">
            <input
              aria-label="Nhập câu hỏi"
              maxLength={1000}
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Hỏi về nhà trọ..."
              className="min-w-0 flex-1 rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
            />
            <button
              type="submit"
              disabled={!input.trim() || loading}
              aria-label="Gửi câu hỏi"
              className="grid h-10 w-10 place-items-center rounded-xl bg-teal-600 text-white disabled:cursor-not-allowed disabled:opacity-40"
            >
              <Send size={17} />
            </button>
          </form>
        </section>
      )}
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-label="Mở trợ lý AI"
        className="ml-auto grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-teal-600 to-emerald-600 text-white shadow-lg shadow-teal-600/25 transition hover:-translate-y-0.5 hover:shadow-xl"
      >
        {open ? <X size={23} /> : <MessageCircle size={25} />}
        <span className="sr-only">Trợ lý AI</span>
      </button>
    </div>
  );
}
