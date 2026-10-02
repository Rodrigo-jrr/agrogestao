import { useId, useState, type FormEvent } from 'react'
import { cropsApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Crop } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { cardClass, labelClass } from './shared'

function CropForm({ onSaved }: { onSaved: () => void }) {
  const ids = useId()
  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await cropsApi.create({ name })
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
          Nome da cultura
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={60}
          placeholder="Ex.: Soja, Milho, Café"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Cadastrar cultura'}
      </button>
    </form>
  )
}

export function CropsTab({
  crops,
  canWrite,
  canManage,
  onChanged,
}: {
  crops: Crop[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  const visible = crops.filter((c) => showArchived || !c.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(c: Crop) {
    const ok = await confirm({
      title: `Excluir a cultura ${c.name}?`,
      message: 'Só culturas sem nenhuma safra cadastrada podem ser excluídas. Já em uso, o caminho é arquivar.',
      confirmLabel: 'Excluir cultura',
    })
    if (ok) run(() => cropsApi.remove(c.id))
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova cultura</h2>
          <CropForm onSaved={onChanged} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Culturas cadastradas</h2>
          <label className="flex items-center gap-2 text-xs text-terra/60">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
            Mostrar arquivadas
          </label>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">
            Nenhuma cultura cadastrada ainda. Cadastre a primeira para começar a registrar safras.
          </p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((c) => (
              <li key={c.id} className={`flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-3 ${c.archived ? 'opacity-60' : ''}`}>
                <p className="font-medium text-terra">
                  {c.name}
                  {c.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivada</span>}
                </p>
                <div className="flex gap-x-4 text-sm">
                  {canManage &&
                    (c.archived ? (
                      <button onClick={() => run(() => cropsApi.unarchive(c.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Reativar
                      </button>
                    ) : (
                      <button onClick={() => run(() => cropsApi.archive(c.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Arquivar
                      </button>
                    ))}
                  {canManage && (
                    <button onClick={() => handleDelete(c)} className={`cursor-pointer ${linkDangerClass}`}>
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
