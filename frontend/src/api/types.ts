export type PropertyRole = 'OWNER' | 'MANAGER' | 'WORKER' | 'VIEWER'

export interface AuthResponse {
  token: string
  userId: number
  name: string
  email: string
}

export interface PropertySummary {
  id: number
  organizationId: number
  name: string
  location: string | null
  totalAreaHa: number | null
  role: PropertyRole
}

export interface ActivityInput {
  name: string
  type: string
  areaHa?: number | null
}

export interface Activity {
  id: number
  name: string
  type: string
  areaHa: number | null
  responsible: string | null
}

export type TransactionType = 'INCOME' | 'EXPENSE'

export interface FinancialTransaction {
  id: number
  type: TransactionType
  description: string
  amount: number
  transactionDate: string
  costCenterId: number | null
}

export interface AccountPayable {
  id: number
  description: string
  amount: number
  dueDate: string
  supplierId: number | null
  paid: boolean
}

export interface AccountReceivable {
  id: number
  description: string
  amount: number
  dueDate: string
  customerId: number | null
  received: boolean
}

export interface CostCenter {
  id: number
  name: string
  activityId: number | null
}

export interface Product {
  id: number
  name: string
  category: string | null
  unit: string
  minStock: number
  currentQuantity: number
}

export type MovementType = 'IN' | 'OUT' | 'ADJUST'

export interface InventoryMovement {
  id: number
  productId: number
  type: MovementType
  quantity: number
  unitCost: number | null
  movementDate: string
  notes: string | null
}

export interface Supplier {
  id: number
  name: string
  document: string | null
  phone: string | null
  email: string | null
}

export interface Customer {
  id: number
  name: string
  document: string | null
  phone: string | null
  email: string | null
}

export interface Purchase {
  id: number
  supplierId: number
  purchaseDate: string
  totalAmount: number
  canceled: boolean
}

export interface Sale {
  id: number
  customerId: number
  saleDate: string
  totalAmount: number
  canceled: boolean
}

export interface ProductionRecord {
  id: number
  productName: string
  quantity: number
  unit: string
  productionDate: string
  activityId: number | null
}

export interface ProductionSummaryItem {
  productName: string
  totalQuantity: number
  unit: string
}

export interface DashboardSummary {
  periodFrom: string
  periodTo: string
  totalIncome: number
  totalExpense: number
  result: number
  production: ProductionSummaryItem[]
  lowStockAlerts: Product[]
}

export interface PropertyMemberInfo {
  id: number
  userId: number
  name: string
  email: string | null
  role: PropertyRole
  currentUser: boolean
}

export interface CashFlowMonth {
  month: string
  income: number
  expense: number
  net: number
  balance: number
  pendingReceivables: number
  pendingPayables: number
  projectedBalance: number
}

export interface CashFlow {
  from: string
  to: string
  openingBalance: number
  overdueReceivables: number
  overduePayables: number
  months: CashFlowMonth[]
}
