import { AxiosError, AxiosHeaders } from 'axios'

/** Erro no formato que a API devolve ({ message }), para testar as mensagens das telas. */
export function apiError(message: string, status = 422) {
  return new AxiosError(message, String(status), undefined, undefined, {
    status,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
    data: { message },
  })
}
