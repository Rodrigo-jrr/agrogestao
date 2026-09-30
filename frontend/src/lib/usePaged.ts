import { useEffect, useState } from 'react'
import { apiErrorMessage } from '../api/client'
import type { PageResponse } from '../api/types'

/**
 * Lista paginada no servidor. Volta para a primeira página quando os filtros (deps) mudam;
 * reload() busca de novo a página atual (ex.: depois de criar ou excluir um registro).
 */
export function usePaged<T>(
  fetchPage: (page: number) => Promise<PageResponse<T>>,
  deps: unknown[],
  enabled = true,
) {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<T> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => setPage(0), deps)

  useEffect(() => {
    if (!enabled) return
    let active = true
    setLoading(true)
    setError(null)
    fetchPage(page)
      .then((result) => active && setData(result))
      .catch((err) => active && setError(apiErrorMessage(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, version, enabled, ...deps])

  return {
    items: data?.content ?? [],
    page,
    setPage,
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    loading,
    error,
    version,
    reload: () => setVersion((v) => v + 1),
  }
}
