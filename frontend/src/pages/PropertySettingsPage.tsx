import { useEffect, useId, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { propertiesApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import { useProperty } from '../property/PropertyContext'
import { FormMessage } from '../components/FormMessage'
import { fieldLabelClass, inputClass, outlineButtonClass, primaryButtonClass } from '../components/ui'
import { formatNumber } from '../lib/format'
import { roleLabels } from '../lib/roles'

function EditForm() {
  const { currentProperty, refresh } = useProperty()
  const id = useId()
  const [name, setName] = useState(currentProperty?.name ?? '')
  const [location, setLocation] = useState(currentProperty?.location ?? '')
  const [area, setArea] = useState(currentProperty?.totalAreaHa?.toString() ?? '')
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<{ error?: string; success?: string }>({})

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setResult({})
    try {
      await propertiesApi.updateCurrent({
        name,
        location: location.trim() || null,
        totalAreaHa: area === '' ? null : Number(area),
      })
      await refresh()
      setResult({ success: 'Dados da propriedade salvos.' })
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
          Nome da propriedade
        </label>
        <input
          id={`${id}-name`}
          required
          maxLength={150}
          value={name}
          onChange={(e) => setName(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${id}-location`} className={fieldLabelClass}>
          Localização
        </label>
        <input
          id={`${id}-location`}
          maxLength={255}
          placeholder="Ex.: Zona rural de Uberaba - MG"
          value={location}
          onChange={(e) => setLocation(e.target.value)}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor={`${id}-area`} className={fieldLabelClass}>
          Área total (hectares)
        </label>
        <input
          id={`${id}-area`}
          type="number"
          min={0}
          step="0.01"
          inputMode="decimal"
          value={area}
          onChange={(e) => setArea(e.target.value)}
          className={inputClass}
        />
      </div>
      <button disabled={saving} className={primaryButtonClass}>
        {saving ? 'Salvando…' : 'Salvar alterações'}
      </button>
    </form>
  )
}

/** Exclusão definitiva: diálogo próprio porque pede o nome da propriedade e a senha. */
function DeleteProperty() {
  const { currentProperty, refresh } = useProperty()
  const navigate = useNavigate()
  const id = useId()
  const dialogRef = useRef<HTMLDialogElement>(null)
  const [open, setOpen] = useState(false)
  const [typedName, setTypedName] = useState('')
  const [password, setPassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const propertyName = currentProperty?.name ?? ''
  const nameMatches = typedName.trim() === propertyName.trim()

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  function close() {
    setOpen(false)
    setTypedName('')
    setPassword('')
    setError(null)
  }

  async function handleDelete(event: FormEvent) {
    event.preventDefault()
    setDeleting(true)
    setError(null)
    try {
      await propertiesApi.deleteCurrent(typedName, password)
      localStorage.removeItem('agro.propertyId')
      close()
      await refresh()
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="max-w-md space-y-3">
      <p className="text-sm text-terra/70">
        Apaga a propriedade e tudo o que foi registrado nela: lançamentos, estoque, compras, vendas, produção, equipe e
        histórico. Não dá para desfazer.
      </p>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="cursor-pointer rounded-md border border-rust px-3 py-2 text-sm font-medium text-rust hover:bg-rust/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust"
      >
        Excluir propriedade…
      </button>

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
              Excluir {propertyName} para sempre?
            </h2>
            <p className="text-sm leading-relaxed text-terra/75">
              Todos os dados desta propriedade serão apagados para todas as pessoas da equipe. Isso não pode ser
              desfeito.
            </p>
            <FormMessage error={error} />
            <div>
              <label htmlFor={`${id}-confirm`} className={fieldLabelClass}>
                Digite <strong className="font-semibold text-terra">{propertyName}</strong> para confirmar
              </label>
              <input
                id={`${id}-confirm`}
                required
                autoComplete="off"
                autoFocus
                value={typedName}
                onChange={(e) => setTypedName(e.target.value)}
                className={inputClass}
              />
            </div>
            <div>
              <label htmlFor={`${id}-password`} className={fieldLabelClass}>
                Sua senha
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
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button type="button" onClick={close} className={`cursor-pointer ${outlineButtonClass}`}>
                Voltar
              </button>
              <button
                disabled={!nameMatches || !password || deleting}
                className="cursor-pointer rounded-md bg-rust px-3 py-2 text-sm font-medium text-terreiro hover:bg-rust-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {deleting ? 'Excluindo…' : 'Excluir para sempre'}
              </button>
            </div>
          </form>
        )}
      </dialog>
    </div>
  )
}

export function PropertySettingsPage() {
  const { currentProperty } = useProperty()
  if (!currentProperty) return null
  const role = currentProperty.role
  const canEdit = role === 'OWNER' || role === 'MANAGER'

  return (
    <div className="space-y-4">
      <h1 className="font-serif text-2xl font-semibold text-terra">Propriedade</h1>
      <div className="rounded-2xl border border-terra/10 bg-white p-4 shadow-sm md:p-6">
        <section className="grid gap-4 md:grid-cols-3">
          <div>
            <h2 className="text-sm font-semibold text-terra">Dados da propriedade</h2>
            <p className="mt-1 text-sm text-terra/60">
              Seu papel aqui: <strong className="font-medium text-terra">{roleLabels[role]}</strong>.
            </p>
          </div>
          <div className="md:col-span-2">
            {canEdit ? (
              // key: recarrega o formulário ao trocar de propriedade no seletor.
              <EditForm key={currentProperty.id} />
            ) : (
              <dl className="grid max-w-md grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
                <dt className="text-terra/60">Nome</dt>
                <dd className="text-terra">{currentProperty.name}</dd>
                <dt className="text-terra/60">Localização</dt>
                <dd className="text-terra">{currentProperty.location || '—'}</dd>
                <dt className="text-terra/60">Área total</dt>
                <dd className="text-terra">
                  {currentProperty.totalAreaHa != null ? `${formatNumber(currentProperty.totalAreaHa)} ha` : '—'}
                </dd>
              </dl>
            )}
          </div>
        </section>

        {role === 'OWNER' && (
          <section className="mt-6 grid gap-4 border-t border-rust/20 pt-6 md:grid-cols-3">
            <div>
              <h2 className="text-sm font-semibold text-rust">Excluir propriedade</h2>
              <p className="mt-1 text-sm text-terra/60">Só proprietários veem esta opção.</p>
            </div>
            <div className="md:col-span-2">
              <DeleteProperty key={currentProperty.id} />
            </div>
          </section>
        )}
      </div>
    </div>
  )
}
