import { useState } from 'react'

/** Conjunto de ids marcados, com alternância. */
export function useSelection() {
  const [chosen, setChosen] = useState<Set<number>>(new Set())
  function toggle(id: number) {
    setChosen((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }
  return { chosen, toggle, clear: () => setChosen(new Set()) }
}
