# Auditoria do AgroGestão — 30/09/2026

Objetivo: deixar o app "100%" antes de publicar. Cada item foi verificado no código e, quando possível,
reproduzido no servidor local. Marque `[x]` ao concluir e anote o commit.

## Bloco 1 — Correções e segurança (bugs reais) — concluído em 30/09 (`0e24d8a` e seguinte)

- [x] **1.1 Erros do usuário viram "erro interno" (500).** JSON malformado, valor de lista inválido
  (ex.: tipo de lançamento "XYZ"), id não numérico na URL e rota inexistente respondem 500 em vez de
  400/404, e ainda aparecem no log como erro do servidor. *Reproduzido.*
- [x] **1.2 Textos longos e valores enormes geram mensagem enganosa.** Nenhum campo tem limite de
  tamanho/dígitos na validação: um nome com 300 caracteres ou um valor de 20 dígitos cai no banco e volta
  como "existem registros relacionados" (409). *Reproduzido.*
- [x] **1.3 Números negativos aceitos** onde não fazem sentido (área da atividade, custo unitário).
  *Reproduzido (área -50 aceita).*
- [x] **1.4 Senha longa truncada em silêncio.** O cadastro aceita senha de 100 caracteres, mas o BCrypt só
  considera os primeiros 72 bytes. (A redefinição de senha já limita a 72.) *Reproduzido.*
- [x] **1.5 Login sem limite de tentativas.** 20 tentativas erradas seguidas são aceitas sem bloqueio nem
  atraso: permite adivinhar senhas. *Reproduzido.*
- [x] **1.6 Spring Boot 3.3.4 fora do período de suporte.** Atualizado para 4.1.1 (Jackson 3, Flyway como
  módulo, `@MockitoBean`). Aproveitado para remover o usuário padrão que o Spring criava com senha impressa no log.

## Bloco 2 — Funcionalidades essenciais do dia a dia — concluído em 30/09

- [x] **2.1 Lançamentos presos no mês atual.** A tela do Financeiro não tem filtro de período: não dá para
  ver os lançamentos do mês passado.
- [x] **2.2 Dashboard fixo no mês atual**, sem escolher período.
- [x] **2.3 Estoque sem histórico e sem valor.** O histórico de entradas/saídas existe na API mas não aparece
  na tela; o custo médio (`average_cost`) nunca é calculado, então não há "valor do estoque".
- [x] **2.4 Compras e vendas só à vista.** Toda compra/venda vira lançamento pago na hora; falta "a prazo"
  (gerar conta a pagar/receber com vencimento). Também não dá para ver os itens de uma compra/venda.
- [x] **2.5 Produto usado não pode ser excluído nem arquivado.** Excluir um produto com movimentação dá
  erro genérico; falta "arquivar" (some das listas, mantém o histórico).
- [x] **2.6 Nenhuma lista tem paginação ou busca.** Com meses de uso as telas ficam enormes e lentas.

## Bloco 3 — Usabilidade e acessibilidade — concluído em 30/09

- [x] **3.1 Datas no formato técnico** ("2026-09-10") em 6 tabelas; números sem formato brasileiro
  ("1500.5" em vez de "1.500,5").
- [x] **3.2 Formulários sem rótulo:** 8 telas usam só o texto de exemplo (placeholder) como rótulo; ele some
  ao digitar e leitores de tela não o anunciam.
- [x] **3.3 Sem indicação de carregamento** nas telas de cadastro: aparece "Nenhum registro" antes dos
  dados chegarem.
- [x] **3.4 Confirmações com a janela nativa do navegador** (12 lugares). Trocar por um diálogo do app que
  explique a consequência (ex.: "o estoque será estornado").
- [x] **3.5 Menu no celular** abre sem fundo escurecido e não fecha ao tocar fora.
- [x] **3.6 Endereço inexistente** volta para o início sem aviso (falta página "não encontrada").
- [x] **3.7 Revisão responsiva tela a tela** (depois dos itens acima).

## Bloco 4 — Conta, equipe e controle

- [x] **4.1 Perfil:** trocar nome, e-mail (com nova confirmação) e senha estando logado.
- [x] **4.2 Propriedade:** editar nome/área/local e excluir.
- [x] **4.3 Convite da equipe por e-mail** (hoje a pessoa precisa criar conta antes).
- [x] **4.4 Tela de auditoria** para o proprietário ("quem fez o quê").

## Bloco 5 — LGPD

- [ ] **5.1 Termos de uso e política de privacidade** aceitos no cadastro (com registro da versão aceita).
- [ ] **5.2 Excluir minha conta** e **exportar meus dados**.

## Bloco 6 — Qualidade

- [ ] **6.1 Testes do frontend** (componentes) e **testes de ponta a ponta** no navegador dos fluxos
  principais.

## Para a fase de publicação (anotado, fora do escopo agora)

- Token de login no `localStorage` + página sem Content-Security-Policy: definir CSP no servidor que vai
  servir o frontend (ou migrar para cookie `httpOnly`).
- Serviço de e-mail real, domínio, HTTPS, backups — ver README, seção Produção.

## O que está bom (verificado)

- Isolamento entre propriedades e permissões por papel (testes automatizados + verificação manual).
- Painel de admin com duas barreiras independentes; 401/403 corretos; cabeçalhos de segurança na API.
- Recuperação de senha sem revelar contas, links de uso único com hash, sessões encerradas ao trocar senha.
- Fail-fast de configuração em produção.
