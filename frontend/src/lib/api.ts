import axios from 'axios';
import { clearAuth, readAuth } from './auth';
import type {
  ApiError,
  AuthResponse,
  Contract,
  ContractDetail,
  ContractDocument,
  ContractDocumentVerify,
  ContractOccupant,
  Dashboard,
  DebtItem,
  Invoice,
  Notification,
  PageResponse,
  ReportBundle,
  User,
  CountRow,
  Room,
  RoomOccupant,
  RentalRequest,
  RentalRequestSummary,
  TenantDetail,
  TenantSummary,
  UtilityReading,
  ServiceItem,
  FeeConfig,
  InvoiceFull,
  Payment,
  PaymentQr,
  AppNotification,
  MaintenanceRequest,
  MaintenanceHistory,
  MaintenanceAssignee,
  MaintenanceAttachment,
  ReportBundleData,
  ReportDebt,
  ReportCount,
  RoomRevenue,
  AssistantChatResponse,
  AssistantTurn,
} from '../types/api';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});
api.interceptors.request.use((config) => {
  const auth = readAuth();
  if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`;
  return config;
});
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) clearAuth();
    return Promise.reject(error);
  },
);
export function getApiMessage(error: unknown): string {
  const data = (error as { response?: { data?: ApiError } })?.response?.data;
  return data?.message || 'Không thể kết nối máy chủ. Vui lòng thử lại.';
}

export const authApi = {
  login: (payload: { identifier: string; password: string }) =>
    api.post<AuthResponse>('/auth/login', payload).then((r) => r.data),
  register: (payload: { username: string; email: string; password: string; fullName: string; phoneNumber?: string }) =>
    api.post<AuthResponse>('/auth/register', payload).then((r) => r.data),
  me: () => api.get<User>('/auth/me').then((r) => r.data),
};
export const reportApi = {
  dashboard: (asOf?: string) =>
    api.get<Dashboard>('/admin/reports/dashboard', { params: asOf ? { asOf } : {} }).then((r) => r.data),
  revenue: (from: string, to: string) =>
    api.get<ReportBundle>('/admin/reports/revenue', { params: { from, to } }).then((r) => r.data),
  revenueByRoom: (from: string, to: string) =>
    api.get<RoomRevenue[]>('/admin/reports/revenue-by-room', { params: { from, to } }).then((r) => r.data),
  rooms: () => api.get<CountRow[]>('/admin/reports/rooms').then((r) => r.data),
};
export const tenantApi = {
  invoices: () =>
    api
      .get<PageResponse<Invoice>>('/tenant/invoices', { params: { page: 0, size: 5, sort: 'issueDate,desc' } })
      .then((r) => r.data),
  debts: () => api.get<PageResponse<DebtItem>>('/tenant/debts', { params: { page: 0, size: 5 } }).then((r) => r.data),
  contracts: () =>
    api.get<PageResponse<Contract>>('/tenant/contracts', { params: { page: 0, size: 5 } }).then((r) => r.data),
  notifications: () =>
    api.get<PageResponse<Notification>>('/tenant/notifications', { params: { page: 0, size: 5 } }).then((r) => r.data),
  profile: () => api.get('/tenant/profile').then((r) => r.data),
  updateProfile: (payload: { fullName: string; phoneNumber?: string; contactAddress?: string }) =>
    api.patch('/tenant/profile', payload).then((r) => r.data),
};
const pageParams = (params: Record<string, unknown>) => ({ params: { page: 0, size: 10, ...params } });
export const roomApi = {
  search: (params: Record<string, unknown>) =>
    api.get<PageResponse<Room>>('/admin/rooms', pageParams(params)).then((r) => r.data),
  detail: (id: number) => api.get<Room>(`/admin/rooms/${id}`).then((r) => r.data),
  occupants: (id: number) => api.get<RoomOccupant[]>(`/admin/rooms/${id}/occupants`).then((r) => r.data),
  create: (payload: Omit<Room, 'id' | 'status' | 'createdAt' | 'updatedAt'>) =>
    api.post<Room>('/admin/rooms', payload).then((r) => r.data),
  update: (id: number, payload: Omit<Room, 'id' | 'status' | 'createdAt' | 'updatedAt'>) =>
    api.put<Room>(`/admin/rooms/${id}`, payload).then((r) => r.data),
  updateStatus: (id: number, status: string) =>
    api.patch<Room>(`/admin/rooms/${id}/status`, { status }).then((r) => r.data),
  available: (page = 0, size = 10) =>
    api.get<PageResponse<Room>>('/rooms/available', { params: { page, size } }).then((r) => r.data),
};
export const rentalApi = {
  mine: (params: Record<string, unknown>) =>
    api.get<PageResponse<RentalRequestSummary>>('/rental-requests/mine', pageParams(params)).then((r) => r.data),
  detail: (id: number) => api.get<RentalRequest>(`/rental-requests/${id}`).then((r) => r.data),
  submit: (roomId: number, payload: object) =>
    api.post<RentalRequest>('/rental-requests', payload, { params: { roomId } }).then((r) => r.data),
  cancel: (id: number) => api.patch<RentalRequest>(`/rental-requests/${id}/cancel`).then((r) => r.data),
  supplement: (id: number, payload: object) =>
    api.patch<RentalRequest>(`/rental-requests/${id}/supplement`, payload).then((r) => r.data),
  adminSearch: (params: Record<string, unknown>) =>
    api.get<PageResponse<RentalRequestSummary>>('/admin/rental-requests', pageParams(params)).then((r) => r.data),
  adminDetail: (id: number) => api.get<RentalRequest>(`/admin/rental-requests/${id}`).then((r) => r.data),
  approve: (id: number) => api.patch<RentalRequest>(`/admin/rental-requests/${id}/approve`).then((r) => r.data),
  reject: (id: number, note: string) =>
    api.patch<RentalRequest>(`/admin/rental-requests/${id}/reject`, { note }).then((r) => r.data),
  requestInfo: (id: number, note: string) =>
    api.patch<RentalRequest>(`/admin/rental-requests/${id}/request-info`, { note }).then((r) => r.data),
};
export const tenantManagementApi = {
  create: (payload: { username: string; password: string; fullName: string; phoneNumber?: string }) =>
    api.post<TenantDetail>('/admin/tenants', payload).then((r) => r.data),
  search: (params: Record<string, unknown>) =>
    api.get<PageResponse<TenantSummary>>('/admin/tenants', pageParams(params)).then((r) => r.data),
  detail: (id: number) => api.get<TenantDetail>(`/admin/tenants/${id}`).then((r) => r.data),
  update: (id: number, payload: object) =>
    api.patch<TenantDetail>(`/admin/tenants/${id}/profile`, payload).then((r) => r.data),
  deactivate: (id: number) => api.delete(`/admin/tenants/${id}`),
};
export const contractApi = {
  adminSearch: (params: Record<string, unknown>) =>
    api.get<PageResponse<Contract>>('/admin/contracts', pageParams(params)).then((r) => r.data),
  mine: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<Contract>>('/tenant/contracts', pageParams(params)).then((r) => r.data),
  detail: (id: number, tenant = false) =>
    api.get<ContractDetail>(`${tenant ? '/tenant' : '/admin'}/contracts/${id}`).then((r) => r.data),
  create: (payload: object) => api.post<ContractDetail>('/admin/contracts', payload).then((r) => r.data),
  update: (id: number, payload: object) =>
    api.patch<ContractDetail>(`/admin/contracts/${id}`, payload).then((r) => r.data),
  submit: (id: number) => api.patch<ContractDetail>(`/admin/contracts/${id}/submit`).then((r) => r.data),
  activate: (id: number) => api.patch<ContractDetail>(`/admin/contracts/${id}/activate`).then((r) => r.data),
  renew: (id: number, endDate: string) =>
    api.patch<ContractDetail>(`/admin/contracts/${id}/renew`, { endDate }).then((r) => r.data),
  terminate: (id: number, payload: object) =>
    api.patch<ContractDetail>(`/admin/contracts/${id}/terminate`, payload).then((r) => r.data),
  cancel: (id: number, note?: string) =>
    api.patch<ContractDetail>(`/admin/contracts/${id}/cancel`, { note }).then((r) => r.data),
  deleteOld: (id: number) => api.delete(`/admin/contracts/${id}`),
  occupants: (id: number) => api.get<ContractOccupant[]>(`/admin/contracts/${id}/occupants`).then((r) => r.data),
  addOccupant: (id: number, payload: object) =>
    api.post(`/admin/contracts/${id}/occupants`, payload).then((r) => r.data),
  updateOccupant: (id: number, payload: object) =>
    api.patch<ContractOccupant>(`/admin/contracts/occupants/${id}`, payload).then((r) => r.data),
  removeOccupant: (id: number) => api.delete(`/admin/contracts/occupants/${id}`),
};
/** Electronic contract documents; `admin` picks the /admin or /tenant variant of each endpoint. */
export const contractDocumentApi = {
  list: (contractId: number, admin: boolean) =>
    api
      .get<ContractDocument[]>(`${admin ? '/admin' : '/tenant'}/contracts/${contractId}/documents`)
      .then((r) => r.data),
  issue: (contractId: number) =>
    api.post<ContractDocument>(`/admin/contracts/${contractId}/documents/issue`).then((r) => r.data),
  pdf: (documentId: number, admin: boolean) =>
    api
      .get<Blob>(`${admin ? '/admin' : '/tenant'}/contracts/documents/${documentId}/pdf`, { responseType: 'blob' })
      .then((r) => r.data),
  verify: (documentId: number, admin: boolean) =>
    api
      .get<ContractDocumentVerify>(`${admin ? '/admin' : '/tenant'}/contracts/documents/${documentId}/verify`)
      .then((r) => r.data),
  verifyFile: (documentId: number, file: File, admin: boolean) => {
    const form = new FormData();
    form.append('file', file);
    return api
      .post<ContractDocumentVerify>(
        `${admin ? '/admin' : '/tenant'}/contracts/documents/${documentId}/verify-file`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      )
      .then((r) => r.data);
  },
  sign: (documentId: number, signatureImage: string) =>
    api
      .post<ContractDocument>(`/tenant/contracts/documents/${documentId}/sign`, { signatureImage, agreed: true })
      .then((r) => r.data),
  requestRenewal: (contractId: number, months: number) =>
    api.post<ContractDocument>(`/tenant/contracts/${contractId}/renewals`, { months }).then((r) => r.data),
};
export const utilityApi = {
  readings: (params: Record<string, unknown>) =>
    api.get<PageResponse<UtilityReading>>('/admin/utility-readings', pageParams(params)).then((r) => r.data),
  createReading: (payload: object) => api.post<UtilityReading>('/admin/utility-readings', payload).then((r) => r.data),
  updateReading: (id: number, payload: object) =>
    api.patch<UtilityReading>(`/admin/utility-readings/${id}`, payload).then((r) => r.data),
  lockReading: (id: number, locked: boolean) =>
    api.patch<UtilityReading>(`/admin/utility-readings/${id}/lock`, { locked }).then((r) => r.data),
  services: (params: Record<string, unknown>) =>
    api.get<PageResponse<ServiceItem>>('/admin/services', pageParams(params)).then((r) => r.data),
  createService: (payload: object) => api.post<ServiceItem>('/admin/services', payload).then((r) => r.data),
  updateService: (id: number, payload: object) =>
    api.patch<ServiceItem>(`/admin/services/${id}`, payload).then((r) => r.data),
  feeConfigs: (params: Record<string, unknown>) =>
    api.get<PageResponse<FeeConfig>>('/admin/fee-configs', pageParams(params)).then((r) => r.data),
  createFeeConfig: (payload: object) => api.post<FeeConfig>('/admin/fee-configs', payload).then((r) => r.data),
  updateFeeConfig: (id: number, payload: object) =>
    api.patch<FeeConfig>(`/admin/fee-configs/${id}`, payload).then((r) => r.data),
};
export const billingApi = {
  adminInvoices: (params: Record<string, unknown>) =>
    api.get<PageResponse<InvoiceFull>>('/admin/invoices', pageParams(params)).then((r) => r.data),
  tenantInvoices: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<InvoiceFull>>('/tenant/invoices', pageParams(params)).then((r) => r.data),
  adminInvoice: (id: number) => api.get<InvoiceFull>(`/admin/invoices/${id}`).then((r) => r.data),
  tenantInvoice: (id: number) => api.get<InvoiceFull>(`/tenant/invoices/${id}`).then((r) => r.data),
  create: (payload: object) => api.post<InvoiceFull>('/admin/invoices', payload).then((r) => r.data),
  issue: (id: number) => api.patch<InvoiceFull>(`/admin/invoices/${id}/issue`).then((r) => r.data),
  issueDrafts: (period?: string) =>
    api
      .patch<{ issued: number }>('/admin/invoices/issue-drafts', undefined, { params: period ? { period } : {} })
      .then((r) => r.data),
  cancel: (id: number) => api.patch<InvoiceFull>(`/admin/invoices/${id}/cancel`).then((r) => r.data),
  delete: (id: number) => api.delete(`/admin/invoices/${id}`),
  adminDebts: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<DebtItem>>('/admin/debts', pageParams(params)).then((r) => r.data),
  tenantDebts: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<DebtItem>>('/tenant/debts', pageParams(params)).then((r) => r.data),
};
export const paymentApi = {
  adminSearch: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<Payment>>('/admin/payments', pageParams(params)).then((r) => r.data),
  tenantSearch: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<Payment>>('/tenant/payments', pageParams(params)).then((r) => r.data),
  adminInvoiceHistory: (id: number, params: Record<string, unknown> = {}) =>
    api.get<PageResponse<Payment>>(`/admin/invoices/${id}/payments`, pageParams(params)).then((r) => r.data),
  tenantInvoiceHistory: (id: number, params: Record<string, unknown> = {}) =>
    api.get<PageResponse<Payment>>(`/tenant/invoices/${id}/payments`, pageParams(params)).then((r) => r.data),
  manual: (invoiceId: number, payload: object) =>
    api.post<Payment>(`/admin/invoices/${invoiceId}/payments/manual`, payload).then((r) => r.data),
  qr: (invoiceId: number) => api.get<PaymentQr>(`/tenant/invoices/${invoiceId}/payment-qr`).then((r) => r.data),
};
export const notificationApi = {
  adminList: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<AppNotification>>('/admin/notifications', pageParams(params)).then((r) => r.data),
  adminUnreadCount: () => api.get<number>('/admin/notifications/unread-count').then((r) => r.data),
  adminMarkRead: (id: number) => api.patch<AppNotification>(`/admin/notifications/${id}/read`).then((r) => r.data),
  create: (payload: object) => api.post<AppNotification>('/admin/notifications', payload).then((r) => r.data),
  tenantList: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<AppNotification>>('/tenant/notifications', pageParams(params)).then((r) => r.data),
  unreadCount: () => api.get<number>('/tenant/notifications/unread-count').then((r) => r.data),
  detail: (id: number) => api.get<AppNotification>(`/tenant/notifications/${id}`).then((r) => r.data),
  markRead: (id: number) => api.patch<AppNotification>(`/tenant/notifications/${id}/read`).then((r) => r.data),
};
export const maintenanceApi = {
  tenantList: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<MaintenanceRequest>>('/maintenance-requests', pageParams(params)).then((r) => r.data),
  tenantDetail: (id: number) => api.get<MaintenanceRequest>(`/maintenance-requests/${id}`).then((r) => r.data),
  create: (payload: object) => api.post<MaintenanceRequest>('/maintenance-requests', payload).then((r) => r.data),
  uploadImages: (id: number, files: File[]) => {
    const form = new FormData();
    files.forEach((file) => form.append('files', file));
    return api
      .post<
        MaintenanceAttachment[]
      >(`/maintenance-requests/${id}/attachments`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then((r) => r.data);
  },
  attachmentImage: (id: number, admin: boolean) =>
    api
      .get<Blob>(`${admin ? '/admin/maintenance-attachments/' : '/maintenance-attachments/'}${id}`, {
        responseType: 'blob',
      })
      .then((r) => r.data),
  adminList: (params: Record<string, unknown> = {}) =>
    api.get<PageResponse<MaintenanceRequest>>('/admin/maintenance-requests', pageParams(params)).then((r) => r.data),
  adminDetail: (id: number) => api.get<MaintenanceRequest>(`/admin/maintenance-requests/${id}`).then((r) => r.data),
  update: (id: number, payload: object) =>
    api.patch<MaintenanceRequest>(`/admin/maintenance-requests/${id}`, payload).then((r) => r.data),
  history: (id: number) =>
    api.get<MaintenanceHistory[]>(`/admin/maintenance-requests/${id}/history`).then((r) => r.data),
  assignees: () => api.get<MaintenanceAssignee[]>('/admin/maintenance-assignees').then((r) => r.data),
};
export const reportsApi = {
  dashboard: (asOf?: string) =>
    api.get<Dashboard>('/admin/reports/dashboard', { params: asOf ? { asOf } : {} }).then((r) => r.data),
  revenue: (from: string, to: string) =>
    api.get<ReportBundleData>('/admin/reports/revenue', { params: { from, to } }).then((r) => r.data),
  invoiced: (from: string, to: string) =>
    api.get<ReportBundleData>('/admin/reports/invoiced', { params: { from, to } }).then((r) => r.data),
  debts: (asOf?: string) =>
    api.get<ReportDebt>('/admin/reports/debts', { params: asOf ? { asOf } : {} }).then((r) => r.data),
  invoiceItems: (from: string, to: string) =>
    api.get<ReportBundleData>('/admin/reports/invoice-items', { params: { from, to } }).then((r) => r.data),
  rooms: () => api.get<ReportCount[]>('/admin/reports/rooms').then((r) => r.data),
  contracts: () => api.get<ReportCount[]>('/admin/reports/contracts').then((r) => r.data),
  maintenance: () => api.get<ReportCount[]>('/admin/reports/maintenance').then((r) => r.data),
  utilities: (fromPeriod: string, toPeriod: string) =>
    api.get<ReportBundleData>('/admin/reports/utilities', { params: { fromPeriod, toPeriod } }).then((r) => r.data),
};
export const assistantApi = {
  chat: (message: string, history: AssistantTurn[] = []) =>
    api.post<AssistantChatResponse>('/assistant/chat', { message, history }).then((r) => r.data),
};
