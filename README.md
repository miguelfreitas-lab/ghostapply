# GhostApply

A full-stack platform for managing resumes,
tracking job opportunities, and analyzing
candidate-to-job compatibility.

## Stack

- Frontend: React, TypeScript, Vite
- Backend: Java, Spring Boot
- Database: PostgreSQL
- Version control: Git, GitHub

## Executar localmente

Requisitos: Java 21, Node.js compatível com o Vite instalado e PostgreSQL.
Mantenha a conexão configurada em `backend/src/main/resources/application.properties`;
não versione senhas ou outros segredos.

### Preparar o banco

O JPA permanece com `spring.jpa.hibernate.ddl-auto=validate`: ele valida o esquema,
mas não cria nem apaga tabelas. Antes de iniciar o backend, aplique **uma vez**
`backend/src/main/resources/db/migration/V1__criar_candidaturas.sql` no banco já
configurado. Use o editor SQL do pgAdmin ou, na raiz do repositório:

```powershell
psql -h localhost -U SEU_USUARIO -d SEU_BANCO -W -v ON_ERROR_STOP=1 -f backend/src/main/resources/db/migration/V1__criar_candidaturas.sql
```

Substitua usuário e banco pelos valores locais; a senha será solicitada sem entrar
no comando. O script cria somente a tabela `candidaturas`, em uma transação, sem
apagar dados. Não há execução automática nem controle automático de versões de
migrações: registre a aplicação de V1 no seu ambiente e não execute novamente.
Se a tabela já existir, confira o esquema antes de aplicar qualquer alteração;
não remova a tabela para reaplicar a migração.

### Iniciar

Em um terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Se o wrapper apresentar erro no PowerShell, use um Maven instalado:
`mvn spring-boot:run` (Java 21).

Em outro terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Com dependências já instaladas, basta `npm run dev`. Acesse a URL exibida pelo Vite
(normalmente `http://localhost:5173`). O proxy de desenvolvimento encaminha `/api`
para `http://localhost:8080`. Para hospedar o build de produção, configure o servidor
HTTP para encaminhar `/api` ao backend na mesma origem.

### API de candidaturas

| Método | Caminho | Resposta |
| --- | --- | --- |
| GET | `/api/candidaturas` | 200, lista por criação decrescente |
| GET | `/api/candidaturas/{id}` | 200 ou 404 |
| POST | `/api/candidaturas` | 201 com Location |
| PUT | `/api/candidaturas/{id}` | 200 ou 404 |
| DELETE | `/api/candidaturas/{id}` | 204 ou 404 |

Exemplo de corpo para POST/PUT:

```json
{
  "empresa": "Empresa Exemplo",
  "titulo": "Desenvolvedor Java Júnior",
  "urlVaga": "https://example.com/vaga",
  "modalidade": "REMOTO",
  "status": "SALVA"
}
```

Empresa e título são obrigatórios e aceitam até 200 caracteres. URL é opcional
(null ou vazia), aceita HTTP/HTTPS e até 2048 caracteres. Modalidade e status são
obrigatórios. Modalidades: REMOTO, HIBRIDO, PRESENCIAL. Status: SALVA, INSCRITO,
ENTREVISTA, OFERTA, REJEITADO. ID e `criadaEm` são gerados pelo servidor e preservados
na edição. Dados inválidos retornam 400; erros tratados contêm `message`.
O endpoint `GET /api/health` permanece disponível.

### Verificações

```powershell
cd frontend
npm run build
npm run lint
```

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

Os testes `CandidaturaServiceTest` e `CandidaturaControllerTest` usam mocks e não
dependem do banco. Para executá-los isoladamente:
`.\mvnw.cmd "-Dtest=CandidaturaServiceTest,CandidaturaControllerTest" test`.
O teste de contexto foi movido para `BackendApplicationIT` e usa exclusivamente
um PostgreSQL descartável do Testcontainers, assim como os demais testes `*IT`.

### Testes PostgreSQL isolados (v0.4)

Requisitos: Java 21, Maven (ou wrapper) e Docker Desktop com contêineres Linux
funcionando. O primeiro uso precisa baixar a imagem `postgres:18.0-alpine` e as
dependências Maven. A versão do Testcontainers é gerenciada pelo Spring Boot;
apenas o módulo `testcontainers-postgresql` foi adicionado, com escopo `test`.

Execute a partir de `backend`:

```powershell
# Testes rápidos: não iniciam Docker e não acessam PostgreSQL.
.\mvnw.cmd test

# Testes rápidos + integração real (Maven Failsafe).
.\mvnw.cmd -Pintegration verify

# Somente uma classe de integração, além dos testes rápidos.
.\mvnw.cmd -Pintegration "-Dit.test=CandidaturaPersistenciaIT" verify
```

Se necessário, substitua `.\mvnw.cmd` por `mvn`. Em Linux/macOS, use `./mvnw`.
Não use apenas a fase `integration-test`: `verify` também verifica os resultados
do Failsafe. Relatórios: `target/surefire-reports` e `target/failsafe-reports`.
Sem Docker disponível, a integração falha; ela não é pulada silenciosamente.

O isolamento funciona assim:

- `PostgresIntegrationTest` aplica o perfil `integration` e importa a configuração
  exclusiva de testes. Todos os testes com acesso real ao banco herdam essa base.
- `PostgresTestConfiguration` cria o contêiner sem reutilização, com porta dinâmica,
  banco e usuário `ghostapply_it` e senha aleatória. Nenhum volume do banco local é
  montado. O contêiner é parado ao encerrar seu contexto Spring.
- A V1 original é aplicada pelo Testcontainers antes da criação do `DataSource` e
  da validação Hibernate. Não há cópia da migração nem `create-drop`.
- O `DataSource` é construído diretamente com os dados do contêiner em execução;
  não lê `spring.datasource.*` nem `DB_PASSWORD`. Mesmo propriedades externas de
  conexão não o redirecionam ao banco de desenvolvimento. O perfil ainda contém
  uma URL sentinela inválida para impedir fallback se a configuração for omitida.
- Os testes de persistência usam service e repository reais, transações confirmadas,
  releitura em outras transações e consultas JDBC. Removem somente os registros que
  criaram nesse banco descartável; não dependem de IDs fixos ou da ordem dos testes.

Cobertura de integração: contexto/conexão isolada, diagnóstico de esquema/HTTP,
ciclo CRUD completo, geração e preservação da data, enums de modalidade e status
e ordenação por data/ID. Os testes rápidos também verificam que a configuração
recusa um contêiner parado e não abre conexão ao apenas montar um `DataSource`.

Não é necessário iniciar o backend, preparar o banco de desenvolvimento ou fornecer
sua senha. Não existe opção de usar um PostgreSQL externo como alternativa nesses
testes. Esta etapa não adiciona GitHub Actions.

Validação manual: cadastrar, recarregar a página para conferir persistência, editar,
excluir, cancelar uma exclusão e desligar o backend para verificar o erro de conexão.
