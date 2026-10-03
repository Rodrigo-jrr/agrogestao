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
import { AgriculturaPage } from './pages/agriculture/AgriculturaPage'
import { MachineryPage } from './pages/machinery/MachineryPage'
import { MembersPage } from './pages/MembersPage'
import { AdminPage } from './pages/AdminPage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { ProfilePage } from './pages/ProfilePage'
import { PropertySettingsPage } from './pages/PropertySettingsPage'
import { AuditPage } from './pages/AuditPage'
import { InvitationPage } from './pages/InvitationPage'
import { ConfirmEmailChangePage } from './pages/ConfirmEmailChangePage'
import { LivestockPage } from './pages/livestock/LivestockPage'
import { AnimalDetailPage } from './pages/livestock/AnimalDetailPage'
import { TermsPage } from './pages/TermsPage'
import { PrivacyPage } from './pages/PrivacyPage'

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
            <Route path="/confirmar-novo-email" element={<ConfirmEmailChangePage />} />
            <Route path="/convite" element={<InvitationPage />} />
            <Route path="/termos" element={<TermsPage />} />
            <Route path="/privacidade" element={<PrivacyPage />} />

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
                  <Route path="/agricultura" element={<AgriculturaPage />} />
                  <Route path="/maquinas" element={<MachineryPage />} />
                  <Route path="/pecuaria" element={<LivestockPage />} />
                  <Route path="/pecuaria/animais/:id" element={<AnimalDetailPage />} />
                  <Route path="/equipe" element={<MembersPage />} />
                  <Route path="/propriedade" element={<PropertySettingsPage />} />
                  <Route path="/historico" element={<AuditPage />} />
                  <Route path="/perfil" element={<ProfilePage />} />
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
