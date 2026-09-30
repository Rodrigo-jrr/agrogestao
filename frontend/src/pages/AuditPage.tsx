import { useEffect, useId, useState } from 'react'
import { auditApi } from '../api/endpoints'
import type { AuditAction, AuditAuthor } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { PeriodFilter, presetPeriod, type Period } from '../components/PeriodFilter'
import { Pagination } from '../components/Pagination'
import { fieldLabelClass, inputClass } from '../components/ui'
import { formatDateTime } from '../lib/format'
import { usePaged } from '../lib/usePaged'

const entityLabels: Record<string, string> = {
  FinancialTransaction: 'Lançamento',
  AccountPayable: 'Conta a pagar',
  AccountReceivable: 'Conta a receber',
  CostCenter: 'Centro de custo',
  Product: 'Produto',
  InventoryMovement: 'Movimentação de estoque',
  Purchase: 'Compra',
  Supplier: 'Fornecedor',
  Sale: 'Venda',
  Customer: 'Cliente',
  ProductionRecord: 'Produção',
  Activity: 'Atividade',
  PropertyMember: 'Membro da equipe',
  PropertyInvitation: 'Convite',
  Property: 'Propriedade',
}

const areas: { key: string; label: string; entities: string[] }[] = [
  { key: 'finance', label: 'Financeiro', entities: ['FinancialTransaction', 'AccountPayable', 'AccountReceivable', 'CostCenter'] },
  { key: 'inventory', label: 'Estoque', entities: ['Product', 'InventoryMovement'] },
  { key: 'purchases', label: 'Compras', entities: ['Purchase', 'Supplier'] },
  { key: 'sales', label: 'Vendas', entities: ['Sale', 'Customer'] },
  { key: 'production', label: 'Produção', entities: ['ProductionRecord'] },
  { key: 'activities', label: 'Atividades', entities: ['Activity'] },
  { key: 'team', label: 'Equipe', entities: ['PropertyMember', 'PropertyInvitation'] },
  { key: 'property', label: 'Propriedade', entities: ['Property'] },
]

const actionLabels: Record<AuditAction, string> = {
  CREATE: 'Criou',
  UPDATE: 'Alterou',
  DELETE: 'Excluiu',
  CANCEL: 'Cancelou',
  VIEW: 'Consultou',
}

/** Verbos mais naturais para algumas áreas ("Adicionou membro da equipe" em vez de "Criou"). */
const actionOverrides: Record<string, Partial<Record<AuditAction, string>>> = {
  PropertyMember: { CREATE: 'Adicionou', DELETE: 'Removeu' },
  PropertyInvitation: { CREATE: 'Enviou', DELETE: 'Cancelou' },
}

function actionText(entityName: string, action: AuditAction) {
  return actionOverrides[entityName]?.[action] ?? actionLabels[action]
}

function entityText(entityName: string) {
  return (entityLabels[entityName] ?? entityName).toLowerCase()
}

const actionTone: Record<AuditAction, string> = {
  CREATE: 'text-mata',
  UPDATE: 'text-terra',
  DELETE: 'text-rust',
  CANCEL: 'text-rust',
  VIEW: 'text-terra/60',
}

