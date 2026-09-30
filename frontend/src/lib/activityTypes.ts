/** Tipos de atividade da propriedade: código gravado na API e nome mostrado na tela. */
export const activityTypes = [
  { code: 'PECUARIA', label: 'Pecuária' },
  { code: 'LEITE', label: 'Leite' },
  { code: 'AGRICULTURA', label: 'Agricultura' },
  { code: 'HORTA', label: 'Horta' },
  { code: 'AVICULTURA', label: 'Avicultura' },
  { code: 'PISCICULTURA', label: 'Piscicultura' },
  { code: 'OUTRA', label: 'Outra' },
] as const

export function activityTypeLabel(code: string) {
  return activityTypes.find((t) => t.code === code)?.label ?? code.charAt(0) + code.slice(1).toLowerCase()
}

/** Código a partir do nome ("Pecuária" -> "PECUARIA"); nomes livres viram OUTRA. */
export function activityTypeCode(name: string) {
  const normalized = name.normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase()
  return activityTypes.find((t) => t.code === normalized)?.code ?? 'OUTRA'
}
