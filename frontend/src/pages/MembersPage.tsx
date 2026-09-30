import { useEffect, useId, useState, type FormEvent } from 'react'
import { invitationsApi, membersApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { InvitationInfo, PropertyMemberInfo, PropertyRole } from '../api/types'
import { useProperty } from '../property/PropertyContext'
import { fieldLabelClass, inputClass, linkDangerClass, linkNeutralClass, primaryButtonClass } from '../components/ui'
import { useConfirm } from '../components/ConfirmDialog'
import { FormMessage } from '../components/FormMessage'
import { formatDate } from '../lib/format'
import { roleDescriptions, roleLabels } from '../lib/roles'

export function MembersPage() {
  const { currentProperty } = useProperty()
  const [members, setMembers] = useState<PropertyMemberInfo[]>([])
  const [invitations, setInvitations] = useState<InvitationInfo[]>([])
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [listError, setListError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<PropertyRole>('WORKER')

  const formId = useId()
  const confirm = useConfirm()

  const myRole = currentProperty?.role
  const canManage = myRole === 'OWNER' || myRole === 'MANAGER'
  const assignableRoles: PropertyRole[] =
    myRole === 'OWNER' ? ['OWNER', 'MANAGER', 'WORKER', 'VIEWER'] : ['MANAGER', 'WORKER', 'VIEWER']

  function loadMembers() {
    setListError(null)
    membersApi
      .list()
      .then(setMembers)
      .catch((err) => setListError(apiErrorMessage(err)))
      .finally(() => setLoading(false))
    if (canManage) {
      invitationsApi
        .list()
        .then(setInvitations)
        .catch((err) => setListError(apiErrorMessage(err)))
    } else {
      setInvitations([])
    }
  }

  useEffect(() => {
    if (currentProperty) loadMembers()
  }, [currentProperty])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    setSending(true)
    try {
      const result = await invitationsApi.invite({ email, role })
      setSuccess(
        result.outcome === 'ADDED'
          ? `${result.member?.name} já tinha conta e entrou na equipe. Avisamos por e-mail.`
          : `Convite enviado para ${email.trim()}. O link vale por 7 dias.`,
      )
      setEmail('')
      setRole('WORKER')
      loadMembers()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSending(false)
    }
  }

  async function handleResend(invitation: InvitationInfo) {
    setListError(null)
    try {
      await invitationsApi.resend(invitation.id)
      setSuccess(`Enviamos um novo convite para ${invitation.email}. O link anterior não funciona mais.`)
      loadMembers()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  async function handleCancelInvitation(invitation: InvitationInfo) {
    const ok = await confirm({
      title: `Cancelar o convite de ${invitation.email}?`,
      message: 'O link enviado deixa de funcionar. Você pode convidar a pessoa de novo depois.',
      confirmLabel: 'Cancelar convite',
    })
    if (!ok) return
    try {
      await invitationsApi.cancel(invitation.id)
      loadMembers()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  async function handleRoleChange(member: PropertyMemberInfo, newRole: PropertyRole) {
    setListError(null)
    try {
      await membersApi.updateRole(member.id, newRole)
      loadMembers()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }


  async function handleRemove(member: PropertyMemberInfo) {
    const ok = await confirm({
      title: `Remover ${member.name} da equipe?`,
      message: 'A pessoa perde o acesso a esta propriedade na hora. Os registros que ela fez continuam. Você pode adicioná-la de novo depois.',
      confirmLabel: 'Remover da equipe',
    })
    if (!ok) return
    try {
      await membersApi.remove(member.id)
      loadMembers()
    } catch (err) {
      setListError(apiErrorMessage(err))
    }
  }

  // Gerentes não alteram proprietários; ninguém altera o próprio papel por aqui.
  const canEdit = (member: PropertyMemberInfo) =>
    canManage && !member.currentUser && (myRole === 'OWNER' || member.role !== 'OWNER')

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Equipe</h1>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {canManage ? (
          <form onSubmit={handleSubmit} className="space-y-3 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
            <h2 className="text-sm font-semibold text-terra">Convidar pessoa</h2>
            <FormMessage error={error} success={success} />
            <div>
              <label htmlFor={`${formId}-e-mail-da-pessoa`} className={fieldLabelClass}>
                E-mail da pessoa
              </label>
              <input
                required
                type="email"
                id={`${formId}-e-mail-da-pessoa`}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label htmlFor={`${formId}-papel`} className={fieldLabelClass}>
                Papel
              </label>
              <select
                id={`${formId}-papel`}
                value={role}
                onChange={(e) => setRole(e.target.value as PropertyRole)}
                className={inputClass}
              >
                {assignableRoles.map((r) => (
                  <option key={r} value={r}>
                    {roleLabels[r]}
                  </option>
                ))}
              </select>
            </div>
            <p className="text-xs text-terra/50">{roleDescriptions[role]}</p>
            <button disabled={sending} className={`w-full ${primaryButtonClass}`}>
              {sending ? 'Enviando…' : 'Enviar convite'}
            </button>
            <p className="text-xs text-terra/50">
              Quem já tem conta entra na hora. Quem não tem recebe um link por e-mail para criar a conta.
            </p>
          </form>
        ) : (
          <div className="rounded-2xl border border-terra/10 bg-white p-4 text-sm text-terra/60 shadow-sm">
            Seu papel nesta propriedade é <strong>{myRole ? roleLabels[myRole] : '-'}</strong>. Apenas proprietários e
            gerentes podem gerenciar a equipe.
          </div>
        )}

        <div className="md:col-span-2 rounded-2xl border border-terra/10 bg-white p-4 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-terra">Membros da propriedade</h2>
          {listError && <p className="mb-3 rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">{listError}</p>}
          <div className="relative overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-terra/50">
                  <th className="pb-2">Nome</th>
                  <th className="pb-2">E-mail</th>
                  <th className="pb-2">Papel</th>
                  <th className="pb-2" />
                </tr>
              </thead>
              <tbody>
                {members.map((m) => (
                  <tr key={m.id} className="border-t border-terra/5">
                    <td className="py-2 pr-3">
                      {m.name}
                      {m.currentUser && <span className="ml-1 text-xs text-terra/40">(você)</span>}
                    </td>
                    <td className="py-2 pr-3 break-all text-terra/50">{m.email}</td>
                    <td className="py-2">
                      {canEdit(m) ? (
                        <select
                          aria-label={`Papel de ${m.name}`}
                          value={m.role}
                          onChange={(e) => handleRoleChange(m, e.target.value as PropertyRole)}
                          className="rounded-md border border-terra/20 bg-white px-2 py-1 text-sm focus:border-rust focus:outline-none"
                        >
                          {assignableRoles.map((r) => (
                            <option key={r} value={r}>
                              {roleLabels[r]}
                            </option>
                          ))}
                        </select>
                      ) : (
                        roleLabels[m.role]
                      )}
                    </td>
                    <td className="py-2 pl-2 text-right">
                      {canEdit(m) && (
                        <button onClick={() => handleRemove(m)} className={linkDangerClass}>
                          Remover
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
                {loading && members.length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-terra/50">
                      Carregando…
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {invitations.length > 0 && (
            <>
              <h2 className="mt-6 mb-3 text-sm font-semibold text-terra">Convites pendentes</h2>
              <ul className="divide-y divide-terra/5 border-t border-terra/5 text-sm">
                {invitations.map((invitation) => {
                  const canHandle = myRole === 'OWNER' || invitation.role !== 'OWNER'
                  return (
                    <li key={invitation.id} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-2">
                      <div className="min-w-0">
                        <p className="break-all text-terra">{invitation.email}</p>
                        <p className="text-xs text-terra/50">
                          {roleLabels[invitation.role]} ·{' '}
                          {invitation.expired ? (
                            <span className="text-rust">expirou em {formatDate(invitation.expiresAt)}</span>
                          ) : (
                            <>vale até {formatDate(invitation.expiresAt)}</>
                          )}
                          {invitation.invitedByName && <> · enviado por {invitation.invitedByName}</>}
                        </p>
                      </div>
                      {canHandle && (
                        <div className="flex gap-3">
                          <button onClick={() => handleResend(invitation)} className={`cursor-pointer ${linkNeutralClass}`}>
                            Reenviar
                          </button>
                          <button
                            onClick={() => handleCancelInvitation(invitation)}
                            className={`cursor-pointer ${linkDangerClass}`}
                          >
                            Cancelar
                          </button>
                        </div>
                      )}
                    </li>
                  )
                })}
              </ul>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
