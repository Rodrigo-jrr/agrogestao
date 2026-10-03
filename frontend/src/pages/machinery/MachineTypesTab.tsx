import { useId, useState, type FormEvent } from 'react'
import { machineTypesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { MachineType } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { cardClass, labelClass } from './shared'

function MachineTypeForm({ onSaved }: { onSaved: () => void }) {
  const ids = useId()
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await machineTypesApi.create({ name })
      setName('')
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage error={error} />
      <div>
        <label htmlFor={`${ids}-name`} className={labelClass}>
          Nome do tipo
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={60}
          placeholder="Ex.: Trator, Colheitadeira, Pulverizador"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Cadastrar tipo'}
      </button>
    </form>
  )
}

export function MachineTypesTab({
  types,
  canWrite,
  canManage,
  onChanged,
}: {
  types: MachineType[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  const visible = types.filter((t) => showArchived || !t.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(t: MachineType) {
    const ok = await confirm({
      title: `Excluir o tipo ${t.name}?`,
      message: 'Só tipos sem nenhuma máquina cadastrada podem ser excluídos. Já em uso, o caminho é arquivar.',
      confirmLabel: 'Excluir tipo',
    })
    if (ok) run(() => machineTypesApi.remove(t.id))
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Novo tipo de máquina</h2>
          <MachineTypeForm onSaved={onChanged} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Tipos cadastrados</h2>
          <label className="flex items-center gap-2 text-xs text-terra/60">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
            Mostrar arquivados
          </label>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">
            Nenhum tipo cadastrado ainda. Cadastre o primeiro para começar a registrar máquinas.
          </p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((t) => (
              <li key={t.id} className={`flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-3 ${t.archived ? 'opacity-60' : ''}`}>
                <p className="font-medium text-terra">
                  {t.name}
                  {t.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivado</span>}
                </p>
                <div className="flex gap-x-4 text-sm">
                  {canManage &&
                    (t.archived ? (
                      <button onClick={() => run(() => machineTypesApi.unarchive(t.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Reativar
                      </button>
                    ) : (
                      <button onClick={() => run(() => machineTypesApi.archive(t.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Arquivar
                      </button>
                    ))}
                  {canManage && (
                    <button onClick={() => handleDelete(t)} className={`cursor-pointer ${linkDangerClass}`}>
                      Excluir
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
