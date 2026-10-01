import { useId, useState } from 'react'
import { Link } from 'react-router-dom'
import { livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { AnimalBatch, AnimalStatus, Species } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { Pagination } from '../../components/Pagination'
import { inputClass, outlineButtonClass, primaryButtonClass } from '../../components/ui'
import { formatNumber, todayIso } from '../../lib/format'
import { formatAge, individualSpecies, sexLabels, speciesLabels, statusLabels } from '../../lib/livestock'
import { useDebounced } from '../../lib/useDebounced'
import { usePaged } from '../../lib/usePaged'
import { AnimalForm } from './AnimalForm'
import { cardClass, labelClass } from './shared'

export function HerdTab({
  batches,
  canWrite,
  onChanged,
}: {
  batches: AnimalBatch[]
  canWrite: boolean
  onChanged: () => void
}) {
  const ids = useId()
  const [status, setStatus] = useState<AnimalStatus | ''>('ATIVO')
  const [batchId, setBatchId] = useState('')
  const [species, setSpecies] = useState<Species | ''>('')
  const [search, setSearch] = useState('')
  const q = useDebounced(search)
  const [creating, setCreating] = useState(false)
  const [created, setCreated] = useState<string | null>(null)
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [moveTo, setMoveTo] = useState('')
  const [moveDate, setMoveDate] = useState(todayIso)
  const [moveResult, setMoveResult] = useState<{ error?: string; success?: string }>({})

  const animals = usePaged(
    (page) =>
      livestockApi.listAnimals({
        status: status || undefined,
        batchId: batchId ? Number(batchId) : undefined,
        species: species || undefined,
        q: q || undefined,
        page,
      }),
    [status, batchId, species, q],
  )

  const individualBatches = batches.filter((b) => b.tracking === 'INDIVIDUAL')
  const selectedAnimals = animals.items.filter((a) => selected.has(a.id))
  const selectableIds = animals.items.filter((a) => a.status === 'ATIVO').map((a) => a.id)
  const allSelected = selectableIds.length > 0 && selectableIds.every((id) => selected.has(id))

  function toggle(id: number) {
    setMoveResult({})
    setCreated(null)
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function move() {
    setMoveResult({})
    try {
      await livestockApi.moveAnimals({
        animalIds: [...selected],
        toBatchId: moveTo ? Number(moveTo) : null,
        date: moveDate,
      })
      const target = batches.find((b) => String(b.id) === moveTo)?.name ?? 'sem lote'
      setMoveResult({ success: `${selected.size === 1 ? '1 animal movido' : `${selected.size} animais movidos`} para ${target}.` })
      setSelected(new Set())
      animals.reload()
      onChanged()
    } catch (err) {
      setMoveResult({ error: apiErrorMessage(err) })
    }
  }

  return (
    <div className="space-y-4">
      {canWrite && (
        <section className={cardClass}>
          {creating ? (
            <>
              <h2 className="mb-3 text-sm font-semibold text-terra">Cadastrar animal</h2>
              <AnimalForm
                batches={batches}
                onCancel={() => setCreating(false)}
                onSaved={(animal) => {
                  setCreating(false)
                  setCreated(`Animal ${animal.tag} cadastrado.`)
                  animals.reload()
                  onChanged()
                }}
              />
            </>
          ) : (
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm text-terra/70">
                Nascimentos e animais que já estavam na propriedade. Compras entram pela aba Compras e vendas.
              </p>
              <button
                type="button"
                onClick={() => {
                  setCreating(true)
                  setCreated(null)
                  setMoveResult({})
                }}
                className={`cursor-pointer ${primaryButtonClass}`}
              >
                Cadastrar animal
              </button>
            </div>
          )}
          {created && !creating && (
            <div className="mt-3">
              <FormMessage success={created} />
            </div>
          )}
        </section>
      )}

      <section className={cardClass} aria-labelledby={`${ids}-title`}>
        <h2 id={`${ids}-title`} className="sr-only">
          Animais
        </h2>
        <div className="mb-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <label htmlFor={`${ids}-q`} className={labelClass}>
              Buscar
            </label>
            <input
              id={`${ids}-q`}
              type="search"
              placeholder="Brinco ou nome"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className={inputClass}
            />
          </div>
          <div>
            <label htmlFor={`${ids}-status`} className={labelClass}>
              Situação
            </label>
            <select id={`${ids}-status`} value={status} onChange={(e) => setStatus(e.target.value as AnimalStatus | '')} className={inputClass}>
              <option value="ATIVO">No rebanho</option>
              <option value="VENDIDO">Vendidos</option>
              <option value="MORTO">Mortos</option>
              <option value="DESCARTADO">Descartados</option>
              <option value="">Todos</option>
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-batch`} className={labelClass}>
              Lote
            </label>
            <select id={`${ids}-batch`} value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
              <option value="">Todos</option>
              {individualBatches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-species`} className={labelClass}>
              Espécie
            </label>
            <select id={`${ids}-species`} value={species} onChange={(e) => setSpecies(e.target.value as Species | '')} className={inputClass}>
              <option value="">Todas</option>
              {individualSpecies.map((s) => (
                <option key={s} value={s}>
                  {speciesLabels[s]}
                </option>
              ))}
            </select>
          </div>
        </div>

        {canWrite && selected.size > 0 && (
          <div className="mb-3 space-y-2 rounded-xl border border-rust/30 bg-rust/5 p-3" role="group" aria-label="Ações com os animais selecionados">
            <p className="text-sm font-medium text-terra">
              {selected.size === 1 ? '1 animal selecionado' : `${selected.size} animais selecionados`}
              {selectedAnimals.length < selected.size && ' (alguns em outras páginas)'}
            </p>
            <FormMessage {...moveResult} />
            <div className="flex flex-wrap items-end gap-2">
              <div>
                <label htmlFor={`${ids}-move`} className={labelClass}>
                  Mover para o lote
                </label>
                <select id={`${ids}-move`} value={moveTo} onChange={(e) => setMoveTo(e.target.value)} className={`${inputClass} w-48`}>
                  <option value="">Sem lote</option>
                  {individualBatches
                    .filter((b) => !b.archived)
                    .map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name} ({speciesLabels[b.species].toLowerCase()})
                      </option>
                    ))}
                </select>
              </div>
              <div>
                <label htmlFor={`${ids}-move-date`} className={labelClass}>
                  Data
                </label>
                <input id={`${ids}-move-date`} type="date" max={todayIso()} value={moveDate} onChange={(e) => setMoveDate(e.target.value)} className={`${inputClass} w-40`} />
              </div>
              <button type="button" onClick={move} className={`cursor-pointer ${primaryButtonClass}`}>
                Mover
              </button>
              <button type="button" onClick={() => setSelected(new Set())} className={`cursor-pointer ${outlineButtonClass}`}>
                Limpar seleção
              </button>
            </div>
          </div>
        )}
        {selected.size === 0 && moveResult.success && (
          <div className="mb-3">
            <FormMessage success={moveResult.success} />
          </div>
        )}

        {animals.error && <FormMessage error={animals.error} />}
        <div className="relative overflow-x-auto">
          <table className="w-full min-w-[620px] text-sm">
            <thead>
              <tr className="text-left text-xs text-terra/55">
                {canWrite && (
                  <th className="w-8 pb-2">
                    <input
                      type="checkbox"
                      aria-label="Selecionar todos os animais ativos desta página"
                      checked={allSelected}
                      onChange={() =>
                        setSelected((current) => {
                          const next = new Set(current)
                          selectableIds.forEach((id) => (allSelected ? next.delete(id) : next.add(id)))
                          return next
                        })
                      }
                      className="accent-rust"
                    />
                  </th>
                )}
                <th className="pb-2 font-medium">Brinco</th>
                <th className="pb-2 font-medium">Espécie</th>
                <th className="pb-2 font-medium">Sexo</th>
                <th className="pb-2 font-medium">Idade</th>
                <th className="pb-2 font-medium">Lote</th>
                <th className="pb-2 text-right font-medium">Peso</th>
                <th className="pb-2 font-medium">
                  <span className="sr-only">Situação</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {animals.items.map((a) => (
                <tr key={a.id} className={`border-t border-terra/5 ${a.status !== 'ATIVO' ? 'text-terra/50' : ''}`}>
                  {canWrite && (
                    <td className="py-2">
                      {a.status === 'ATIVO' && (
                        <input
                          type="checkbox"
                          aria-label={`Selecionar ${a.tag}`}
                          checked={selected.has(a.id)}
                          onChange={() => toggle(a.id)}
                          className="accent-rust"
                        />
                      )}
                    </td>
                  )}
                  <td className="py-2 pr-3">
                    <Link to={`/pecuaria/animais/${a.id}`} className="font-medium text-terra hover:text-rust hover:underline">
                      {a.tag}
                    </Link>
                    {a.name && <span className="ml-1 text-terra/55">{a.name}</span>}
                  </td>
                  <td className="py-2 pr-3">
                    {speciesLabels[a.species]}
                    {a.breed && <span className="text-terra/55"> · {a.breed}</span>}
                  </td>
                  <td className="py-2 pr-3">{sexLabels[a.sex]}</td>
                  <td className="py-2 pr-3 whitespace-nowrap">
                    {a.birthDateEstimated ? '~' : ''}
                    {formatAge(a.birthDate)}
                  </td>
                  <td className="py-2 pr-3">{a.batchName ?? '—'}</td>
                  <td className="py-2 text-right tabular-nums">{a.currentWeight != null ? `${formatNumber(a.currentWeight)} kg` : '—'}</td>
                  <td className="py-2 pl-3 text-right text-xs whitespace-nowrap">
                    {a.status !== 'ATIVO' && <span className="rounded-full bg-terra/5 px-2 py-0.5">{statusLabels[a.status]}</span>}
                  </td>
                </tr>
              ))}
              {animals.loading && animals.items.length === 0 && (
                <tr>
                  <td colSpan={8} className="py-6 text-center text-terra/50">
                    Carregando…
                  </td>
                </tr>
              )}
              {!animals.loading && !animals.error && animals.items.length === 0 && (
                <tr>
                  <td colSpan={8} className="py-6 text-center text-terra/50">
                    {q || batchId || species || status !== 'ATIVO'
                      ? 'Nenhum animal com esses filtros.'
                      : 'Nenhum animal no rebanho ainda. Cadastre o primeiro ou registre uma compra.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={animals.page} totalPages={animals.totalPages} totalElements={animals.totalElements} onChange={animals.setPage} />
      </section>
    </div>
  )
}
