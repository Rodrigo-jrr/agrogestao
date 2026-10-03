import type { CropHarvestDestination, CropOperationKind } from '../../api/types'

export const cardClass = 'rounded-2xl border border-terra/10 bg-white p-4 shadow-sm'
export const labelClass = 'mb-1 block text-xs font-medium text-terra/70'

export const cropOperationKindLabels: Record<CropOperationKind, string> = {
  PREPARO: 'Preparo',
  PLANTIO: 'Plantio',
  ADUBACAO: 'Adubação',
  PULVERIZACAO: 'Pulverização',
}

export const cropHarvestDestinationLabels: Record<CropHarvestDestination, string> = {
  ESTOQUE: 'Estoque',
  VENDA: 'Venda',
}
