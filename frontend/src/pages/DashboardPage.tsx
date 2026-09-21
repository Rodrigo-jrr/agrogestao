import { useEffect, useState } from 'react'
import { dashboardApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { DashboardSummary } from '../api/types'
import { useProperty } from '../property/PropertyContext'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function stockSeverity(currentQuantity: number, minStock: number): 'Crítico' | 'Baixo' {
  if (minStock <= 0) return 'Baixo'
  return currentQuantity / minStock <= 0.5 ? 'Crítico' : 'Baixo'
}

export function DashboardPage() {
  const { currentProperty } = useProperty()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  function load() {
    if (!currentProperty) return
    setLoading(true)
    setError(null)
    dashboardApi
      .summary()
      .then(setSummary)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty])

  if (loading) {
    return <div className="text-terra/50">Carregando resumo da propriedade...</div>
  }

  if (error || !summary) {
    return (
      <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
        <p className="mb-3 text-sm text-rust">{error ?? 'Não foi possível carregar o resumo da propriedade.'}</p>
        <button
          onClick={load}
          className="rounded-md border border-terra/15 px-3 py-1.5 text-sm text-terra hover:bg-terra/5"
        >
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl font-semibold text-terra">Dashboard</h1>
        <p className="text-sm text-terra/50">Resumo de {currentProperty?.name}</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wider text-terra/45">Receita do período</p>
          <p className="mt-2 font-serif text-3xl font-semibold text-terra">{formatCurrency(summary.totalIncome)}</p>
        </div>
        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wider text-terra/45">Despesa do período</p>
          <p className="mt-2 font-serif text-3xl font-semibold text-terra">{formatCurrency(summary.totalExpense)}</p>
        </div>
        <div className="rounded-2xl bg-mata p-5 text-terreiro shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-wider text-terreiro/60">Resultado</p>
          <p className="mt-2 font-serif text-3xl font-semibold">{formatCurrency(summary.result)}</p>
          {summary.totalIncome > 0 && (
            <p className="mt-1 text-xs text-terreiro/60">
              Margem de {((summary.result / summary.totalIncome) * 100).toFixed(1)}%
            </p>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-base font-semibold text-terra">Produção recente</h2>
          {summary.production.length === 0 ? (
            <p className="text-sm text-terra/40">Nenhum registro de produção neste período.</p>
          ) : (
            <ul className="divide-y divide-terra/5 text-sm">
              {summary.production.map((item) => (
                <li key={item.productName} className="flex justify-between py-2">
                  <span className="text-terra/70">{item.productName}</span>
                  <span className="font-medium text-terra">
                    {item.totalQuantity} {item.unit}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-base font-semibold text-terra">Alertas de estoque baixo</h2>
          {summary.lowStockAlerts.length === 0 ? (
            <p className="text-sm text-terra/40">Nenhum item abaixo do estoque mínimo.</p>
          ) : (
            <ul className="space-y-2">
              {summary.lowStockAlerts.map((product) => {
                const severity = stockSeverity(product.currentQuantity, product.minStock)
                return (
                  <li
                    key={product.id}
                    className="flex items-center justify-between rounded-lg bg-terreiro px-3 py-2"
                  >
                    <div>
                      <p className="text-sm font-medium text-terra">{product.name}</p>
                      <p className="text-xs text-terra/50">
                        {product.currentQuantity} {product.unit} restantes
                      </p>
                    </div>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                        severity === 'Crítico' ? 'bg-rust/15 text-rust' : 'bg-trigo/20 text-trigo-dark'
                      }`}
                    >
                      {severity}
                    </span>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
