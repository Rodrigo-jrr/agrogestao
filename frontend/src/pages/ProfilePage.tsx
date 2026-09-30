import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { meApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { DeletionPreview } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { FormMessage } from '../components/FormMessage'
import {
  fieldLabelClass,
  inputClass,
  linkDangerClass,
  outlineButtonClass,
  primaryButtonClass,
} from '../components/ui'
import { todayIso } from '../lib/format'

function Section({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <section className="grid gap-4 border-t border-terra/10 py-6 first:border-t-0 first:pt-0 md:grid-cols-3">
      <div>
        <h2 className="text-sm font-semibold text-terra">{title}</h2>
        <p className="mt-1 text-sm text-terra/60">{description}</p>
      </div>
      <div className="md:col-span-2">{children}</div>
    </section>
  )
}

function NameForm() {
  const { user, refreshMe } = useAuth()
  const id = useId()
  const [name, setName] = useState(user?.name ?? '')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setResult({})
    try {
      await meApi.updateName(name)
      await refreshMe()
      setResult({ success: 'Nome atualizado.' })
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-3">
      <FormMessage {...result} />
      <div>
        <label htmlFor={`${id}-name`} className={fieldLabelClass}>
          Nome
        </label>
        <input
          id={`${id}-name`}
          required
          maxLength={150}
          autoComplete="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <button disabled={saving || name.trim() === user?.name} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Salvar nome'}
      </button>
    </form>
  )
}

function EmailForm() {
  const { user, pendingEmail, refreshMe } = useAuth()
  const id = useId()
  const [newEmail, setNewEmail] = useState('')
  const [password, setPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setResult({})
    try {
      await meApi.requestEmailChange(newEmail, password)
      await refreshMe()
      setResult({ success: `Enviamos um link para ${newEmail.trim()}. A troca acontece quando você abrir o link.` })
      setNewEmail('')
      setPassword('')
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  async function cancel() {
    setResult({})
    try {
      await meApi.cancelEmailChange()
      await refreshMe()
      setResult({ success: 'Troca de e-mail cancelada. O link enviado não funciona mais.' })
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    }
  }

  return (
    <div className="max-w-md space-y-3">
      <p className="text-sm text-terra/70">
        E-mail atual: <strong className="font-medium text-terra break-all">{user?.email}</strong>
      </p>
      <FormMessage {...result} />
      {pendingEmail && (
        <div className="rounded-md border border-trigo/40 bg-trigo/15 px-3 py-2 text-sm text-terra">
          Aguardando confirmação de <strong className="font-medium break-all">{pendingEmail}</strong>. Abra o link que
          enviamos para esse endereço.{' '}
          <button type="button" onClick={cancel} className={`cursor-pointer ${linkDangerClass}`}>
            Cancelar troca
          </button>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-3">
        <div>
          <label htmlFor={`${id}-email`} className={fieldLabelClass}>
            Novo e-mail
          </label>
          <input
            id={`${id}-email`}
            type="email"
            required
            maxLength={180}
            autoComplete="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor={`${id}-password`} className={fieldLabelClass}>
            Sua senha atual
          </label>
          <input
            id={`${id}-password`}
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
        </div>
        <button disabled={saving} className={primaryButtonClass}>
          {saving ? 'Enviando…' : 'Enviar link de confirmação'}
        </button>
      </form>
    </div>
  )
}

function PasswordForm() {
  const { startSession } = useAuth()
  const id = useId()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  const mismatch = confirmation.length > 0 && confirmation !== next

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (next !== confirmation) {
      setResult({ error: 'A confirmação não é igual à nova senha.' })
      return
    }
    setSaving(true)
    setResult({})
    try {
      // O token antigo deixa de valer na hora: guardamos o novo antes de qualquer outra chamada.
      startSession(await meApi.changePassword(current, next))
      setResult({ success: 'Senha alterada. Outros aparelhos conectados precisarão entrar de novo.' })
      setCurrent('')
      setNext('')
      setConfirmation('')
    } catch (err) {
      setResult({ error: apiErrorMessage(err) })
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-3">
      <FormMessage {...result} />
      <div>
        <label htmlFor={`${id}-current`} className={fieldLabelClass}>
          Senha atual
        </label>
        <input
          id={`${id}-current`}
          type="password"
          required
          autoComplete="current-password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${id}-new`} className={fieldLabelClass}>
          Nova senha
        </label>
        <input
          id={`${id}-new`}
          type="password"
          required
          minLength={8}
          maxLength={72}
          autoComplete="new-password"
          aria-describedby={`${id}-hint`}
          value={next}
          onChange={(e) => setNext(e.target.value)}
          className={inputClass}
        />
        <p id={`${id}-hint`} className="mt-1 text-xs text-terra/50">
          Pelo menos 8 caracteres.
        </p>
      </div>
      <div>
        <label htmlFor={`${id}-confirm`} className={fieldLabelClass}>
          Repita a nova senha
        </label>
        <input
          id={`${id}-confirm`}
          type="password"
          required
          autoComplete="new-password"
          aria-invalid={mismatch}
          aria-describedby={mismatch ? `${id}-mismatch` : undefined}
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          className={inputClass}
        />
        {mismatch && (
          <p id={`${id}-mismatch`} className="mt-1 text-xs text-rust">
            Diferente da nova senha.
          </p>
        )}
      </div>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Alterando…' : 'Alterar senha'}
      </button>
    </form>
  )
}

/** Erros de download vêm como Blob; lê a mensagem JSON de dentro dele. */
async function blobErrorMessage(err: unknown) {
  if (axios.isAxiosError(err) && err.response?.data instanceof Blob) {
    try {
      const data = JSON.parse(await err.response.data.text()) as { message?: string }
      if (data.message) return data.message
    } catch {
      // cai na mensagem padrão
    }
  }
  return apiErrorMessage(err)
}

function ExportData() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function download() {
    setBusy(true)
    setError(null)
    try {
      const blob = await meApi.exportData()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `agrogestao-meus-dados-${todayIso()}.json`
      link.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(await blobErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="max-w-md space-y-3">
      <p className="text-sm text-terra/70">
        Um arquivo com sua conta, seus aceites, as propriedades em que você participa, o que você fez no histórico e,
        onde você é proprietário, todos os dados da propriedade. Formato JSON, que pode ser aberto em outros sistemas.
      </p>
      <FormMessage error={error} />
      <button type="button" onClick={download} disabled={busy} className={`cursor-pointer ${outlineButtonClass}`}>
        {busy ? 'Preparando arquivo…' : 'Baixar meus dados'}
      </button>
      <p className="text-xs text-terra/50">
        Leia a{' '}
        <Link to="/privacidade" className="text-rust hover:underline">
          Política de privacidade
        </Link>{' '}
        e os{' '}
        <Link to="/termos" className="text-rust hover:underline">
          Termos de uso
        </Link>
        .
      </p>
    </div>
  )
}

function PropertyList({ title, items }: { title: string; items: { id: number; name: string }[] }) {
  if (items.length === 0) return null
  return (
    <div>
      <p className="font-medium text-terra">{title}</p>
      <ul className="mt-1 list-disc pl-5 text-terra/75">
        {items.map((item) => (
          <li key={item.id}>{item.name}</li>
        ))}
      </ul>
    </div>
  )
}

function DeleteAccount() {
  const { logout } = useAuth()
  const id = useId()
  const dialogRef = useRef<HTMLDialogElement>(null)
  const [preview, setPreview] = useState<DeletionPreview | null>(null)
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    meApi
      .deletionPreview()
      .then(setPreview)
      .catch((err) => setError(apiErrorMessage(err)))
  }, [])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  function close() {
    setOpen(false)
    setPassword('')
    setError(null)
  }

  async function handleDelete(event: FormEvent) {
    event.preventDefault()
    setDeleting(true)
    setError(null)
    try {
      await meApi.deleteAccount(password)
      // Aviso para a tela de login: ao sair, a rota protegida redireciona para /login por conta própria.
      sessionStorage.setItem('agro.accountDeleted', '1')
      logout()
    } catch (err) {
      setError(apiErrorMessage(err))
      setDeleting(false)
    }
  }

  if (!preview) {
    return error ? <FormMessage error={error} /> : <p className="text-sm text-terra/50">Carregando…</p>
  }

  const blocked = preview.blocking.length > 0

  return (
    <div className="max-w-md space-y-3 text-sm">
      <p className="text-terra/70">
        Apaga sua conta e seus dados pessoais. Não dá para desfazer. Se quiser guardar uma cópia, baixe seus dados antes.
      </p>
      <PropertyList title="Serão excluídas junto (só você está nelas):" items={preview.toDelete} />
      <PropertyList title="Você sai da equipe (elas continuam para as outras pessoas):" items={preview.toLeave} />
      {blocked ? (
        <div className="rounded-md border border-trigo/40 bg-trigo/15 px-3 py-2 text-terra">
          <PropertyList title="Antes, resolva estas propriedades:" items={preview.blocking} />
          <p className="mt-2">
            Você é o único proprietário e há outras pessoas na equipe. Em{' '}
            <Link to="/equipe" className="font-medium text-rust hover:underline">
              Equipe
            </Link>
            , torne alguém proprietário — ou exclua a propriedade em{' '}
            <Link to="/propriedade" className="font-medium text-rust hover:underline">
              Propriedade
            </Link>
            .
          </p>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className="cursor-pointer rounded-md border border-rust px-3 py-2 font-medium text-rust hover:bg-rust/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust"
        >
          Excluir minha conta…
        </button>
      )}

      <dialog
        ref={dialogRef}
        aria-labelledby={`${id}-title`}
        onCancel={(e) => {
          e.preventDefault()
          close()
        }}
        className="m-auto w-[min(30rem,calc(100vw-2rem))] rounded-2xl border border-terra/10 bg-white p-0 text-terra shadow-xl backdrop:bg-terra/40"
      >
        {open && (
          <form onSubmit={handleDelete} className="space-y-4 p-5">
            <h2 id={`${id}-title`} className="font-serif text-xl font-semibold">
              Excluir sua conta para sempre?
            </h2>
            <p className="text-sm leading-relaxed text-terra/75">
              Você sai de todas as propriedades e perde o acesso na hora.
              {preview.toDelete.length > 0 &&
                ` ${preview.toDelete.length === 1 ? 'A propriedade' : 'As propriedades'} ${preview.toDelete
                  .map((p) => p.name)
                  .join(', ')} ${preview.toDelete.length === 1 ? 'será apagada' : 'serão apagadas'} com todos os dados.`}
            </p>
            <FormMessage error={error} />
            <div>
              <label htmlFor={`${id}-password`} className={fieldLabelClass}>
                Sua senha
              </label>
              <input
                id={`${id}-password`}
                type="password"
                required
                autoFocus
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClass}
              />
            </div>
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button type="button" onClick={close} className={`cursor-pointer ${outlineButtonClass}`}>
                Voltar
              </button>
              <button
                disabled={deleting}
                className="cursor-pointer rounded-md bg-rust px-3 py-2 text-sm font-medium text-terreiro hover:bg-rust-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust focus-visible:ring-offset-2 disabled:opacity-50"
              >
                {deleting ? 'Excluindo…' : 'Excluir minha conta'}
              </button>
            </div>
          </form>
        )}
      </dialog>
    </div>
  )
}

export function ProfilePage() {
  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Meu perfil</h1>
      <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm md:p-6">
        <Section title="Nome" description="Como seu nome aparece para a equipe e no histórico.">
          <NameForm />
        </Section>
        <Section
          title="E-mail"
          description="Usado para entrar e recuperar a senha. O novo endereço precisa ser confirmado."
        >
          <EmailForm />
        </Section>
        <Section title="Senha" description="Ao trocar, os outros aparelhos conectados saem da conta.">
          <PasswordForm />
        </Section>
        <Section title="Seus dados" description="Baixe uma cópia de tudo o que o AgroGestão guarda sobre você.">
          <ExportData />
        </Section>
        <Section title="Excluir conta" description="Você pode sair do AgroGestão quando quiser.">
          <DeleteAccount />
        </Section>
      </div>
    </div>
  )
}
