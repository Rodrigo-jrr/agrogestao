# Próximos passos do AgroGestão

Este arquivo tem duas partes: as **instruções para quem continuar o desenvolvimento** (pessoa ou agente) e a
**lista do que falta implementar**, em ordem. Mantenha-o atualizado: ao concluir uma entrega, marque-a aqui.

---

## Parte 1 — Instruções para o agente

Você vai continuar o desenvolvimento do **AgroGestão**, um SaaS de gestão para pequenas propriedades rurais.
Fale com o usuário **em português**. Antes de qualquer código, **verifique o projeto** e depois siga a Parte 2
("Implementações pendentes"), **uma entrega por vez**.

### 1. Verifique o projeto primeiro
- Pasta: `/Users/rodrigorezende/Documents/Projeto Saas Paulo/agro`.
- Repositório privado: https://github.com/Rodrigo-jrr/agrogestao (branch `main`).
- Leia nesta ordem:
  - `README.md`;
  - `docs/ARCHITECTURE.md`, com as regras de negócio e técnicas e o que já existe;
  - `docs/AUDITORIA.md`, com o checklist concluído e os itens da fase de publicação;
  - `docs/AgroGestao_documento_base.md`, a visão do produto: seções 12–14 (pecuária), 15 (agricultura) e 49 (segunda fase).