export function AuditPage() {
  const { currentProperty } = useProperty()
  const id = useId()
  const [period, setPeriod] = useState<Period>(() => presetPeriod('thisMonth'))
  const [authorId, setAuthorId] = useState('')
  const [area, setArea] = useState('')
  const [authors, setAuthors] = useState<AuditAuthor[]>([])

  const isOwner = currentProperty?.role === 'OWNER'

  useEffect(() => {
    if (!isOwner) return
    auditApi.authors().then(setAuthors).catch(() => setAuthors([]))
  }, [isOwner, currentProperty?.id])

  const entities = areas.find((a) => a.key === area)?.entities
  const history = usePaged(
    (page) =>
      auditApi.search({
        from: period.from,
        to: period.to,
        userId: authorId ? Number(authorId) : undefined,
        entity: entities,
        page,
      }),
    [period.from, period.to, authorId, area, currentProperty?.id],
    isOwner,
  )

  if (!isOwner) {
    return (
      <div className="space-y-4">
        <h1 className="font-serif text-2xl font-semibold text-terra">Histórico de alterações</h1>
        <p className="rounded-2xl border border-terra/10 bg-white p-4 text-sm text-terra/60 shadow-sm">
          Apenas proprietários veem o histórico de alterações da propriedade.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="font-serif text-2xl font-semibold text-terra">Histórico de alterações</h1>
        <p className="mt-1 text-sm text-terra/60">Quem criou, alterou, cancelou ou excluiu cada registro.</p>
      </div>

      <div className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        <PeriodFilter value={period} onChange={setPeriod} />
        <div className="grid gap-3 sm:grid-cols-2 md:max-w-xl">
          <div>
            <label htmlFor={`${id}-author`} className={fieldLabelClass}>
              Pessoa
            </label>
            <select id={`${id}-author`} value={authorId} onChange={(e) => setAuthorId(e.target.value)} className={inputClass}>
              <option value="">Todas</option>
              {authors.map((author) => (
                <option key={author.userId} value={author.userId}>
                  {author.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor={`${id}-area`} className={fieldLabelClass}>
              Área
            </label>
            <select id={`${id}-area`} value={area} onChange={(e) => setArea(e.target.value)} className={inputClass}>
              <option value="">Todas</option>
              {areas.map((a) => (
                <option key={a.key} value={a.key}>
                  {a.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
        {history.error && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{history.error}</p>}
        {/* Celular: lista empilhada, para os detalhes não ficarem escondidos numa tabela que rola de lado. */}
        <ul className="divide-y divide-terra/5 text-sm md:hidden">
          {history.items.map((entry) => (
            <li key={entry.id} className="py-3 first:pt-0">
              <p className="text-xs text-terra/55 tabular-nums">
                {formatDateTime(entry.createdAt)} · {entry.userName ?? '—'}
              </p>
              <p className="mt-0.5">
                <span className={`font-medium ${actionTone[entry.action]}`}>{actionText(entry.entityName, entry.action)}</span>{' '}
                <span className="text-terra/70">{entityText(entry.entityName)}</span>
              </p>
              {entry.details && <p className="mt-0.5 break-words text-terra/70">{entry.details}</p>}
            </li>
          ))}
          {history.loading && history.items.length === 0 && <li className="py-6 text-center text-terra/50">Carregando…</li>}
          {!history.loading && !history.error && history.items.length === 0 && (
            <li className="py-6 text-center text-terra/50">Nenhuma alteração neste período com esses filtros.</li>
          )}
        </ul>

        <div className="relative hidden overflow-x-auto md:block">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-terra/50">
                <th className="pb-2 pr-3 font-medium">Quando</th>
                <th className="pb-2 pr-3 font-medium">Quem</th>
                <th className="pb-2 pr-3 font-medium">O quê</th>
                <th className="pb-2 font-medium">Detalhes</th>
              </tr>
            </thead>
            <tbody>
              {history.items.map((entry) => (
                <tr key={entry.id} className="border-t border-terra/5 align-top">
                  <td className="py-2 pr-3 whitespace-nowrap text-terra/60 tabular-nums">{formatDateTime(entry.createdAt)}</td>
                  <td className="py-2 pr-3 whitespace-nowrap text-terra">{entry.userName ?? '—'}</td>
                  <td className="py-2 pr-3 whitespace-nowrap">
                    <span className={`font-medium ${actionTone[entry.action]}`}>{actionText(entry.entityName, entry.action)}</span>{' '}
                    <span className="text-terra/70">{entityText(entry.entityName)}</span>
                  </td>
                  <td className="min-w-48 py-2 text-terra/70">{entry.details || '—'}</td>
                </tr>
              ))}
              {history.loading && history.items.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-terra/50">
                    Carregando…
                  </td>
                </tr>
              )}
              {!history.loading && !history.error && history.items.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-terra/50">
                    Nenhuma alteração neste período com esses filtros.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        <Pagination
          page={history.page}
          totalPages={history.totalPages}
          totalElements={history.totalElements}
          onChange={history.setPage}
        />
      </div>
    </div>
  )
}
