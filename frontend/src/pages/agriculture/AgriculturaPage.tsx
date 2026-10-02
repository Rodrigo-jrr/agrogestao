import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { cropCyclesApi, cropsApi, fieldsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Crop, CropCycle, Field } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { useProperty } from '../../property/PropertyContext'
import { CropCyclesTab } from './CropCyclesTab'
import { CropsTab } from './CropsTab'
import { FieldsTab } from './FieldsTab'

type Tab = 'talhoes' | 'culturas' | 'safras'

const tabs: [Tab, string][] = [
  ['talhoes', 'Talhões'],
  ['culturas', 'Culturas'],
  ['safras', 'Safras'],
]

export function AgriculturaPage() {
  const { currentProperty } = useProperty()
  // A aba fica no endereço (?aba=culturas): voltar e compartilhar o link mantêm a tela.
  const [params, setParams] = useSearchParams()
  const tab = (tabs.find(([key]) => key === params.get('aba'))?.[0] ?? 'talhoes') as Tab
  const [fields, setFields] = useState<Field[]>([])
  const [crops, setCrops] = useState<Crop[]>([])
  const [cycles, setCycles] = useState<CropCycle[]>([])
  const [error, setError] = useState<string | null>(null)

  const role = currentProperty?.role
  const canWrite = role === 'OWNER' || role === 'MANAGER' || role === 'WORKER'
  const canManage = role === 'OWNER' || role === 'MANAGER'

  const load = useCallback(() => {
    Promise.all([fieldsApi.list(true), cropsApi.list(true), cropCyclesApi.list(true)])
      .then(([fieldList, cropList, cycleList]) => {
        setFields(fieldList)
        setCrops(cropList)
        setCycles(cycleList)
        setError(null)
      })
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  useEffect(() => {
    if (currentProperty) load()
  }, [currentProperty, load])

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Agricultura</h1>
      <FormMessage error={error} />

      <div role="tablist" aria-label="Seções da agricultura" className="flex gap-2 overflow-x-auto border-b border-terra/10">
        {tabs.map(([key, label]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setParams(key === 'talhoes' ? {} : { aba: key }, { replace: true })}
            className={`shrink-0 cursor-pointer px-3 py-2 text-sm font-medium whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
              tab === key ? 'border-b-2 border-rust text-rust' : 'text-terra/50 hover:text-terra'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'talhoes' && <FieldsTab fields={fields} canWrite={canWrite} canManage={canManage} onChanged={load} />}
      {tab === 'culturas' && <CropsTab crops={crops} canWrite={canWrite} canManage={canManage} onChanged={load} />}
      {tab === 'safras' && (
        <CropCyclesTab cycles={cycles} fields={fields} crops={crops} canWrite={canWrite} canManage={canManage} onChanged={load} />
      )}
    </div>
  )
}
