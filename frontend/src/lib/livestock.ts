import type { AnimalEvent, AnimalEventType, AnimalOrigin, AnimalStatus, ExitType, HealthKind, Sex, Species } from '../api/types'

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
  PESAGEM: 'Pesagem',
  SANIDADE: 'Sanidade',
}

export const healthKindLabels: Record<HealthKind, string> = {
  VACINA: 'Vacina',
  MEDICAMENTO: 'Medicamento',
  PROCEDIMENTO: 'Procedimento',
}

/** "vacina Raiva", mas só "Vacina Aftosa" quando o nome já diz o tipo. */
export function healthTitle(kind: HealthKind, productName: string, capitalize = false): string {
  const label = healthKindLabels[kind]
  if (productName.toLowerCase().startsWith(label.toLowerCase())) return productName
  return `${capitalize ? label : label.toLowerCase()} ${productName}`
}

export interface WeightPoint {
  date: string
  weight: number
  /** Ganho médio diário (kg/dia) desde o peso anterior; null no primeiro. */
  dailyGain: number | null
}

/** Pesos do animal (entrada e pesagens) em ordem de data, com o ganho médio diário entre um e outro. */
export function weightHistory(events: AnimalEvent[]): WeightPoint[] {
  const points = events
    .filter((e) => e.weight != null && e.animalId != null)
    .map((e) => ({ date: e.date, weight: e.weight as number, id: e.id }))
    .sort((a, b) => (a.date === b.date ? a.id - b.id : a.date < b.date ? -1 : 1))
  return points.map((p, i) => {
    const previous = points[i - 1]
    const days = previous ? daysBetween(previous.date, p.date) : 0
    return {
      date: p.date,
      weight: p.weight,
      dailyGain: previous && days > 0 ? Math.round(((p.weight - previous.weight) / days) * 1000) / 1000 : null,
    }
  })
}

/** Ganho médio diário entre o primeiro e o último peso (null com menos de dois pesos em datas diferentes). */
export function overallDailyGain(points: WeightPoint[]): number | null {
  if (points.length < 2) return null
  const first = points[0]
  const last = points[points.length - 1]
  const days = daysBetween(first.date, last.date)
  return days > 0 ? Math.round(((last.weight - first.weight) / days) * 1000) / 1000 : null
}

export function daysBetween(from: string, to: string): number {
  const toUtc = (iso: string) => {
    const [y, m, d] = iso.split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.round((toUtc(to) - toUtc(from)) / 86_400_000)
}

/** "hoje", "amanhã", "em 3 dias", "há 2 dias". */
export function relativeDay(iso: string, todayIso: string): string {
  const days = daysBetween(todayIso, iso)
  if (days === 0) return 'hoje'
  if (days === 1) return 'amanhã'
  if (days === -1) return 'ontem'
  return days > 0 ? `em ${days} dias` : `há ${-days} dias`
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
