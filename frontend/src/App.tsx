import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { PropertyProvider } from './property/PropertyContext'
import { ConfirmProvider } from './components/ConfirmDialog'
import { ProtectedRoute, RequirePropertyRoute } from './components/ProtectedRoute'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { OnboardingPage } from './pages/OnboardingPage'
import { DashboardPage } from './pages/DashboardPage'
import { FinancePage } from './pages/FinancePage'
import { InventoryPage } from './pages/InventoryPage'
import { ProductionPage } from './pages/ProductionPage'
import { PurchasesPage } from './pages/PurchasesPage'
import { SalesPage } from './pages/SalesPage'
import { ActivitiesPage } from './pages/ActivitiesPage'
import { MembersPage } from './pages/MembersPage'
import { AdminPage } from './pages/AdminPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { NotFoundPage } from './pages/NotFoundPage'

function RootRedirect() {
  const { isAuthenticated } = useAuth()
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ConfirmProvider>
        <PropertyProvider>
          <Routes>
            <Route path="/" element={<RootRedirect />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
            <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
            <Route path="/verificar-email" element={<VerifyEmailPage />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/onboarding" element={<OnboardingPage />} />
              <Route path="/admin" element={<AdminPage />} />

              <Route element={<RequirePropertyRoute />}>
                <Route element={<Layout />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/financeiro" element={<FinancePage />} />
                  <Route path="/estoque" element={<InventoryPage />} />
                  <Route path="/producao" element={<ProductionPage />} />
                  <Route path="/compras" element={<PurchasesPage />} />
                  <Route path="/vendas" element={<SalesPage />} />
                  <Route path="/atividades" element={<ActivitiesPage />} />
                  <Route path="/equipe" element={<MembersPage />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </PropertyProvider>
        </ConfirmProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
