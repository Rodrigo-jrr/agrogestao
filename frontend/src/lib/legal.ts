/**
 * Versão dos termos de uso + política de privacidade. Precisa ser igual a TermsService.CURRENT_VERSION no
 * backend (o cadastro é recusado se não for). Ao mudar o texto de /termos ou /privacidade, troque a versão
 * nos dois lugares: quem aceitou a anterior verá o pedido para aceitar a nova.
 */
export const TERMS_VERSION = '2026-09-30'
export const TERMS_UPDATED_AT = '30 de setembro de 2026'

/**
 * Dados do responsável pelo AgroGestão. A PREENCHER antes da publicação (ver docs/AUDITORIA.md);
 * enquanto estiverem entre colchetes, as páginas mostram o campo destacado.
 */
export const LEGAL_ENTITY = {
  controller: '[razão social e CNPJ do responsável pelo AgroGestão]',
  address: '[endereço do responsável]',
  privacyEmail: '[e-mail de contato para privacidade]',
  forum: '[cidade e estado do foro]',
  backupRetention: '[prazo de retenção dos backups, ex.: 30 dias]',
}
