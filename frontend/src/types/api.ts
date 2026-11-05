export type Role = 'ADMIN' | 'TENANT';

export interface User {
  userId: number;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  role: Role;
  active: boolean;
}
export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}
export interface ApiError {
  code: string;
  message: string;
}
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
export interface CountRow {
  code: string;
  count: number;
}
export interface MoneyRow {
  code: string;
  amount: number;
}
export interface Dashboard {
  totalRooms: number;
  occupiedRooms: number;
  vacantRooms: number;
  occupancyRate: number;
  activeContracts: number;
  expiringContracts: number;
  expiredContracts: number;
  activeTenants: number;
  pendingRentalRequests: number;
  maintenanceStatuses: CountRow[];
  unpaidInvoices: number;
  overdueInvoices: number;
  outstandingDebt: number;
}
export interface ReportBundle {
  rows: MoneyRow[];
}
export interface RoomRevenue {
  roomId: number;
  roomCode: string;
  amount: number;
}
export interface Debt {
  invoiceCount: number;
  overdueInvoiceCount: number;
  outstanding: number;
  overdue: number;
}
export interface Invoice {
  id: number;
  invoiceCode: string;
  contractId: number;
  roomId: number;
  roomCode: string;
  payerUserId: number;
  billingPeriod: string;
  issueDate: string;
  dueDate: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  status: string;
}
export interface DebtItem {
  invoiceId: number;
  invoiceCode: string;
  contractId: number;
  roomId: number;
  roomCode: string;
  payerUserId: number;
  payerName: string;
  billingPeriod: string;
  dueDate: string;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  status: string;
  overdueDays: number;
}
export interface Contract {
  id: number;
  contractCode: string;
  tenantUserId: number;
  tenantName: string;
  roomId: number;
  roomCode: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: string;
}
export type ContractDocumentStatus = 'AWAITING_SIGNATURE' | 'SIGNED' | 'VOID';
export interface ContractDocument {
  id: number;
  contractId: number;
  contractCode: string;
  documentType: 'CONTRACT' | 'RENEWAL';
  status: ContractDocumentStatus;
  previousEndDate?: string | null;
  newEndDate?: string | null;
  renewalMonths?: number | null;
  contentHash: string;
  signerUserId: number;
  signerName: string;
  signedAt?: string | null;
  signerIp?: string | null;
  signedPdfHash?: string | null;
  createdAt: string;
  voidedAt?: string | null;
}
export interface ContractDocumentVerify {
  documentId: number;
  valid: boolean;
  expectedHash: string;
  actualHash?: string | null;
  checkedAt: string;
}
export interface Notification {
  id: number;
  title: string;
  content: string;
  read: boolean;
  createdAt: string;
  notificationType: string;
}
export type RoomStatus = 'VACANT' | 'OCCUPIED' | 'FULL' | 'MAINTENANCE' | 'INACTIVE';
export interface Room {
  id: number;
  roomCode: string;
  roomName?: string | null;
  floorNumber: number;
  areaM2: number;
  baseRent: number;
  maxOccupants: number;
  allowsSharing: boolean;
  status: RoomStatus;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
export interface RoomOccupant {
  occupantId: number;
  userId?: number | null;
  fullName: string;
  phoneNumber?: string | null;
  identityDocumentNumber?: string | null;
  contractId: number;
  contractCode: string;
  primaryTenant: boolean;
  moveInDate: string;
  moveOutDate?: string | null;
}
export interface RentalRequest {
  id: number;
  requestCode: string;
  applicantUserId?: number;
  roomId: number;
  roomCode: string;
  contactName: string;
  contactPhone?: string;
  contactEmail?: string | null;
  expectedOccupantCount: number;
  expectedMoveInDate: string;
  note?: string | null;
  status: 'PENDING' | 'NEEDS_INFO' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'EXPIRED';
  processedByUserId?: number | null;
  processedAt?: string | null;
  processingNote?: string | null;
  createdAt: string;
  updatedAt?: string;
}
export interface RentalRequestSummary
  extends Pick<
    RentalRequest,
    | 'id'
    | 'requestCode'
    | 'roomId'
    | 'roomCode'
    | 'contactName'
    | 'expectedOccupantCount'
    | 'expectedMoveInDate'
    | 'status'
    | 'createdAt'
  > {}
export interface TenantSummary {
  userId: number;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string | null;
  contactAddress?: string | null;
  active: boolean;
}
export interface TenantDetail extends TenantSummary {
  identityDocumentType?: string | null;
  identityDocumentNumber?: string | null;
  identityIssuedDate?: string | null;
  identityIssuedPlace?: string | null;
  notes?: string | null;
  profileCreatedAt?: string;
  profileUpdatedAt?: string;
}
export interface ContractDetail {
  id: number;
  contractCode: string;
  tenantUserId: number;
  roomId: number;
  roomCode: string;
  rentalRequestId?: number | null;
  startDate: string;
  endDate: string;
  actualEndDate?: string | null;
  monthlyRent: number;
  depositAmount: number;
  billingCycleMonths: number;
  dueDay: number;
  status: string;
  terms?: string | null;
  notes?: string | null;
  activatedAt?: string | null;
  terminatedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
export interface ContractOccupant {
  id: number;
  contractId: number;
  userId?: number | null;
  fullName: string;
  phoneNumber?: string | null;
  identityDocumentNumber?: string | null;
  primaryTenant: boolean;
  moveInDate: string;
  moveOutDate?: string | null;
  createdAt?: string;
}
export type UtilityType = 'ELECTRICITY' | 'WATER';
export type FeePricingMode = 'PER_ROOM' | 'PER_PERSON' | 'PER_USAGE';
export interface UtilityReading {
  id: number;
  roomId: number;
  utilityType: UtilityType;
  billingPeriod: string;
  readingStart: number;
  readingEnd: number;
  consumption: number;
  readingDate: string;
  locked: boolean;
  note?: string | null;
  createdAt?: string;
  updatedAt?: string;
}
export interface ServiceItem {
  id: number;
  serviceCode: string;
  serviceName: string;
  pricingMode: FeePricingMode;
  defaultUnitPrice: number;
  active: boolean;
  createdAt?: string;
}
export interface FeeConfig {
  id: number;
  feeCode: string;
  feeName: string;
  unitName: string;
  unitPrice: number;
  pricingMode: FeePricingMode;
  effectiveFrom: string;
  effectiveTo?: string | null;
  active: boolean;
  createdAt?: string;
}
export type InvoiceStatus = 'DRAFT' | 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';
export interface InvoiceItem {
  id: number;
  itemType: string;
  description: string;
  quantity: number;
  unitName: string;
  unitPrice: number;
  amount: number;
  feeConfigId?: number | null;
  utilityReadingId?: number | null;
  serviceId?: number | null;
}
export interface InvoiceFull {
  id: number;
  invoiceCode: string;
  contractId: number;
  roomId: number;
  roomCode: string;
  payerUserId: number;
  billingPeriod: string;
  issueDate: string;
  dueDate: string;
  rentAmount: number;
  utilityAmount: number;
  serviceAmount: number;
  adjustmentAmount: number;
  totalAmount: number;
  paidAmount: number;
  outstandingAmount: number;
  status: InvoiceStatus;
  note?: string | null;
  issuedAt?: string | null;
  items: InvoiceItem[];
}
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'EXPIRED' | 'MANUAL_REVIEW';
export interface Payment {
  id: number;
  transactionCode: string;
  invoiceId: number;
  payerUserId: number;
  providerCode?: string | null;
  paymentMethod: string;
  amount: number;
  currencyCode: string;
  status: PaymentStatus;
  providerTransactionCode?: string | null;
  confirmedAt?: string | null;
}
export interface PaymentQr {
  invoiceId: number;
  invoiceCode: string;
  amount: number;
  currencyCode: string;
  bankCode: string;
  accountNumber: string;
  accountName: string;
  transferContent: string;
  qrUrl: string;
}
export type MaintenanceStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type MaintenancePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export interface MaintenanceAttachment {
  id: number;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  createdAt: string;
}
export interface MaintenanceAssignee {
  id: number;
  fullName: string;
}
export interface MaintenanceRequest {
  id: number;
  requestCode: string;
  reporterUserId: number;
  reporterName?: string | null;
  roomId: number;
  roomCode?: string | null;
  contractId?: number | null;
  issueType: string;
  title: string;
  description: string;
  priority: MaintenancePriority;
  status: MaintenanceStatus;
  assignedToUserId?: number | null;
  assignedToName?: string | null;
  estimatedCost?: number | null;
  actualCost?: number | null;
  resolutionNote?: string | null;
  rejectionReason?: string | null;
  createdAt: string;
  updatedAt?: string;
  resolvedAt?: string | null;
  attachments: MaintenanceAttachment[];
}
export interface MaintenanceHistory {
  id: number;
  requestId: number;
  oldStatus?: MaintenanceStatus | null;
  newStatus: MaintenanceStatus;
  note?: string | null;
  changedByUserId: number;
  changedAt: string;
}
export interface AppNotification {
  id: number;
  recipientUserId: number;
  notificationType: string;
  title: string;
  content: string;
  relatedEntityType?: string | null;
  relatedEntityId?: number | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}
export interface ReportCount {
  code: string;
  count: number;
}
export interface ReportMoney {
  code: string;
  amount: number;
}
export interface ReportBundleData {
  rows: ReportMoney[];
}
export interface ReportDebt {
  invoiceCount: number;
  overdueInvoiceCount: number;
  outstanding: number;
  overdue: number;
}
export interface AssistantAction {
  label: string;
  path: string;
}
export interface AssistantChatResponse {
  answer: string;
  intent: string;
  answeredAt: string;
  actions?: AssistantAction[];
  suggestions?: string[];
  mode?: 'AI' | 'BASIC';
}
export interface AssistantTurn {
  role: 'user' | 'assistant';
  text: string;
}
