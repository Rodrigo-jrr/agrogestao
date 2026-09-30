/** Mensagem de resultado de um formulário: erro (vermelho) ou sucesso (verde), anunciada a leitores de tela. */
export function FormMessage({ error, success }: { error?: string | null; success?: string | null }) {
  if (error) {
    return (
      <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
        {error}
      </p>
    )
  }
  if (success) {
    return (
      <p role="status" className="rounded-md bg-mata/10 px-3 py-2 text-sm text-mata">
        {success}
      </p>
    )
  }
  return null
}
