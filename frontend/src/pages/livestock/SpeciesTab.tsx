import { useId, useState, type FormEvent } from 'react'
import { speciesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { SpeciesCatalogItem } from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { cardClass, labelClass } from './shared'

function SpeciesForm({ onSaved }: { onSaved: () => void }) {
  const ids = useId()
  const [name, setName] = useState('')
  const [allowsIndividual, setAllowsIndividual] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await speciesApi.create({ name, allowsIndividual })
      setName('')
      setAllowsIndividual(true)
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
          Nome da espécie
        </label>
        <input
          id={`${ids}-name`}
          required
          maxLength={60}
          placeholder="Ex.: Bovino, Ovino, Codorna"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <fieldset>
        <legend className={labelClass}>Cadastro</legend>
        <div className="grid gap-1 sm:grid-cols-2">
          {(
            [
              [true, 'Com brinco', 'Cada animal pode ser cadastrado individualmente, com brinco, idade, peso e histórico.'],
              [false, 'Só por lote', 'Conta só a quantidade, como aves e peixes.'],
            ] as [boolean, string, string][]
          ).map(([value, title, help]) => (
            <label
              key={String(value)}
              className={`cursor-pointer rounded-md border px-3 py-2 text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                allowsIndividual === value ? 'border-rust bg-rust/10' : 'border-terra/15'
              }`}
            >
              <input
                type="radio"
                name={`${ids}-allows`}
                checked={allowsIndividual === value}
                onChange={() => setAllowsIndividual(value)}
                className="sr-only"
              />
              <span className={`block font-medium ${allowsIndividual === value ? 'text-rust' : 'text-terra'}`}>{title}</span>
              <span className="block text-xs text-terra/60">{help}</span>
            </label>
          ))}
        </div>
        <p className="mt-1 text-xs text-terra/55">Depois de usada por um animal ou lote, essa opção não muda mais.</p>
      </fieldset>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Cadastrar espécie'}
      </button>
    </form>
  )
}

export function SpeciesTab({
  species,
  canWrite,
  canManage,
  onChanged,
}: {
  species: SpeciesCatalogItem[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [error, setError] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)

  const visible = species.filter((s) => showArchived || !s.archived)

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
      onChanged()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  async function handleDelete(s: SpeciesCatalogItem) {
    const ok = await confirm({
      title: `Excluir a espécie ${s.name}?`,
      message: 'Só espécies sem nenhum animal ou lote cadastrado podem ser excluídas. Já em uso, o caminho é arquivar.',
      confirmLabel: 'Excluir espécie',
    })
    if (ok) run(() => speciesApi.remove(s.id))
  }

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start`}>
          <h2 className="mb-3 text-sm font-semibold text-terra">Nova espécie</h2>
          <SpeciesForm onSaved={onChanged} />
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-2' : 'lg:col-span-3'}`}>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold text-terra">Espécies cadastradas</h2>
          <label className="flex items-center gap-2 text-xs text-terra/60">
            <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} className="accent-rust" />
            Mostrar arquivadas
          </label>
        </div>
        <FormMessage error={error} />
        {visible.length === 0 ? (
          <p className="py-6 text-center text-sm text-terra/50">
            Nenhuma espécie cadastrada ainda. Cadastre a primeira para começar a criar lotes e animais.
          </p>
        ) : (
          <ul className="divide-y divide-terra/5">
            {visible.map((s) => (
              <li key={s.id} className={`flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-3 ${s.archived ? 'opacity-60' : ''}`}>
                <div>
                  <p className="font-medium text-terra">
                    {s.name}
                    {s.archived && <span className="ml-2 rounded-full bg-terra/5 px-1.5 text-xs font-normal">arquivada</span>}
                  </p>
                  <p className="text-xs text-terra/55">{s.allowsIndividual ? 'Com brinco (individual)' : 'Só por lote (contagem)'}</p>
                </div>
                <div className="flex gap-x-4 text-sm">
                  {canManage &&
                    (s.archived ? (
                      <button onClick={() => run(() => speciesApi.unarchive(s.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Reativar
                      </button>
                    ) : (
                      <button onClick={() => run(() => speciesApi.archive(s.id))} className={`cursor-pointer ${linkNeutralClass}`}>
                        Arquivar
                      </button>
                    ))}
                  {canManage && (
                    <button onClick={() => handleDelete(s)} className={`cursor-pointer ${linkDangerClass}`}>
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
