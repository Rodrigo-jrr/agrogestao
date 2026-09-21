import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { PropertyProvider } from './property/PropertyContext'
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

function RootRedirect() {
  const { isAuthenticated } = useAuth()
  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <PropertyProvider>
          <Routes>
            <Route path="/" element={<RootRedirect />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            <Route element={<ProtectedRoute />}>
              <Route path="/onboarding" element={<OnboardingPage />} />

              <Route element={<RequirePropertyRoute />}>
                <Route element={<Layout />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/financeiro" element={<FinancePage />} />
                  <Route path="/estoque" element={<InventoryPage />} />
                  <Route path="/producao" element={<ProductionPage />} />
                  <Route path="/compras" element={<PurchasesPage />} />
                  <Route path="/vendas" element={<SalesPage />} />
                  <Route path="/atividades" element={<ActivitiesPage />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </PropertyProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
