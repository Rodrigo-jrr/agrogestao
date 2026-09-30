import { useState } from 'react'

/**
 * Lê o token do link recebido por e-mail e o remove da barra de endereço, para que não fique
 * no histórico do navegador nem seja copiado junto com a URL.
 */
export function useUrlToken(): string | null {
  const [token] = useState(() => {
    const params = new URLSearchParams(window.location.search)
    const value = params.get('token')
    if (value) {
      params.delete('token')
      const query = params.toString()
      window.history.replaceState(window.history.state, '', window.location.pathname + (query ? `?${query}` : ''))
    }
    return value
  })
  return token
}
