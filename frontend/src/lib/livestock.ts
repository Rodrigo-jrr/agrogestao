import type { AnimalEventType, AnimalOrigin, AnimalStatus, ExitType, Sex, Species } from '../api/types'

export const speciesLabels: Record<Species, string> = {
  BOVINO: 'Bovino',
  BUBALINO: 'Bubalino',
  OVINO: 'Ovino',
  CAPRINO: 'Caprino',
  SUINO: 'Suíno',
  EQUINO: 'Equino',
  AVE: 'Ave',
  PEIXE: 'Peixe',
  OUTRO: 'Outro',
}

export const speciesPlural: Record<Species, string> = {
  BOVINO: 'bovinos',
  BUBALINO: 'bubalinos',
  OVINO: 'ovinos',
  CAPRINO: 'caprinos',
  SUINO: 'suínos',
  EQUINO: 'equinos',
  AVE: 'aves',
  PEIXE: 'peixes',
  OUTRO: 'outros',
}

/** Aves e peixes só por lote (contagem); as demais também com brinco. */
export const individualSpecies: Species[] = ['BOVINO', 'BUBALINO', 'OVINO', 'CAPRINO', 'SUINO', 'EQUINO', 'OUTRO']
export const allSpecies: Species[] = [...individualSpecies.slice(0, 6), 'AVE', 'PEIXE', 'OUTRO']

export const sexLabels: Record<Sex, string> = { MACHO: 'Macho', FEMEA: 'Fêmea' }

export const statusLabels: Record<AnimalStatus, string> = {
  ATIVO: 'Ativo',
  VENDIDO: 'Vendido',
  MORTO: 'Morto',
  DESCARTADO: 'Descartado',
}

export const originLabels: Record<AnimalOrigin, string> = {
  NASCIMENTO: 'Nascido na propriedade',
  COMPRA: 'Comprado',
  OUTRA: 'Outra origem',
}

export const exitLabels: Record<ExitType, string> = { MORTE: 'Morte', DESCARTE: 'Descarte', OUTRA: 'Outra saída' }

export const eventLabels: Record<AnimalEventType, string> = {
  ENTRADA: 'Entrada',
  MUDANCA_LOTE: 'Troca de lote',
  SAIDA: 'Saída',
}

/** Idade a partir do nascimento: "15 dias", "8 meses", "2 anos e 3 meses". */
export function formatAge(birthDate: string | null, today = new Date()): string {
  if (!birthDate) return '—'
  const [y, m, d] = birthDate.split('-').map(Number)
  const birth = new Date(y, m - 1, d)
  let months = (today.getFullYear() - birth.getFullYear()) * 12 + (today.getMonth() - birth.getMonth())
  if (today.getDate() < birth.getDate()) months -= 1
  if (months < 1) {
    const days = Math.max(0, Math.floor((today.getTime() - birth.getTime()) / 86_400_000))
    return days === 1 ? '1 dia' : `${days} dias`
  }
  const years = Math.floor(months / 12)
  const rest = months % 12
  const monthsText = rest === 1 ? '1 mês' : `${rest} meses`
  if (years === 0) return monthsText
  const yearsText = years === 1 ? '1 ano' : `${years} anos`
  return rest === 0 ? yearsText : `${yearsText} e ${monthsText}`
}

/** Linhas coladas de uma planilha ou digitadas: "101", "101; Mimosa", "101, Mimosa" -> brinco e nome. */
export function parseTagList(text: string): { tag: string; name: string | null }[] {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [tag, ...rest] = line.split(/[;,\t]/)
      const name = rest.join(' ').trim()
      return { tag: tag.trim(), name: name || null }
    })
    .filter((row) => row.tag)
}
