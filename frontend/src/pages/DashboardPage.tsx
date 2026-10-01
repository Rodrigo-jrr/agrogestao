import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { dashboardApi, livestockApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { DashboardSummary, HerdSummary, LivestockAlerts } from '../api/types'
import { HealthAgenda } from './livestock/HealthTab'
import { useProperty } from '../property/PropertyContext'
import { PeriodFilter, presetPeriod, type Period } from '../components/PeriodFilter'
import { formatCurrency, formatDate, formatNumber } from '../lib/format'

function stockSeverity(currentQuantity: number, minStock: number): 'Crítico' | 'Baixo' {
  if (minStock <= 0) return 'Baixo'
  return currentQuantity / minStock <= 0.5 ? 'Crítico' : 'Baixo'
}

export function DashboardPage() {
  const { currentProperty } = useProperty()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))
  const [herd, setHerd] = useState<HerdSummary | null>(null)
  const [herdAlerts, setHerdAlerts] = useState<LivestockAlerts | null>(null)

  // Pecuária: só aparece para quem tem rebanho ou algo pendente (falha aqui não derruba o painel).
  useEffect(() => {
    if (!currentProperty) return
    Promise.all([livestockApi.summary(), livestockApi.alerts()])
      .then(([summaryResult, alertsResult]) => {
        setHerd(summaryResult)
        setHerdAlerts(alertsResult)
      })
      .catch(() => {
        setHerd(null)
        setHerdAlerts(null)
      })
  }, [currentProperty])
  const showHerd =
    !!herd && !!herdAlerts && (herd.totalHeads > 0 || herdAlerts.followUps.length > 0 || herdAlerts.withdrawals.length > 0)

  function load() {
    if (!currentProperty) return
    setLoading(true)
    setError(null)
    dashboardApi
      .summary(period.from, period.to)
      .then(setSummary)
      .catch((err) => setError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentProperty, period])

  if (loading && !summary) {
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
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-serif text-3xl font-semibold text-terra">Dashboard</h1>
          <p className="text-sm text-terra/50">
            Resumo de {currentProperty?.name} · {formatDate(summary.periodFrom)} a {formatDate(summary.periodTo)}
          </p>
        </div>
        <PeriodFilter value={period} onChange={setPeriod} />
      </div>

      <div className={`grid grid-cols-1 gap-4 sm:grid-cols-3 ${loading ? 'opacity-60' : ''}`} aria-busy={loading}>
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
              Margem de {formatNumber(Math.round((summary.result / summary.totalIncome) * 1000) / 10)}%
            </p>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-base font-semibold text-terra">Produção no período</h2>
          {summary.production.length === 0 ? (
            <p className="text-sm text-terra/40">Nenhum registro de produção neste período.</p>
          ) : (
            <ul className="divide-y divide-terra/5 text-sm">
              {summary.production.map((item) => (
                <li key={item.productName} className="flex justify-between py-2">
                  <span className="text-terra/70">{item.productName}</span>
                  <span className="font-medium text-terra">
                    {formatNumber(item.totalQuantity)} {item.unit}
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
                        {formatNumber(product.currentQuantity)} {product.unit} restantes
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

      {showHerd && (
        <div className="rounded-2xl border border-terra/10 bg-white p-5 shadow-sm">
          <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
            <h2 className="text-base font-semibold text-terra">
              Pecuária{' '}
              <span className="text-sm font-normal text-terra/55">
                · {formatNumber(herd!.totalHeads)} {herd!.totalHeads === 1 ? 'cabeça' : 'cabeças'}
              </span>
            </h2>
            <Link to="/pecuaria?aba=sanidade" className="text-sm text-rust hover:underline">
              Ver sanidade
            </Link>
          </div>
          <HealthAgenda alerts={herdAlerts} canWrite={false} />
        </div>
      )}
    </div>
  )
}