- Rode `git log --oneline -15` e `git status` para confirmar o ponto atual. O último merge de funcionalidade
  foi a "Pecuária, entrega 3: reprodução e indicadores" (PR #7).
- Rode os testes antes de mexer, para ter a linha de base:
  - backend: `mvn -f backend/pom.xml test`, com 145 testes no H2;
  - frontend: `npm --prefix frontend test` (40 testes) e `npm --prefix frontend run lint`;
  - ponta a ponta: `docker compose up -d` na raiz e depois `npm --prefix frontend run test:e2e` (13 testes).

### 2. Stack
- **Backend:** Spring Boot 4.1 e Java 21 (Jackson 3 = `tools.jackson`), com MySQL 8 do docker na porta 3307.
  As migrações Flyway estão em `backend/src/main/resources/db/migration`, e **a próxima é a V13**.
- **Frontend:** React 19, TypeScript, Vite e Tailwind v4.
- **E-mails locais:** Mailpit (SMTP na porta 1025, interface em http://localhost:8025).

### 3. Ambiente (macOS + iCloud)
- O projeto está numa pasta sincronizada com o iCloud. Por isso:
  - `frontend/node_modules` é um link para `node_modules.nosync`, e `backend/target` é um link para `target.nosync`;
  - **depois de `npm install` ou `npm ci`**, o npm troca o link por uma pasta comum. Restaure com
    `rm -rf node_modules.nosync && mv node_modules node_modules.nosync && ln -s node_modules.nosync node_modules`.
- Servidores de desenvolvimento:
  - backend: `mvn -q spring-boot:run` em `backend/`, porta 8080;
  - frontend: `npm run dev` em `frontend/`, porta 5173, com proxy de `/api`.
- Os testes de ponta a ponta sobem servidores próprios nas portas 8081 e 5174, com cache do Vite separado.

### 4. Fluxo de trabalho (obrigatório)
- **Nunca envie direto para a `main`.** O hook `.githooks/pre-push` bloqueia isso (ative com
  `git config core.hooksPath .githooks`).
- Para cada entrega:
  1. `git switch main && git pull`, depois `git switch -c feat/<nome>`;
  2. implemente e escreva os testes;
  3. faça o commit (mensagem em português, explicando o que mudou e por quê);
  4. `git push -u origin feat/<nome>` e `gh pr create`;
  5. espere os **3 jobs do CI** passarem: Backend (Java 21), Frontend (Node 22) e Ponta a ponta (Playwright);
  6. **pare e mostre ao usuário**;
  7. só junte com `gh pr merge --squash --delete-branch` quando ele pedir.
- Uma entrega por PR. Antes de começar uma entrega com decisões de produto em aberto, pergunte ao usuário
  (ex.: quais espécies, se integra com o financeiro).

### 5. Padrões que o código já segue (mantenha)
- **Multi-tenancy:**
  - toda tabela de negócio tem `property_id` e a propriedade vem do header `X-Property-Id` (`TenantContext`);
  - escrita exige `requireWriteAccess()`; excluir, cancelar ou desfazer exige `requireManageAccess()`;
  - papéis: OWNER, MANAGER, WORKER e VIEWER.
- **Toda escrita registra auditoria** com `AuditService.record(...)`, em texto legível. Valores em reais via `Money.brl`.
- **Tabela nova com `property_id`:**
  - inclua-a em `PropertyDataEraser`, na ordem das chaves estrangeiras;
  - inclua-a em `DataExportService.PROPERTY_SECTIONS`;
  - os testes `PropertyManagementTest` e `DataExportTest` falham se ela ficar de fora;
  - acrescente dados dela no `fillWithData` do teste de exclusão.
- **Operações que mexem em dinheiro** seguem Compras/Vendas: à vista gera lançamento (`registerFromOperation`),
  a prazo gera parcelas, e cancelar desfaz tudo.
- **Estoque** usa custo médio ponderado (`InventoryMovementService.registerMovement`).
- **Armadilhas já encontradas:**
  - em DTO, use `Boolean` (e não `boolean`) para campos opcionais: o Jackson 3 recusa primitivo ausente;
  - datas e horas lidas por JDBC direto: use `getTimestamp`, porque o banco guarda em UTC.
- **Frontend:**
  - todo campo tem rótulo visível;
  - mensagens de resultado usam `FormMessage`, confirmações usam `useConfirm`, listas grandes usam `usePaged`
    com `Pagination`;
  - cards dentro de grid levam `min-w-0`, e tabelas largas ficam em `relative overflow-x-auto`;
  - toda tela nova é conferida a **375px sem rolagem lateral**;
  - textos da interface em português, diretos e no ponto de vista de quem usa.
- **Testes por entrega:** testes de API no backend (regras, papéis, isolamento entre propriedades), testes de
  componentes ou funções no frontend quando houver lógica, e um fluxo de ponta a ponta para a funcionalidade
  principal.
- **Verifique no navegador** antes de abrir o PR: crie uma conta de teste pelo cadastro local (aceitando os
  termos) e faça o fluxo de verdade.

---

## Parte 2 — Implementações pendentes

Faça em ordem, **uma entrega por PR**, parando para o usuário revisar entre elas.

### 1. Pecuária — Entrega 3: reprodução e indicadores — concluída (01/10/2026)
Base: documento-base, seções 14 e 43. PRs #5 (pré-requisito: catálogo de espécies), #7 (reprodução e
indicadores). Decisões confirmadas com o usuário e aplicadas: reprodução só para espécies com
`allowsIndividual = true`; diagnóstico de prenhez obrigatório antes do parto; custos gerais da atividade
rateados igualmente por cabeça (mesmo padrão de `HealthService`).

- **Eventos reprodutivos** por fêmea: cio, cobertura (touro/reprodutor), inseminação (sêmen), diagnóstico de
  prenhez (positivo/negativo, data prevista do parto), parto e aborto.
- **Parto** que já cadastra a cria (ou crias) com mãe, pai, nascimento e lote, e encerra a gestação.
- **Histórico reprodutivo** na ficha da fêmea, situação atual (vazia, coberta, prenhe) e previsão de parto.
- **Alertas** na agenda e no painel: partos previstos nos próximos dias, fêmeas para diagnóstico e reforços.
- **Indicadores** numa aba ou painel da pecuária, com filtro de período e lote:
  - taxa de prenhez, natalidade e mortalidade;
  - intervalo entre partos;
  - GMD médio por lote;
  - custo por animal e por kg: compra + sanidade rateada + parte dos lançamentos ligados à atividade/centro de custo.

### 2. Agricultura (Fase 2, documento-base seções 15 e 49) (próxima)
Pacotes `crops` e `fields` existem vazios.
- **Talhões:** nome, área (ha), localização e solo.
- **Culturas e safras (ciclos):** cultura, variedade, talhão, plantio e colheita previstos.
- **Operações agrícolas:** preparo, plantio, adubação, pulverização. Insumos saem do estoque (como a sanidade),
  com custo, responsável e máquina usada.
- **Colheita:** quantidade, unidade e destino (estoque, venda).
- **Indicadores:** custo/ha, produção/ha, receita/ha e margem/ha por safra e talhão.
- Antes de começar, confirme com o usuário o escopo e a divisão em entregas.

### 3. Máquinas e manutenção (pacotes `machinery`, `maintenance`)
- Cadastro de máquinas e implementos, com horímetro ou hodômetro.
- Uso por operação (liga à agricultura).
- Manutenção preventiva por horas ou datas, com alertas, e corretiva com custo e peças do estoque.
- Custo por hora de máquina.

### 4. Tarefas (pacote `tasks`)
- Tarefas por propriedade: título, responsável (membro da equipe), prazo, status e ligação opcional a um animal,
  lote, talhão ou máquina.
- Lista "minhas tarefas" e alertas de atraso.

### 5. Dívidas técnicas (podem entrar junto de outra entrega)
- **Pacote do frontend acima de 500 kB:** carregar páginas sob demanda (`React.lazy` por rota).
- **Seletores limitados a 200 animais:** mãe, venda, sanidade e pesagem "sem lote" usam `size: 200`. Trocar por
  busca paginada quando o rebanho for grande.
- **Excluir uma atividade usada por um lote** devolve um aviso genérico ("registros relacionados"). Explicar o motivo.
- **Avisos do oxlint** (`set-state-in-effect`) em algumas páginas antigas.

### 6. Antes de publicar (não fazer sem o usuário pedir)
Lista completa em `docs/AUDITORIA.md`, seção "Para a fase de publicação".
- Preencher os dados do responsável em `frontend/src/lib/legal.ts` (razão social/CNPJ, endereço, e-mail de
  privacidade, foro, prazo dos backups) e levar os termos e a política a um advogado. Ao mudar o texto, trocar
  a versão em `legal.ts` e em `TermsService.CURRENT_VERSION`.
- E-mail real (SMTP), domínio, HTTPS, backups, CSP no servidor do frontend (ou cookie `httpOnly`) e Redis para
  os limites de tentativas se houver mais de uma instância.
- Opcional: GitHub Pro para proteger a `main` no servidor.

### 7. Limpeza local (perguntar antes)
- Contas de teste `@teste.dev` e `@e2e.teste.dev` no MySQL local.
- Pasta `frontend/node_modules_old_icloud`.
- Sugestão ao usuário: mover o projeto para fora da pasta Documentos sincronizada com o iCloud.
