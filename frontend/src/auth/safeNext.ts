/** Aceita só caminhos internos (evita redirecionar para outro site via ?next=). */
export function safeNext(value: string | null): string | null {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.includes('\\')) return null
  return value
}
