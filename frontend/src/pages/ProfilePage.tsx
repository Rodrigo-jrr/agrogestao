import { useId, useState, type FormEvent, type ReactNode } from 'react'
import { meApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { FormMessage } from '../components/FormMessage'
import { fieldLabelClass, inputClass, linkDangerClass, primaryButtonClass } from '../components/ui'

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

export function ProfilePage() {
  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Meu perfil</h1>
      <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm md:p-6">
        <Section title="Seus dados" description="Como seu nome aparece para a equipe e no histórico.">
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
      </div>
    </div>
  )
}
