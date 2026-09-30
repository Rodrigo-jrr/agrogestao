# AgroGestão — Arquitetura (Fase 0 + Esqueleto MVP)

Este documento registra as decisões técnicas concretas tomadas para a primeira versão do projeto, complementando a visão de produto em [AgroGestao_documento_base.md](AgroGestao_documento_base.md).

## Stack

| Camada     | Tecnologia |
|------------|------------|
| Backend    | Java 21, Spring Boot 4.1 (Jackson 3), Spring Web, Spring Security, Spring Data JPA, Flyway, Maven |
| Banco      | MySQL 8 |
| Frontend   | React 18, TypeScript, Vite, React Router, Tailwind CSS |
| Autenticação | JWT stateless (access token) |
| Infra local | Docker Compose (MySQL) |

## Multi-tenancy

Schema único de banco de dados. Isolamento feito na camada de aplicação através de duas entidades de fronteira:

```
User ──< OrganizationMember >── Organization ──< Property >── PropertyMember >── User
```

- **Organization**: conta/tenant. Um usuário pode pertencer a várias organizações.
- **Property**: propriedade rural, pertence a uma `Organization` (coluna `organization_id`).
- **PropertyMember**: vincula um usuário a uma propriedade com um papel (`OWNER`, `MANAGER`, `WORKER`, `VIEWER`).

Toda entidade de negócio (financeiro, estoque, produção, compras, vendas, atividades) carrega uma coluna `property_id`. O acesso é resolvido por request:

1. O cliente envia o header `X-Property-Id` além do JWT.
2. `TenantContext` (request-scoped) valida que o usuário autenticado possui um `PropertyMember` para aquela propriedade e expõe `getCurrentPropertyId()` para os services.
3. Repositórios/queries de negócio sempre filtram por `property_id = TenantContext.getCurrentPropertyId()` — nenhuma query de negócio deve ser feita sem esse filtro.

Essa abordagem evita a complexidade operacional de schema-por-tenant nesta fase, mantendo a porta aberta para migrar caso o volume de dados/propriedades justifique isolamento mais forte no futuro (seção 37 do documento-base).

## Pacotes do backend (`com.agrogestao`)

Implementados nesta fase:

```
common          - BaseEntity, tratamento de erro, paginação
auth            - usuário, login/registro, JWT
organizations   - Organization, OrganizationMember
properties      - Property, PropertyMember, TenantContext
activities      - Activity
finance         - CostCenter, FinancialTransaction, AccountPayable, AccountReceivable
inventory       - Product, InventoryItem, InventoryMovement
purchases       - Supplier, Purchase, PurchaseItem
sales           - Customer, Sale, SaleItem
production      - ProductionRecord
dashboard       - agregações somente leitura
audit           - AuditLog (usado como exemplo em finance)
```

Reservados para fases futuras (diretório criado, sem código — ver seções 49–51 do documento-base):

```
livestock, crops, fields, machinery, maintenance,
tasks, documents, notifications, analytics, ai
```

## Fluxos integrados (seção 6.4 / 42 do documento-base)

- **Compra** → gera `InventoryMovement` (IN) + `FinancialTransaction`/`AccountPayable`, vinculados ao `CostCenter` da atividade.
- **Venda** → gera `InventoryMovement` (OUT) + `FinancialTransaction`/`AccountReceivable`.
- **Dashboard** → agrega `FinancialTransaction` (receita/despesa/resultado do período), `ProductionRecord` recentes e alertas de estoque (produtos com saldo abaixo do mínimo).

## Segurança

- Senhas com `BCrypt`.
- JWT assinado (HS256), expiração curta, validado em `JwtAuthenticationFilter`.
- Toda rota exceto `/api/auth/**` exige autenticação; rotas de negócio exigem também `PropertyMember` válido via `TenantContext`.
- Auditoria (`AuditLog`, seção 39 do documento-base) registrada em todas as operações de escrita — criação/edição/exclusão em todos os módulos, baixas de contas, cancelamentos de compras/vendas e mudanças na equipe — com propriedade, usuário, entidade, ação e um resumo. Use `AuditService.record(entidade, id, ação, detalhes)` ao criar novos serviços.
- Papéis por propriedade: `OWNER` (tudo, inclusive gerenciar proprietários), `MANAGER` (exclui e gerencia equipe), `WORKER` (cria/edita), `VIEWER` (só leitura). A propriedade sempre mantém ao menos um `OWNER`.
- Compras/vendas guardam o lançamento financeiro gerado (`financial_transaction_id`); o cancelamento estorna o estoque e remove o lançamento. Lançamentos gerados por compra/venda/baixa de conta não podem ser editados ou excluídos diretamente.
- Painel da plataforma (`/admin`, API `/api/admin/**`): somente leitura, visão de todas as organizações, propriedades e usuários. Acesso definido por `PLATFORM_ADMIN_EMAILS` e verificado no servidor a cada requisição (regra de rota + `@PreAuthorize`, via `PlatformAdminService`); nada disso vai no JWT. Não dá acesso aos dados internos das propriedades. Cada acesso é auditado (`AuditAction.VIEW`).
- Respostas de erro de segurança: sem token/token inválido → 401; sem permissão → 403 em JSON (sem `sendError`, que viraria 401 no redirecionamento para `/error`).
- Conta: o cadastro envia um link de confirmação de e-mail (24h); "esqueci minha senha" envia um link de redefinição (1h). Links de uso único; no banco fica só o SHA-256 do token (`user_tokens`). A resposta de "esqueci minha senha" é igual exista ou não a conta, com limite de 3 envios/hora. Redefinir a senha confirma o e-mail e invalida JWTs emitidos antes (`users.password_changed_at`). O app funciona sem e-mail confirmado (aviso não bloqueante); o painel de admin exige confirmação.
- Testes de integração em `backend/src/test` (H2 em modo MySQL + migrations Flyway), rodados no CI.

## Como rodar

Ver [README.md](../README.md) na raiz do projeto.

## Mapeamento com o roadmap do documento-base

Esta entrega cobre a **Fase 0 (Fundação)** e o núcleo da **Fase 1 (Gestão básica)** da seção 59. Pecuária, agricultura, máquinas, tarefas, IA, WhatsApp/voz e IoT (Fases 2–5) ficam para etapas futuras, com os pacotes já reservados na estrutura do backend.
