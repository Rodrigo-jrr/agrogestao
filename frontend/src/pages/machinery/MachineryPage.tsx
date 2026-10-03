import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { machineTypesApi, machinesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Machine, MachineType } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { useProperty } from '../../property/PropertyContext'
import { MachineTypesTab } from './MachineTypesTab'
import { MachinesTab } from './MachinesTab'

type Tab = 'maquinas' | 'tipos'

const tabs: [Tab, string][] = [
  ['maquinas', 'Máquinas'],
  ['tipos', 'Tipos'],
]

export function MachineryPage() {
  const { currentProperty } = useProperty()
  // A aba fica no endereço (?aba=tipos): voltar e compartilhar o link mantêm a tela.
  const [params, setParams] = useSearchParams()
  const tab = (tabs.find(([key]) => key === params.get('aba'))?.[0] ?? 'maquinas') as Tab
  const [types, setTypes] = useState<MachineType[]>([])
  const [machines, setMachines] = useState<Machine[]>([])
  const [error, setError] = useState<string | null>(null)

  const role = currentProperty?.role
  const canWrite = role === 'OWNER' || role === 'MANAGER' || role === 'WORKER'
  const canManage = role === 'OWNER' || role === 'MANAGER'

  const load = useCallback(() => {
    Promise.all([machineTypesApi.list(true), machinesApi.list(true)])
      .then(([typeList, machineList]) => {
        setTypes(typeList)
        setMachines(machineList)
        setError(null)
      })
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  useEffect(() => {
    if (currentProperty) load()
  }, [currentProperty, load])

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Máquinas</h1>
      <FormMessage error={error} />

      <div role="tablist" aria-label="Seções de máquinas" className="flex gap-2 overflow-x-auto border-b border-terra/10">
        {tabs.map(([key, label]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setParams(key === 'maquinas' ? {} : { aba: key }, { replace: true })}
            className={`shrink-0 cursor-pointer px-3 py-2 text-sm font-medium whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
              tab === key ? 'border-b-2 border-rust text-rust' : 'text-terra/50 hover:text-terra'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'maquinas' && <MachinesTab machines={machines} types={types} canWrite={canWrite} canManage={canManage} onChanged={load} />}
      {tab === 'tipos' && <MachineTypesTab types={types} canWrite={canWrite} canManage={canManage} onChanged={load} />}
    </div>
  )
}
