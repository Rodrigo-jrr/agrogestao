import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { AnimalBatch, HerdSummary } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { useProperty } from '../../property/PropertyContext'
import { formatNumber } from '../../lib/format'
import { speciesPlural } from '../../lib/livestock'
import { BatchesTab } from './BatchesTab'
import { HerdTab } from './HerdTab'
import { TradesTab } from './TradesTab'

type Tab = 'rebanho' | 'lotes' | 'compras-e-vendas'

const tabs: [Tab, string][] = [
  ['rebanho', 'Rebanho'],
  ['lotes', 'Lotes'],
  ['compras-e-vendas', 'Compras e vendas'],
]

export function LivestockPage() {
  const { currentProperty } = useProperty()
  // A aba fica no endereço (?aba=lotes): voltar e compartilhar o link mantêm a tela.
  const [params, setParams] = useSearchParams()
  const tab = (tabs.find(([key]) => key === params.get('aba'))?.[0] ?? 'rebanho') as Tab
  const [batches, setBatches] = useState<AnimalBatch[]>([])
  const [summary, setSummary] = useState<HerdSummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  const role = currentProperty?.role
  const canWrite = role === 'OWNER' || role === 'MANAGER' || role === 'WORKER'
  const canManage = role === 'OWNER' || role === 'MANAGER'

  const load = useCallback(() => {
    Promise.all([livestockApi.listBatches(true), livestockApi.summary()])
      .then(([batchList, herd]) => {
        setBatches(batchList)
        setSummary(herd)
        setError(null)
      })
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  useEffect(() => {
    if (currentProperty) load()
  }, [currentProperty, load])

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-serif text-2xl font-semibold text-terra">Pecuária</h1>
          {summary && (
            <p className="mt-1 text-sm text-terra/60">
              {summary.totalHeads === 0
                ? 'Nenhum animal no rebanho ainda.'
                : summary.bySpecies.map((s) => `${formatNumber(s.heads)} ${speciesPlural[s.species]}`).join(' · ')}
            </p>
          )}
        </div>
        {summary && summary.totalHeads > 0 && (
          <p className="font-serif text-3xl font-semibold text-terra tabular-nums">
            {formatNumber(summary.totalHeads)} <span className="text-base font-normal text-terra/55">cabeças</span>
          </p>
        )}
      </div>
      <FormMessage error={error} />

      <div role="tablist" aria-label="Seções da pecuária" className="flex gap-2 overflow-x-auto border-b border-terra/10">
        {tabs.map(([key, label]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setParams(key === 'rebanho' ? {} : { aba: key }, { replace: true })}
            className={`shrink-0 cursor-pointer px-3 py-2 text-sm font-medium whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
              tab === key ? 'border-b-2 border-rust text-rust' : 'text-terra/50 hover:text-terra'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'rebanho' && <HerdTab batches={batches} canWrite={canWrite} onChanged={load} />}
      {tab === 'lotes' && <BatchesTab batches={batches} canWrite={canWrite} canManage={canManage} onChanged={load} />}
      {tab === 'compras-e-vendas' && <TradesTab batches={batches} canWrite={canWrite} canManage={canManage} onChanged={load} />}
    </div>
  )
}
