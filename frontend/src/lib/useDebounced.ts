import { useEffect, useState } from 'react'

/** Valor que só muda depois de {@code delay} ms sem alterações (ex.: busca enquanto digita). */
export function useDebounced<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}
