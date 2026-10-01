import { Fragment, useEffect, useId, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { livestockApi, purchasesApi, salesApi } from '../../api/endpoints'
import { apiErrorMessage } from '../../api/client'
import type {
  Animal,
  AnimalBatch,
  InstallmentInput,
  LivestockTrade,
  LivestockTradeDetail,
  PaymentType,
  Sex,
  Species,
} from '../../api/types'
import { useConfirm } from '../../components/ConfirmDialog'
import { FormMessage } from '../../components/FormMessage'
import { installmentsSum } from '../../components/InstallmentsEditor'
import { Pagination } from '../../components/Pagination'
import { inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../../components/ui'
import { formatCurrency, formatDate, formatNumber, todayIso } from '../../lib/format'
import { individualSpecies, parseTagList, sexLabels, speciesLabels } from '../../lib/livestock'
import { usePaged } from '../../lib/usePaged'
import { AnimalPicker, PartySelect, PaymentFields, cardClass, labelClass } from './shared'
import { useSelection } from './useSelection'

type Mode = 'individual' | 'aggregate'
type Party = { id: number; name: string }

function ModeToggle({ mode, onChange, name }: { mode: Mode; onChange: (mode: Mode) => void; name: string }) {
  return (
    <fieldset>
      <legend className={labelClass}>Animais</legend>
      <div className="grid grid-cols-2 gap-1">
        {(
          [
            ['individual', 'Com brinco'],
            ['aggregate', 'Só a quantidade (lote)'],
          ] as [Mode, string][]
        ).map(([value, text]) => (
          <label
            key={value}
            className={`flex cursor-pointer items-center justify-center rounded-md border px-2 py-1.5 text-center text-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-rust ${
              mode === value ? 'border-rust bg-rust/10 font-medium text-rust' : 'border-terra/15 text-terra/70'
            }`}
          >
            <input type="radio" name={name} checked={mode === value} onChange={() => onChange(value)} className="sr-only" />
            {text}
          </label>
        ))}
      </div>
    </fieldset>
  )
}

function PurchaseForm({ batches, onSaved }: { batches: AnimalBatch[]; onSaved: () => void }) {
  const ids = useId()
  const [suppliers, setSuppliers] = useState<Party[]>([])
  const [supplierId, setSupplierId] = useState('')
  const [date, setDate] = useState(todayIso)
  const [mode, setMode] = useState<Mode>('individual')
  const [species, setSpecies] = useState<Species>('BOVINO')
  const [batchId, setBatchId] = useState('')
  const [sex, setSex] = useState<Sex>('MACHO')
  const [breed, setBreed] = useState('')
  const [tags, setTags] = useState('')
  const [headCount, setHeadCount] = useState('')
  const [total, setTotal] = useState('')
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [installments, setInstallments] = useState<InstallmentInput[]>([])
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  useEffect(() => {
    purchasesApi.listSuppliers().then(setSuppliers).catch(() => setSuppliers([]))
  }, [])

  const parsed = useMemo(() => parseTagList(tags), [tags])
  const totalValue = Number(total) || 0
  const batchOptions = batches.filter(
    (b) => !b.archived && (mode === 'aggregate' ? b.tracking === 'AGGREGATE' : b.tracking === 'INDIVIDUAL' && b.species === species),
  )
  const heads = mode === 'individual' ? parsed.length : Number(headCount) || 0

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    if (mode === 'individual' && parsed.length === 0) {
      setResult({ error: 'Informe os brincos dos animais comprados, um por linha.' })
      return
    }
    if (paymentType === 'INSTALLMENTS' && installmentsSum(installments) !== totalValue) {
      setResult({ error: 'A soma das parcelas precisa ser igual ao total.' })
      return
    }
    setSaving(true)
    try {
      const trade = await livestockApi.purchase({
        supplierId: Number(supplierId),
        tradeDate: date,
        totalAmount: totalValue,
        paymentType,
        installments: paymentType === 'INSTALLMENTS' ? installments : undefined,
        batchId: batchId ? Number(batchId) : null,
        ...(mode === 'individual'
          ? { species, animals: parsed.map((row) => ({ tag: row.tag, name: row.name, sex, breed: breed || null })) }
          : { headCount: Number(headCount) }),
      })
      setResult({ success: `Compra de ${trade.headCount === 1 ? '1 cabeça' : `${trade.headCount} cabeças`} registrada.` })
      setTags('')
      setHeadCount('')
      setTotal('')
      setPaymentType('CASH')
      onSaved()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage {...result} />
      <div className="grid gap-3 sm:grid-cols-2">
        <PartySelect
          label="Fornecedor"
          parties={suppliers}
          value={supplierId}
          onChange={setSupplierId}
          onCreate={async (name) => {
            const created = await purchasesApi.createSupplier({ name })
            setSuppliers((list) => [...list, created])
            return created
          }}
        />
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            Data da compra
          </label>
          <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </div>
      </div>
      <ModeToggle mode={mode} name={`${ids}-mode`} onChange={(m) => {
        setMode(m)
        setBatchId('')
      }} />

      {mode === 'individual' ? (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            <div>
              <label htmlFor={`${ids}-species`} className={labelClass}>
                Espécie
              </label>
              <select id={`${ids}-species`} value={species} onChange={(e) => {
                setSpecies(e.target.value as Species)
                setBatchId('')
              }} className={inputClass}>
                {individualSpecies.map((s) => (
                  <option key={s} value={s}>
                    {speciesLabels[s]}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor={`${ids}-sex`} className={labelClass}>
                Sexo
              </label>
              <select id={`${ids}-sex`} value={sex} onChange={(e) => setSex(e.target.value as Sex)} className={inputClass}>
                {(['MACHO', 'FEMEA'] as Sex[]).map((s) => (
                  <option key={s} value={s}>
                    {sexLabels[s]}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor={`${ids}-breed`} className={labelClass}>
                Raça <span className="font-normal text-terra/45">(opcional)</span>
              </label>
              <input id={`${ids}-breed`} maxLength={60} value={breed} onChange={(e) => setBreed(e.target.value)} className={inputClass} />
            </div>
          </div>
          <div>
            <label htmlFor={`${ids}-tags`} className={labelClass}>
              Brincos, um por linha <span className="font-normal text-terra/45">(pode colar da planilha; "brinco; nome")</span>
            </label>
            <textarea
              id={`${ids}-tags`}
              rows={4}
              value={tags}
              onChange={(e) => setTags(e.target.value)}
              aria-describedby={`${ids}-tags-count`}
              placeholder={'301\n302; Estrela\n303'}
              className={`${inputClass} font-mono`}
            />
            <p id={`${ids}-tags-count`} className="mt-1 text-xs text-terra/55">
              {parsed.length === 0 ? 'Nenhum brinco ainda.' : `${parsed.length === 1 ? '1 animal' : `${parsed.length} animais`}.`} Sexo e raça valem para todos; dá
              para ajustar cada um depois na ficha.
            </p>
          </div>
        </>
      ) : (
        <div>
          <label htmlFor={`${ids}-heads`} className={labelClass}>
            Cabeças
          </label>
          <input id={`${ids}-heads`} required type="number" min="1" step="1" value={headCount} onChange={(e) => setHeadCount(e.target.value)} className={inputClass} />
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor={`${ids}-batch`} className={labelClass}>
            {mode === 'individual' ? (
              <>
                Lote <span className="font-normal text-terra/45">(opcional)</span>
              </>
            ) : (
              'Lote que recebe'
            )}
          </label>
          <select id={`${ids}-batch`} required={mode === 'aggregate'} value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
            <option value="">{mode === 'individual' ? 'Sem lote' : 'Selecione'}</option>
            {batchOptions.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
                {mode === 'aggregate' ? ` (${speciesLabels[b.species].toLowerCase()})` : ''}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor={`${ids}-total`} className={labelClass}>
            Valor total (R$)
          </label>
          <input id={`${ids}-total`} required type="number" min="0.01" step="0.01" inputMode="decimal" value={total} onChange={(e) => setTotal(e.target.value)} className={inputClass} />
          {heads > 1 && totalValue > 0 && (
            <p className="mt-1 text-xs text-terra/55">{formatCurrency(totalValue / heads)} por cabeça</p>
          )}
        </div>
      </div>
      <PaymentFields kind="purchase" total={totalValue} date={date} paymentType={paymentType} onPaymentType={setPaymentType} installments={installments} onInstallments={setInstallments} />
      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Registrando…' : 'Registrar compra'}
      </button>
    </form>
  )
}

function SaleForm({ batches, onSaved }: { batches: AnimalBatch[]; onSaved: () => void }) {
  const ids = useId()
  const [customers, setCustomers] = useState<Party[]>([])
  const [customerId, setCustomerId] = useState('')
  const [date, setDate] = useState(todayIso)
  const [mode, setMode] = useState<Mode>('individual')
  const [active, setActive] = useState<Animal[]>([])
  const selection = useSelection()
  const [batchId, setBatchId] = useState('')
  const [headCount, setHeadCount] = useState('')
  const [total, setTotal] = useState('')
  const [paymentType, setPaymentType] = useState<PaymentType>('CASH')
  const [installments, setInstallments] = useState<InstallmentInput[]>([])
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})
  const [version, setVersion] = useState(0)

  useEffect(() => {
    salesApi.listCustomers().then(setCustomers).catch(() => setCustomers([]))
  }, [])
  useEffect(() => {
    livestockApi
      .listAnimals({ status: 'ATIVO', size: 200 })
      .then((page) => setActive(page.content))
      .catch(() => setActive([]))
  }, [version])

  const totalValue = Number(total) || 0
  const aggregateBatches = batches.filter((b) => !b.archived && b.tracking === 'AGGREGATE' && b.headCount > 0)
  const heads = mode === 'individual' ? selection.chosen.size : Number(headCount) || 0

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setResult({})
    if (mode === 'individual' && selection.chosen.size === 0) {
      setResult({ error: 'Marque os animais vendidos.' })
      return
    }
    if (paymentType === 'INSTALLMENTS' && installmentsSum(installments) !== totalValue) {
      setResult({ error: 'A soma das parcelas precisa ser igual ao total.' })
      return
    }
    setSaving(true)
    try {
      const trade = await livestockApi.sale({
        customerId: Number(customerId),
        tradeDate: date,
        totalAmount: totalValue,
        paymentType,
        installments: paymentType === 'INSTALLMENTS' ? installments : undefined,
        ...(mode === 'individual' ? { animalIds: [...selection.chosen] } : { batchId: Number(batchId), headCount: Number(headCount) }),
      })
      setResult({ success: `Venda de ${trade.headCount === 1 ? '1 cabeça' : `${trade.headCount} cabeças`} registrada.` })
      selection.clear()
      setHeadCount('')
      setTotal('')
      setPaymentType('CASH')
      setVersion((v) => v + 1)
      onSaved()
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <FormMessage {...result} />
      <div className="grid gap-3 sm:grid-cols-2">
        <PartySelect
          label="Cliente"
          parties={customers}
          value={customerId}
          onChange={setCustomerId}
          onCreate={async (name) => {
            const created = await salesApi.createCustomer({ name })
            setCustomers((list) => [...list, created])
            return created
          }}
        />
        <div>
          <label htmlFor={`${ids}-date`} className={labelClass}>
            Data da venda
          </label>
          <input id={`${ids}-date`} type="date" required max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} className={inputClass} />
        </div>
      </div>
      <ModeToggle mode={mode} name={`${ids}-mode`} onChange={setMode} />

      {mode === 'individual' ? (
        <>
          <AnimalPicker legend="Animais vendidos" animals={active} chosen={selection.chosen} onToggle={selection.toggle} />
          {active.some((a) => selection.chosen.has(a.id) && a.withdrawalUntil) && (
            <p role="status" className="rounded-md bg-trigo/15 px-3 py-2 text-xs text-terra">
              Há animais marcados em carência: não podem ir para abate até o fim do prazo.
            </p>
          )}
        </>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor={`${ids}-batch`} className={labelClass}>
              Lote
            </label>
            <select id={`${ids}-batch`} required value={batchId} onChange={(e) => setBatchId(e.target.value)} className={inputClass}>
              <option value="">Selecione</option>
              {aggregateBatches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({formatNumber(b.headCount)} cabeças)
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${ids}-heads`} className={labelClass}>
              Cabeças vendidas
            </label>
            <input id={`${ids}-heads`} required type="number" min="1" step="1" value={headCount} onChange={(e) => setHeadCount(e.target.value)} className={inputClass} />
          </div>
        </div>
      )}

      <div>
        <label htmlFor={`${ids}-total`} className={labelClass}>
          Valor total (R$)
        </label>
        <input id={`${ids}-total`} required type="number" min="0.01" step="0.01" inputMode="decimal" value={total} onChange={(e) => setTotal(e.target.value)} className={inputClass} />
        {heads > 1 && totalValue > 0 && <p className="mt-1 text-xs text-terra/55">{formatCurrency(totalValue / heads)} por cabeça</p>}
      </div>
      <PaymentFields kind="sale" total={totalValue} date={date} paymentType={paymentType} onPaymentType={setPaymentType} installments={installments} onInstallments={setInstallments} />
      <button disabled={saving} className={`w-full ${primaryButtonClass}`}>
        {saving ? 'Registrando…' : 'Registrar venda'}
      </button>
    </form>
  )
}

function TradeDetail({ id }: { id: number }) {
  const [detail, setDetail] = useState<LivestockTradeDetail | null>(null)
  useEffect(() => {
    livestockApi.trade(id).then(setDetail).catch(() => setDetail(null))
  }, [id])
  if (!detail) return <p className="text-xs text-terra/50">Carregando…</p>
  return (
    <div className="space-y-2 text-sm">
      {detail.animals.length > 0 && (
        <p className="text-terra/70">
          Animais:{' '}
          {detail.animals.map((a, i) => (
            <span key={a.id}>
              {i > 0 && ', '}
              <Link to={`/pecuaria/animais/${a.id}`} className="text-rust hover:underline">
                {a.tag}
              </Link>
            </span>
          ))}
        </p>
      )}
      {detail.installments.length > 0 && (
        <ul className="text-xs text-terra/70">
          {detail.installments.map((i) => (
            <li key={i.accountId}>
              Parcela {i.number}/{i.count}: {formatCurrency(i.amount)} em {formatDate(i.dueDate)} — {i.settled ? 'baixada' : 'pendente'}
            </li>
          ))}
        </ul>
      )}
      {detail.trade.notes && <p className="text-xs text-terra/60">{detail.trade.notes}</p>}
    </div>
  )
}

export function TradesTab({
  batches,
  canWrite,
  canManage,
  onChanged,
}: {
  batches: AnimalBatch[]
  canWrite: boolean
  canManage: boolean
  onChanged: () => void
}) {
  const confirm = useConfirm()
  const [kind, setKind] = useState<'purchase' | 'sale'>('purchase')
  const [openId, setOpenId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const trades = usePaged((page) => livestockApi.listTrades({ page }), [])

  function saved() {
    trades.reload()
    onChanged()
  }

  async function cancel(trade: LivestockTrade) {
    const purchase = trade.kind === 'PURCHASE'
    const ok = await confirm({
      title: `Cancelar a ${purchase ? 'compra' : 'venda'} de ${formatCurrency(trade.totalAmount)} (${formatDate(trade.tradeDate)})?`,
      message: purchase
        ? 'Os animais comprados saem do rebanho (só é possível se ainda não tiveram outras movimentações) e o pagamento é removido do financeiro.'
        : 'Os animais voltam para o rebanho e o recebimento é removido do financeiro. Só é possível se nenhuma parcela foi recebida.',
      confirmLabel: purchase ? 'Cancelar compra' : 'Cancelar venda',
    })
    if (!ok) return
    setError(null)
    try {
      await livestockApi.cancelTrade(trade.id)
      saved()
    } catch (err) {
      setError(apiErrorMessage(err))
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-5">
      {canWrite && (
        <section className={`${cardClass} min-w-0 self-start lg:col-span-2`}>
          <div role="tablist" aria-label="Tipo de operação" className="mb-3 grid grid-cols-2 gap-1 rounded-lg bg-terra/5 p-1">
            {(
              [
                ['purchase', 'Comprar animais'],
                ['sale', 'Vender animais'],
              ] as ['purchase' | 'sale', string][]
            ).map(([value, text]) => (
              <button
                key={value}
                type="button"
                role="tab"
                aria-selected={kind === value}
                onClick={() => setKind(value)}
                className={`cursor-pointer rounded-md px-2 py-1.5 text-sm font-medium focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust ${
                  kind === value ? 'bg-white text-rust shadow-sm' : 'text-terra/60'
                }`}
              >
                {text}
              </button>
            ))}
          </div>
          {kind === 'purchase' ? <PurchaseForm batches={batches} onSaved={saved} /> : <SaleForm batches={batches} onSaved={saved} />}
        </section>
      )}
      <section className={`${cardClass} min-w-0 ${canWrite ? 'lg:col-span-3' : 'lg:col-span-5'}`}>
        <h2 className="mb-3 text-sm font-semibold text-terra">Histórico de compras e vendas de animais</h2>
        <FormMessage error={error ?? trades.error} />
        <div className="relative overflow-x-auto">
          <table className="w-full min-w-[560px] text-sm">
            <thead>
              <tr className="text-left text-xs text-terra/55">
                <th className="pb-2 font-medium">Data</th>
                <th className="pb-2 font-medium">Operação</th>
                <th className="pb-2 text-right font-medium">Cabeças</th>
                <th className="pb-2 text-right font-medium">Total</th>
                <th className="pb-2">
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {trades.items.map((t) => (
                <Fragment key={t.id}>
                  <tr className={`border-t border-terra/5 align-top ${t.canceled ? 'text-terra/45' : ''}`}>
                    <td className="py-2 pr-3 whitespace-nowrap">{formatDate(t.tradeDate)}</td>
                    <td className="py-2 pr-3">
                      <span className={`font-medium ${t.kind === 'SALE' ? 'text-mata' : 'text-terra'}`}>
                        {t.kind === 'PURCHASE' ? 'Compra' : 'Venda'}
                      </span>{' '}
                      {t.kind === 'PURCHASE' ? 'de' : 'para'} {t.partyName}
                      {t.batchName && <span className="text-terra/55"> · {t.batchName}</span>}
                      <span className="block text-xs text-terra/55">
                        {t.paymentType === 'INSTALLMENTS' ? `a prazo, ${t.installmentCount}x` : 'à vista'}
                        {t.canceled && ' · cancelada'}
                      </span>
                    </td>
                    <td className="py-2 text-right tabular-nums">{formatNumber(t.headCount)}</td>
                    <td className="py-2 text-right tabular-nums whitespace-nowrap">{formatCurrency(t.totalAmount)}</td>
                    <td className="py-2 pl-3 text-right whitespace-nowrap">
                      <button
                        onClick={() => setOpenId(openId === t.id ? null : t.id)}
                        aria-expanded={openId === t.id}
                        aria-label={`Detalhes da ${t.kind === 'PURCHASE' ? 'compra' : 'venda'} de ${formatDate(t.tradeDate)}`}
                        className={`cursor-pointer ${linkNeutralClass}`}
                      >
                        Detalhes
                      </button>
                      {canManage && !t.canceled && (
                        <button
                          onClick={() => cancel(t)}
                          aria-label={`Cancelar ${t.kind === 'PURCHASE' ? 'compra' : 'venda'} de ${formatDate(t.tradeDate)}`}
                          className={`ml-3 cursor-pointer ${linkDangerClass}`}
                        >
                          Cancelar
                        </button>
                      )}
                    </td>
                  </tr>
                  {openId === t.id && (
                    <tr className="bg-terreiro/40">
                      <td colSpan={5} className="px-3 py-2">
                        <TradeDetail id={t.id} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
              {trades.loading && trades.items.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-6 text-center text-terra/50">
                    Carregando…
                  </td>
                </tr>
              )}
              {!trades.loading && !trades.error && trades.items.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-6 text-center text-terra/50">
                    Nenhuma compra ou venda de animais ainda.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={trades.page} totalPages={trades.totalPages} totalElements={trades.totalElements} onChange={trades.setPage} />
      </section>
    </div>
  )
}
