import { useEffect, useId, useState, type FormEvent } from 'react'
import { livestockApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type { Animal, AnimalBatch, AnimalOrigin, Sex, SpeciesCatalogItem } from '../../api/types'
import { FormMessage } from '../../components/FormMessage'
import { inputClass, outlineButtonClass, primaryButtonClass } from '../../components/ui'
import { todayIso } from '../../lib/format'
import { sexLabels } from '../../lib/livestock'
import { labelClass } from './shared'

/**
 * Cadastro (nascimento ou outra origem) e edição de um animal. Na edição, lote, entrada e peso não aparecem:
 * eles mudam por eventos (troca de lote, pesagem...). A compra tem fluxo próprio, que lança o pagamento.
 */
export function AnimalForm({
  animal,
  batches,
  species,
  onSaved,
  onCancel,
}: {
  animal?: Animal
  batches: AnimalBatch[]
  species: SpeciesCatalogItem[]
  onSaved: (animal: Animal) => void
  onCancel?: () => void
}) {
  const ids = useId()
  const editing = !!animal
  const [tag, setTag] = useState(animal?.tag ?? '')
  const [name, setName] = useState(animal?.name ?? '')
  const individualOptions = species.filter((s) => s.allowsIndividual && (!s.archived || s.id === animal?.speciesId))
  const [speciesId, setSpeciesId] = useState(animal?.speciesId ? String(animal.speciesId) : String(individualOptions[0]?.id ?? ''))
  const [sex, setSex] = useState<Sex>(animal?.sex ?? 'FEMEA')
  const [breed, setBreed] = useState(animal?.breed ?? '')
  const [birthDate, setBirthDate] = useState(animal?.birthDate ?? '')
  const [estimated, setEstimated] = useState(animal?.birthDateEstimated ?? false)
  const [origin, setOrigin] = useState<AnimalOrigin>('NASCIMENTO')
  const [entryDate, setEntryDate] = useState(todayIso)
  const [batchId, setBatchId] = useState('')
  const [weight, setWeight] = useState('')
  const [motherId, setMotherId] = useState(animal?.motherId ? String(animal.motherId) : '')
  const [sire, setSire] = useState(animal?.sire ?? '')
  const [notes, setNotes] = useState(animal?.notes ?? '')
  const [mothers, setMothers] = useState<Animal[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Possíveis mães: fêmeas da mesma espécie (ativas ou não — a mãe pode já ter saído do rebanho).
  useEffect(() => {
    if (!speciesId) return
    let active = true
    livestockApi
      .listAnimals({ speciesId: Number(speciesId), size: 200 })
      .then((page) => active && setMothers(page.content.filter((a) => a.sex === 'FEMEA' && a.id !== animal?.id)))
      .catch(() => active && setMothers([]))
    return () => {
      active = false
    }
  }, [speciesId, animal?.id])

  const batchOptions = batches.filter((b) => !b.archived && b.tracking === 'INDIVIDUAL' && b.speciesId === Number(speciesId))
  const born = origin === 'NASCIMENTO'

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    const common = {
      tag,
      name: name || null,
      speciesId: Number(speciesId),
      breed: breed || null,
      sex,
      birthDate: birthDate || null,
      birthDateEstimated: estimated,
      motherId: motherId ? Number(motherId) : null,
      sire: sire || null,
      notes: notes || null,
    }
    try {
      const saved = editing
        ? await livestockApi.updateAnimal(animal.id, common)
        : await livestockApi.createAnimal({
            ...common,
            // Nasceu aqui sem data de nascimento preenchida: a data informada é o nascimento.
            birthDate: common.birthDate ?? (born ? entryDate : null),
            origin,
            entryDate: born && birthDate ? birthDate : entryDate,
            batchId: batchId ? Number(batchId) : null,
            weight: weight ? Number(weight) : null,
          })
      onSaved(saved)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage error={error} />
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-tag`} className={labelClass}>
            Brinco
          </label>
          <input id={`${ids}-tag`} required maxLength={30} value={tag} onChange={(e) => setTag(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor={`${ids}-name`} className={labelClass}>
            Nome <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-name`} maxLength={100} value={name} onChange={(e) => setName(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor={`${ids}-species`} className={labelClass}>
            Espécie
          </label>
          <select
            id={`${ids}-species`}
            value={speciesId}
            disabled={editing}
            onChange={(e) => {
              setSpeciesId(e.target.value)
              setBatchId('')
              setMotherId('')
            }}
            className={`${inputClass} disabled:bg-terra/5`}
          >
            {individualOptions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
        <fieldset>
          <legend className={labelClass}>Sexo</legend>
          <div className="grid grid-cols-2 gap-1">
            {(['FEMEA', 'MACHO'] as Sex[]).map((s) => (
              <label
                key={s}
                className={`flex cursor-pointer items-center justify-center rounded-md border px-2 py-2 text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                  sex === s ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
                }`}
              >
                <input type="radio" name={`${ids}-sex`} checked={sex === s} onChange={() => setSex(s)} className="sr-only" />
                {sexLabels[s]}
              </label>
            ))}
          </div>
        </fieldset>
        <div>
          <label htmlFor={`${ids}-breed`} className={labelClass}>
            Raça <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input
            id={`${ids}-breed`}
            maxLength={60}
            placeholder="Ex.: Nelore, Girolando, Santa Inês"
            value={breed}
            onChange={(e) => setBreed(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${ids}-birth`} className={labelClass}>
            Nascimento <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <input id={`${ids}-birth`} type="date" max={todayIso()} value={birthDate} onChange={(e) => setBirthDate(e.target.value)} className={inputClass} />
          {birthDate && (
            <label className="mt-1 flex items-center gap-2 text-xs text-terra/70">
              <input type="checkbox" checked={estimated} onChange={(e) => setEstimated(e.target.checked)} className="accent-rust" />
              Data aproximada
            </label>
          )}
        </div>
      </div>

      {!editing && (
        <div className="grid gap-3 sm:grid-cols-2">
          <fieldset className="sm:col-span-2">
            <legend className={labelClass}>Origem</legend>
            <div className="grid grid-cols-2 gap-1">
              {(
                [
                  ['NASCIMENTO', 'Nasceu aqui'],
                  ['OUTRA', 'Já estava / outra origem'],
                ] as [AnimalOrigin, string][]
              ).map(([value, text]) => (
                <label
                  key={value}
                  className={`flex cursor-pointer items-center justify-center rounded-md border px-2 py-2 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
                    origin === value ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
                  }`}
                >
                  <input type="radio" name={`${ids}-origin`} checked={origin === value} onChange={() => setOrigin(value)} className="sr-only" />
                  {text}
                </label>
              ))}
            </div>
            <p className="mt-1 text-xs text-terra/55">Animais comprados entram pela aba Compras e vendas, que lança o pagamento.</p>
          </fieldset>
          {!(born && birthDate) && (
            <div>
              <label htmlFor={`${ids}-entry`} className={labelClass}>
                {born ? 'Data do nascimento' : 'Data de entrada'}
              </label>
              <input id={`${ids}-entry`} type="date" required max={todayIso()} value={entryDate} onChange={(e) => setEntryDate(e.target.value)} className={inputClass} />
            </div>
          )}
          <div>
            <label htmlFor={`${ids}-batch`} className={labelClass}>
              Lote <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <select id={`${ids}-batch`} value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
              <option value="">Sem lote</option>
              {batchOptions.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-weight`} className={labelClass}>
              Peso (kg) <span className="font-normal text-terra/45">(opcional)</span>
            </label>
            <input id={`${ids}-weight`} type="number" min="0.01" step="0.01" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} className={inputClass} />
          </div>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-mother`} className={labelClass}>
            Mãe <span className="font-normal text-terra/45">(opcional)</span>
          </label>
          <select id={`${ids}-mother`} value={motherId} onChange={(e) => setMotherId(e.target.value)} className={inputClass}>
            <option value="">Não informada</option>
            {mothers.map((m) => (
              <option key={m.id} value={m.id}>
                {m.tag}
                {m.name ? ` — ${m.name}` : ''}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-sire`} className={labelClass}>
            Pai <span className="font-normal text-terra/45">(touro, reprodutor ou sêmen)</span>
          </label>
          <input id={`${ids}-sire`} maxLength={100} value={sire} onChange={(e) => setSire(e.target.value)} className={inputClass} />
        </div>
      </div>
      <div>
        <label htmlFor={`${ids}-notes`} className={labelClass}>
          Observações <span className="font-normal text-terra/45">(opcional)</span>
        </label>
        <textarea id={`${ids}-notes`} maxLength={500} rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClass} />
      </div>
      <div className="flex gap-2">
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Salvando…' : editing ? 'Salvar alterações' : 'Cadastrar animal'}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className={`cursor-pointer ${outlineButtonClass}`}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
