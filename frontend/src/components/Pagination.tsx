import { formatNumber } from '../lib/format'

/** Navegação entre páginas de uma lista paginada (page começa em 0). */
export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}) {
  if (totalElements === 0) return null
  const buttonClass =
    'cursor-pointer rounded-md border border-terra/15 px-3 py-1 text-sm text-terra hover:bg-terra/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rust disabled:cursor-default disabled:opacity-40'
  return (
    <nav aria-label="Paginação" className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-terra/60">
      <span>
        {formatNumber(totalElements)} {totalElements === 1 ? 'registro' : 'registros'}
      </span>
      {totalPages > 1 && (
        <div className="flex items-center gap-2">
          <button type="button" className={buttonClass} disabled={page <= 0} onClick={() => onChange(page - 1)}>
            Anterior
          </button>
          <span aria-current="page">
            Página {page + 1} de {totalPages}
          </span>
          <button
            type="button"
            className={buttonClass}
            disabled={page >= totalPages - 1}
            onClick={() => onChange(page + 1)}
          >
            Próxima
          </button>
        </div>
      )}
    </nav>
  )
}
