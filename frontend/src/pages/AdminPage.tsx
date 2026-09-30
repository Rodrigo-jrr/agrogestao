import { useCallback, useEffect, useId, useMemo, useState, type ReactNode } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { adminApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { AdminOrganizationRow, AdminOverview, AdminUserRow } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Logo'
import { inputClass, outlineButtonClass } from '../components/ui'

type Tab = 'organizations' | 'users'

const ACTIVE_WINDOW_DAYS = 30
const focusRing = 'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust focus-visible:ring-offset-2'

const dateFormatter = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeStyle: 'short' })
const relativeFormatter = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' })

function formatDate(iso: string) {
  return dateFormatter.format(new Date(iso)).replace('.', '')
}

function daysSince(iso: string) {
  return Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000)
}

function normalize(text: string | null | undefined) {
  return (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

/** Última atividade em texto relativo; status comunicado por texto, não só por cor. */
function Activity({ at }: { at: string | null }) {
  if (!at) return <span className="text-terra/40">Sem atividade</span>
  const days = daysSince(at)
  const label =
    days === 0 ? 'hoje' : days < 30 ? relativeFormatter.format(-days, 'day') : relativeFormatter.format(-Math.round(days / 30), 'month')
  const stale = days > ACTIVE_WINDOW_DAYS
  return (
    <time dateTime={at} title={dateTimeFormatter.format(new Date(at))} className={stale ? 'text-terra/45' : 'text-terra'}>
      {!stale && <span aria-hidden="true" className="mr-1.5 inline-block h-1.5 w-1.5 rounded-full bg-mata align-middle" />}
      {label}
    </time>
  )
}

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 16 16"
      className={`h-4 w-4 shrink-0 text-terra/50 motion-safe:transition-transform motion-safe:duration-150 ${open ? 'rotate-90' : ''}`}
    >
      <path d="M6 3.5 10.5 8 6 12.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function AdminPage() {
  const { user, logout } = useAuth()
  const [data, setData] = useState<AdminOverview | null>(null)
  const [loadedAt, setLoadedAt] = useState<Date | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [forbidden, setForbidden] = useState(false)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState<Tab>('organizations')
  const [query, setQuery] = useState('')
  const searchId = useId()

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminApi
      .overview()
      .then((overview) => {
        setData(overview)
        setLoadedAt(new Date())
      })
      .catch((err) => {
        if (err?.response?.status === 403) setForbidden(true)
        else setError(apiErrorMessage(err))
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const term = normalize(query.trim())
  const organizations = useMemo(
    () =>
      (data?.organizations ?? []).filter(
        (org) =>
          !term ||
          [org.name, org.ownerName, org.ownerEmail, ...org.properties.flatMap((p) => [p.name, p.location])].some((field) =>
            normalize(field).includes(term),
          ),
      ),
    [data, term],
  )
  const users = useMemo(
    () => (data?.users ?? []).filter((u) => !term || normalize(u.name).includes(term) || normalize(u.email).includes(term)),
    [data, term],
  )

  // O servidor é quem decide; aqui só evitamos mostrar a tela para quem claramente não tem acesso.
  if (forbidden) return <Navigate to="/dashboard" replace />

  return (
    <div className="min-h-screen bg-terreiro font-sans text-terra">
      <header className="bg-mata-dark text-terreiro">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
          <Logo size={22} variant="light" />
          <span className="rounded-full border border-trigo/60 px-2.5 py-0.5 text-xs font-medium text-trigo">
            Administração da plataforma
          </span>
          <div className="ml-auto flex items-center gap-3 text-sm">
            <span className="hidden text-terreiro/60 sm:inline">{user?.email}</span>
            <Link to="/dashboard" className={`rounded-md px-2 py-1 text-terreiro/80 hover:text-terreiro hover:underline ${focusRing}`}>
              Voltar ao app
            </Link>
            <button
              onClick={logout}
              className={`cursor-pointer rounded-md border border-terreiro/25 px-3 py-1.5 text-terreiro hover:bg-white/10 ${focusRing}`}
            >
              Sair
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-6 px-4 py-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="font-serif text-3xl font-semibold">Visão da plataforma</h1>
            <p className="mt-1 text-sm text-terra/55">
              Somente leitura. Cada acesso a esta página fica registrado na auditoria.
              {loadedAt && <> Dados de {loadedAt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}.</>}
            </p>
          </div>
          <button onClick={load} disabled={loading} className={`cursor-pointer ${outlineButtonClass} ${focusRing} disabled:opacity-50`}>
            {loading ? 'Atualizando…' : 'Atualizar dados'}
          </button>
        </div>

        {error && (
          <div role="alert" className="flex flex-wrap items-center gap-3 rounded-md bg-rust/10 px-4 py-3 text-sm text-rust">
            <span>Não foi possível carregar os dados: {error}</span>
            <button onClick={load} className={`cursor-pointer font-medium underline ${focusRing}`}>
              Tentar de novo
            </button>
          </div>
        )}

        {loading && !data && <SummarySkeleton />}

        {data && (
          <>
            <SummaryStrip summary={data.summary} />

            <section className="rounded-xl border border-terra/10 bg-white">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-terra/10 px-4 pt-3">
                <div role="tablist" aria-label="Tipo de registro" className="flex gap-1">
                  <TabButton active={tab === 'organizations'} onClick={() => setTab('organizations')} controls="admin-panel">
                    Organizações <Count value={data.organizations.length} />
                  </TabButton>
                  <TabButton active={tab === 'users'} onClick={() => setTab('users')} controls="admin-panel">
                    Usuários <Count value={data.users.length} />
                  </TabButton>
                </div>
                <div className="mb-3 flex w-full items-center gap-2 sm:w-80">
                  <label htmlFor={searchId} className="text-sm text-terra/60">
                    Buscar
                  </label>
                  <input
                    id={searchId}
                    type="search"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder={tab === 'organizations' ? 'Nome, e-mail ou propriedade' : 'Nome ou e-mail'}
                    className={`${inputClass} py-1.5`}
                  />
                </div>
              </div>

              <div id="admin-panel" role="tabpanel" className="relative overflow-x-auto">
                {tab === 'organizations' ? (
                  <OrganizationsTable rows={organizations} query={query} />
                ) : (
                  <UsersTable rows={users} query={query} />
                )}
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  )
}

function SummaryStrip({ summary }: { summary: AdminOverview['summary'] }) {
  const items = [
    { value: summary.organizations, label: 'organizações' },
    { value: summary.properties, label: 'propriedades' },
    { value: summary.activePropertiesLast30Days, label: `propriedades com uso nos últimos ${ACTIVE_WINDOW_DAYS} dias` },
    { value: summary.users, label: 'usuários' },
    { value: summary.newUsersLast30Days, label: `cadastros nos últimos ${ACTIVE_WINDOW_DAYS} dias` },
  ]
  return (
    <dl className="grid grid-cols-2 overflow-hidden rounded-xl border border-terra/10 bg-white sm:grid-cols-5 sm:divide-x sm:divide-terra/10">
      {items.map((item) => (
        <div key={item.label} className="flex flex-col gap-1 border-b border-terra/10 px-4 py-3 last:border-b-0 sm:border-b-0">
          <dt className="order-2 text-xs leading-snug text-terra/55">{item.label}</dt>
          <dd className="order-1 font-serif text-3xl font-semibold leading-none tabular-nums">{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}

function SummarySkeleton() {
  return <div aria-hidden="true" className="h-[88px] rounded-xl border border-terra/10 bg-white motion-safe:animate-pulse" />
}

function Count({ value }: { value: number }) {
  return <span className="ml-1 rounded-full bg-terra/5 px-1.5 text-xs tabular-nums text-terra/60">{value}</span>
}

function TabButton({
  active,
  onClick,
  controls,
  children,
}: {
  active: boolean
  onClick: () => void
  controls: string
  children: ReactNode
}) {
  return (
    <button
      role="tab"
      aria-selected={active}
      aria-controls={controls}
      onClick={onClick}
      className={`-mb-px cursor-pointer border-b-2 px-3 py-2 text-sm font-medium ${focusRing} ${
        active ? 'border-rust text-rust' : 'border-transparent text-terra/55 hover:text-terra'
      }`}
    >
      {children}
    </button>
  )
}

function EmptyRow({ colSpan, query, hint }: { colSpan: number; query: string; hint: string }) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-4 py-8 text-center text-sm text-terra/55">
        {query.trim() ? (
          <>
            Nada encontrado para “{query.trim()}”. {hint}
          </>
        ) : (
          'Nenhum registro ainda.'
        )}
      </td>
    </tr>
  )
}

const th = 'px-4 py-2 text-left text-xs font-medium text-terra/55'
const td = 'px-4 py-2.5 align-top'

function OrganizationsTable({ rows, query }: { rows: AdminOrganizationRow[]; query: string }) {
  const [open, setOpen] = useState<Set<number>>(new Set())
  const toggle = (id: number) =>
    setOpen((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  return (
    <table className="w-full min-w-[720px] text-sm">
      <thead className="border-b border-terra/10 bg-terreiro/40">
        <tr>
          <th className={th}>Organização</th>
          <th className={th}>Dono</th>
          <th className={`${th} text-right`}>Propriedades</th>
          <th className={th}>Criada em</th>
          <th className={th}>Última atividade</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((org) => (
          <OrganizationRows key={org.id} org={org} expanded={open.has(org.id)} onToggle={() => toggle(org.id)} />
        ))}
        {rows.length === 0 && <EmptyRow colSpan={5} query={query} hint="Tente o e-mail do dono ou o nome da propriedade." />}
      </tbody>
    </table>
  )
}

function OrganizationRows({ org, expanded, onToggle }: { org: AdminOrganizationRow; expanded: boolean; onToggle: () => void }) {
  const detailsId = `org-${org.id}-properties`
  return (
    <>
      <tr className="border-t border-terra/5 hover:bg-terreiro/40">
        <td className={td}>
          <button
            onClick={onToggle}
            aria-expanded={expanded}
            aria-controls={detailsId}
            className={`-mx-1 flex cursor-pointer items-center gap-1.5 rounded px-1 text-left font-medium ${focusRing}`}
          >
            <Chevron open={expanded} />
            {org.name}
          </button>
        </td>
        <td className={td}>
          {org.ownerEmail ? (
            <>
              <div>{org.ownerName}</div>
              <div className="text-xs text-terra/55">{org.ownerEmail}</div>
            </>
          ) : (
            <span className="text-terra/40">Sem dono</span>
          )}
        </td>
        <td className={`${td} text-right tabular-nums`}>{org.properties.length}</td>
        <td className={`${td} whitespace-nowrap text-terra/70`}>{formatDate(org.createdAt)}</td>
        <td className={`${td} whitespace-nowrap`}>
          <Activity at={org.lastActivityAt} />
        </td>
      </tr>
      {expanded && (
        <tr id={detailsId} className="bg-terreiro/50">
          <td colSpan={5} className="px-4 pb-3 pt-1">
            {org.properties.length === 0 ? (
              <p className="pl-6 text-sm text-terra/50">Organização sem propriedades cadastradas.</p>
            ) : (
              <table className="ml-6 w-[calc(100%-1.5rem)] text-sm">
                <thead>
                  <tr>
                    <th className="py-1.5 pr-4 text-left text-xs font-medium text-terra/50">Propriedade</th>
                    <th className="py-1.5 pr-4 text-left text-xs font-medium text-terra/50">Local</th>
                    <th className="py-1.5 pr-4 text-right text-xs font-medium text-terra/50">Área (ha)</th>
                    <th className="py-1.5 pr-4 text-right text-xs font-medium text-terra/50">Membros</th>
                    <th className="py-1.5 pr-4 text-left text-xs font-medium text-terra/50">Criada em</th>
                    <th className="py-1.5 text-left text-xs font-medium text-terra/50">Última atividade</th>
                  </tr>
                </thead>
                <tbody>
                  {org.properties.map((p) => (
                    <tr key={p.id} className="border-t border-terra/10">
                      <td className="py-1.5 pr-4">{p.name}</td>
                      <td className="py-1.5 pr-4 text-terra/70">{p.location || '—'}</td>
                      <td className="py-1.5 pr-4 text-right tabular-nums">{p.totalAreaHa ?? '—'}</td>
                      <td className="py-1.5 pr-4 text-right tabular-nums">{p.members}</td>
                      <td className="whitespace-nowrap py-1.5 pr-4 text-terra/70">{formatDate(p.createdAt)}</td>
                      <td className="whitespace-nowrap py-1.5">
                        <Activity at={p.lastActivityAt} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </td>
        </tr>
      )}
    </>
  )
}

function UsersTable({ rows, query }: { rows: AdminUserRow[]; query: string }) {
  return (
    <table className="w-full min-w-[720px] text-sm">
      <thead className="border-b border-terra/10 bg-terreiro/40">
        <tr>
          <th className={th}>Nome</th>
          <th className={th}>E-mail</th>
          <th className={`${th} text-right`}>Propriedades</th>
          <th className={th}>Cadastro</th>
          <th className={th}>Última atividade</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((u) => (
          <tr key={u.id} className="border-t border-terra/5 hover:bg-terreiro/40">
            <td className={`${td} whitespace-nowrap`}>
              {u.name}
              {u.platformAdmin && (
                <span className="ml-2 rounded-full border border-trigo-dark/40 px-1.5 py-px text-xs text-trigo-dark">Admin</span>
              )}
            </td>
            <td className={`${td} text-terra/70`}>{u.email}</td>
            <td className={`${td} text-right tabular-nums`}>
              {u.properties > 0 ? u.properties : <span className="text-terra/40">nenhuma</span>}
            </td>
            <td className={`${td} whitespace-nowrap text-terra/70`}>{formatDate(u.createdAt)}</td>
            <td className={`${td} whitespace-nowrap`}>
              <Activity at={u.lastActivityAt} />
            </td>
          </tr>
        ))}
        {rows.length === 0 && <EmptyRow colSpan={5} query={query} hint="Busque por parte do nome ou do e-mail." />}
      </tbody>
    </table>
  )
}
