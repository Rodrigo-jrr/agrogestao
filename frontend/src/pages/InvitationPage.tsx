import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { invitationsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { InvitationPreview } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { useUrlToken } from '../auth/useUrlToken'
import { useProperty } from '../property/PropertyContext'
import { AuthShell } from '../components/AuthShell'
import { FormMessage } from '../components/FormMessage'
import { inputClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { roleLabels } from '../lib/roles'
import { TERMS_VERSION } from '../lib/legal'
import { TermsCheckbox } from '../components/TermsCheckbox'

// O token sai da barra de endereço; fica guardado nesta aba para voltar aqui depois de entrar.
const STORAGE_KEY = 'agro.inviteToken'

function useInvitationToken(): string | null {
  const fromUrl = useUrlToken()
  const [token] = useState(() => {
    if (fromUrl) {
      sessionStorage.setItem(STORAGE_KEY, fromUrl)
      return fromUrl
    }
    return sessionStorage.getItem(STORAGE_KEY)
  })
  return token
}

export function InvitationPage() {
  const token = useInvitationToken()
  const { user, isAuthenticated, logout, startSession } = useAuth()
  const { refresh } = useProperty()
  const navigate = useNavigate()
  const id = useId()
  const [preview, setPreview] = useState<InvitationPreview | null>(null)
  const [loadError, setLoadError] = useState<string | null>(token ? null : 'Este endereço não traz o convite.')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const started = useRef(false)

  useEffect(() => {
    if (!token || started.current) return
    started.current = true
    invitationsApi
      .preview(token)
      .then(setPreview)
      .catch((err) => {
        sessionStorage.removeItem(STORAGE_KEY)
        setLoadError(apiErrorMessage(err))
      })
  }, [token])

  async function finish(propertyId?: number) {
    sessionStorage.removeItem(STORAGE_KEY)
    if (propertyId) localStorage.setItem('agro.propertyId', String(propertyId))
    await refresh()
    navigate('/dashboard', { replace: true })
  }

  async function accept() {
    if (!token) return
    setBusy(true)
    setError(null)
    try {
      const property = await invitationsApi.accept(token)
      await finish(property.id)
    } catch (err) {
      setError(apiErrorMessage(err))
      setBusy(false)
    }
  }

  async function register(event: FormEvent) {
    event.preventDefault()
    if (!token) return
    setBusy(true)
    setError(null)
    try {
      startSession(await invitationsApi.register(token, name, password, TERMS_VERSION))
      // A lista de propriedades recarrega sozinha ao entrar; a única é a do convite.
      sessionStorage.removeItem(STORAGE_KEY)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
      setBusy(false)
    }
  }

  if (loadError) {
    return (
      <AuthShell title="Convite indisponível">
        <p role="alert" className="mb-6 text-sm text-terra/70">
          {loadError}
        </p>
        <button onClick={() => navigate(isAuthenticated ? '/dashboard' : '/login')} className={`w-full ${primaryButtonClass}`}>
          {isAuthenticated ? 'Ir para o painel' : 'Entrar'}
        </button>
      </AuthShell>
    )
  }

  if (!preview) {
    return (
      <AuthShell title="Convite">
        <p role="status" className="text-sm text-terra/60">
          Carregando o convite…
        </p>
      </AuthShell>
    )
  }

  const intro = (
    <p className="mb-6 text-sm leading-relaxed text-terra/70">
      {preview.invitedByName ?? 'Alguém'} convidou você para a equipe de{' '}
      <strong className="font-medium text-terra">{preview.propertyName}</strong> como{' '}
      <strong className="font-medium text-terra">{roleLabels[preview.role].toLowerCase()}</strong>.
    </p>
  )
  const sameAccount = user && user.email.toLowerCase() === preview.email.toLowerCase()

  return (
    <AuthShell title="Convite para a equipe">
      {intro}
      <div className="space-y-4">
        <FormMessage error={error} />

        {isAuthenticated && sameAccount && (
          <button onClick={accept} disabled={busy} className={`w-full ${primaryButtonClass}`}>
            {busy ? 'Entrando na equipe…' : 'Aceitar convite'}
          </button>
        )}

        {isAuthenticated && !sameAccount && (
          <>
            <p className="text-sm text-terra/70">
              Você está conectado como <strong className="font-medium break-all">{user?.email}</strong>, mas o convite é
              para <strong className="font-medium break-all">{preview.email}</strong>.
            </p>
            <button onClick={logout} className={`w-full ${outlineButtonClass}`}>
              Sair desta conta e continuar
            </button>
          </>
        )}

        {!isAuthenticated && preview.accountExists && (
          <>
            <p className="text-sm text-terra/70">
              Já existe uma conta com <strong className="font-medium break-all">{preview.email}</strong>. Entre com ela
              para aceitar.
            </p>
            <button onClick={() => navigate('/login?next=/convite')} className={`w-full ${primaryButtonClass}`}>
              Entrar para aceitar
            </button>
          </>
        )}

        {!isAuthenticated && !preview.accountExists && (
          <form onSubmit={register}>
            <p className="mb-4 text-sm text-terra/70">Crie sua conta para entrar na equipe.</p>

            <label htmlFor={`${id}-email`} className="mb-1 block text-sm font-medium text-terra">
              E-mail
            </label>
            <input id={`${id}-email`} value={preview.email} readOnly className={`mb-4 bg-terra/5 ${inputClass}`} />

            <label htmlFor={`${id}-name`} className="mb-1 block text-sm font-medium text-terra">
              Seu nome
            </label>
            <input
              id={`${id}-name`}
              required
              maxLength={150}
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={`mb-4 ${inputClass}`}
            />

            <label htmlFor={`${id}-password`} className="mb-1 block text-sm font-medium text-terra">
              Crie uma senha
            </label>
            <input
              id={`${id}-password`}
              type="password"
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              aria-describedby={`${id}-hint`}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClass}
            />
            <p id={`${id}-hint`} className="mt-1 mb-4 text-xs text-terra/50">
              Pelo menos 8 caracteres.
            </p>

            <div className="mb-6">
              <TermsCheckbox checked={acceptedTerms} onChange={setAcceptedTerms} />
            </div>

            <button disabled={busy} className={`w-full ${primaryButtonClass}`}>
              {busy ? 'Criando conta…' : 'Criar conta e entrar na equipe'}
            </button>
          </form>
        )}
      </div>
    </AuthShell>
  )
}
