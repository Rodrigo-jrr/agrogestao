import type { MeasurementType } from '../../api/types'

export const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'
export const labelClass = 'mb-1 block text-xs font-medium text-terra/70'

export const measurementTypeLabels: Record<MeasurementType, string> = {
  HOROMETRO: 'Horímetro',
  ODOMETRO: 'Hodômetro',
}
