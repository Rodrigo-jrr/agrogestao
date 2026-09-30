import type { PropertyRole } from '../api/types'

export const roleLabels: Record<PropertyRole, string> = {
  OWNER: 'Proprietário',
  MANAGER: 'Gerente',
  WORKER: 'Colaborador',
  VIEWER: 'Visualizador',
}

export const roleDescriptions: Record<PropertyRole, string> = {
  OWNER: 'Acesso total, inclusive gerenciar outros proprietários e excluir a propriedade.',
  MANAGER: 'Registra, edita e exclui dados; gerencia a equipe.',
  WORKER: 'Registra e edita o dia a dia; não exclui.',
  VIEWER: 'Somente consulta.',
}
