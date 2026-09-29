# AgroGestão

Plataforma de gestão para pequenas propriedades rurais. Veja a visão completa do produto em [docs/AgroGestao_documento_base.md](docs/AgroGestao_documento_base.md) e as decisões técnicas em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Esta é a base do projeto: fundação (autenticação, multi-propriedade, segurança) + esqueleto funcional do MVP (financeiro, estoque, produção, compras, vendas, atividades).

## Stack

- **Backend:** Java 21, Spring Boot 3.3, Maven
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS
- **Banco:** MySQL 8

## Pré-requisitos

- Java 21 (`java -version`)
- Maven (`brew install maven`, caso não tenha)
- Node.js 20+ e npm
- Docker (para rodar o MySQL localmente via `docker compose`)

## Subindo o banco de dados

```bash
docker compose up -d
```

Isso sobe um MySQL 8 na porta **3307** do host (mapeada para não colidir com outros containers MySQL que já estejam usando a porta padrão 3306). Usuário/senha/banco: `agrogestao` / `agrogestao` / `agrogestao`.

Se preferir usar outra porta ou um MySQL já existente, ajuste as variáveis de ambiente `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` ao rodar o backend.

## Rodando o backend

```bash
cd backend
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente na primeira execução.

## Rodando o frontend

```bash
cd frontend
npm install
npm run dev
```

A aplicação sobe em `http://localhost:5173` e já está configurada com proxy para `/api` → `http://localhost:8080` (ver `vite.config.ts`).

## Fluxo de uso

1. Criar conta em `/register`.
2. Completar o onboarding (`/onboarding`): nome da propriedade, localização, área e atividades.
3. Usar o dashboard e os módulos: Financeiro (com fluxo de caixa), Estoque, Produção, Compras, Vendas, Atividades.
4. Convidar a equipe em **Equipe**: a pessoa cria a conta em `/register` e o proprietário/gerente a adiciona pelo e-mail, escolhendo o papel (Proprietário, Gerente, Colaborador ou Visualizador).

## Produção

Todas as variáveis estão documentadas em [.env.example](.env.example).

- **Backend:** gere o jar com `mvn -f backend/pom.xml -B package` e rode com `SPRING_PROFILES_ACTIVE=prod`. Nesse perfil não há valores padrão: a aplicação **se recusa a subir** se `JWT_SECRET` estiver ausente, curto (< 32 bytes) ou igual ao de desenvolvimento, se `CORS_ALLOWED_ORIGINS` apontar para localhost, ou se faltarem as variáveis do banco. As migrations do Flyway rodam na inicialização.
- **Frontend:** `npm --prefix frontend run build` gera `frontend/dist/` (arquivos estáticos). O padrão é chamar a API em `/api` no mesmo domínio (configure o reverse proxy para encaminhar `/api` ao backend); para API em outro domínio, defina `VITE_API_BASE_URL` no build — ver [frontend/.env.example](frontend/.env.example).
- **CI:** [.github/workflows/ci.yml](.github/workflows/ci.yml) roda `mvn verify` no backend e lint + build no frontend a cada push/PR para `main`.

> **Aviso (macOS/iCloud):** se o projeto estiver em uma pasta sincronizada com o iCloud (ex.: Documentos), deixe as dependências em `frontend/node_modules.nosync` com um link `node_modules -> node_modules.nosync`; caso contrário o iCloud pode tirar os arquivos do disco e travar `npm`/`tsc`/`git`.

## Testando a API diretamente

```bash
# registrar
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria","email":"maria@example.com","password":"senha1234"}'

# as demais rotas de negócio exigem o header X-Property-Id além do Authorization Bearer <token>,
# obtido a partir da resposta de POST /api/onboarding
```

## Estrutura do repositório

```
agro/
├── docs/               documentação de produto e arquitetura
├── backend/            API Spring Boot (com.agrogestao.*)
├── frontend/           SPA React + Vite
└── docker-compose.yml  MySQL para desenvolvimento local
```
