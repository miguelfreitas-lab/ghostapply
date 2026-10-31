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

**O banco de desenvolvimento existente ainda NÃO foi adotado pelo Flyway.**
Ele possui a V1 aplicada manualmente. Sua adoção exigirá conferência do esquema,
backup e uma operação de baseline explícita na versão 1, separada e expressamente
autorizada. Nenhum baseline, leitura ou migração desse banco foi executado nesta
etapa. Não inicie esta versão contra esse banco antes dessa adoção: a inicialização
deve ser recusada quando houver tabelas sem histórico Flyway.

O backend agora usa Flyway na inicialização. Em um banco vazio e autorizado, ele
executa a V1 original de `db/migration` e registra seu checksum em
`flyway_schema_history`. Em seguida o Hibernate valida o mapeamento com
`spring.jpa.hibernate.ddl-auto=validate`. Não reaplique a V1 manualmente e não
remova tabelas para resolver falhas. O conteúdo da V1 permanece inalterado.

As configurações mantêm `baseline-on-migrate=false`, `validate-on-migrate=true`,
`clean-disabled=true`, `fail-on-missing-locations=true` e `out-of-order=false`.
`spring.sql.init.mode=never` evita um segundo mecanismo de inicialização.
Não são executadas operações de limpeza ou reparo do histórico.

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
  exclusiva de testes para o CRUD e o contexto da aplicação. Os cenários de ciclo
  de vida Flyway usam outro contêiner exclusivo e schemas novos por cenário.
- `PostgresTestConfiguration` cria o contêiner sem reutilização, com porta dinâmica,
  banco e usuário `ghostapply_it` e senha aleatória. Nenhum volume do banco local é
  montado. O contêiner é parado ao encerrar seu contexto Spring.
- O contêiner inicia vazio. O Flyway do Spring Boot aplica a V1 original antes da
  validação Hibernate; não há `withInitScript` nem `create-drop`.
- O `DataSource` é construído diretamente com os dados do contêiner em execução;
  não lê `spring.datasource.*` nem `DB_PASSWORD`. Mesmo propriedades externas de
  conexão não o redirecionam ao banco de desenvolvimento. O perfil ainda contém
  uma URL sentinela inválida para impedir fallback se a configuração for omitida.
- O mesmo bean recebe `@FlywayDataSource`, garantindo prioridade sobre propriedades
  externas `spring.flyway.url/user/password`. Um teste verifica a identidade entre
  o DataSource do Flyway e o do contêiner; os cenários também usam propriedades
  JDBC externas inválidas de propósito.
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

### Validação do Flyway exclusivamente em contêineres

O Spring Boot 4.1.1 gerencia a versão do Flyway. O projeto declara
`spring-boot-starter-flyway` e `flyway-database-postgresql`, sem versões próprias.
Os testes rápidos sem DataSource mantêm Flyway desabilitado somente nesse contexto.

Além do CRUD real, `FlywayLifecycleIT` verifica:

- Banco vazio migrado pela V1 original e histórico com checksum.
- Reinicialização sem reaplicar a V1 ou alterar seu histórico.
- Legado simulado no contêiner, com V1 aplicada manualmente, registro inserido e
  baseline explícito 1 preservando todos os campos. Esse é o único cenário de
  baseline; `baseline-on-migrate` continua sempre falso.
- Recusa de inicialização para legado sem baseline, checksum divergente e migração
  incompatível. As fixtures negativas ficam apenas em `src/test/resources`, fora
  da localização das migrações normais.
- Preservação dos dados existentes e rollback da transação da V2 incompatível de
  teste, sem limpeza ou reparo.

Execute tudo com `.\mvnw.cmd -Pintegration verify` a partir de `backend`.
Para somente os cenários Flyway, além dos testes rápidos:
`.\mvnw.cmd -Pintegration "-Dit.test=FlywayLifecycleIT" verify`.

A V1 contém `BEGIN`/`COMMIT` explícitos. O PostgreSQL pode emitir o aviso de
transação já em andamento quando o Flyway a executa; a execução e o histórico são
verificados nos testes. Isso não oferece garantia de atomicidade entre o COMMIT
explícito e a gravação do histórico em caso de interrupção nesse intervalo.
Não altere a V1 nem use reparo automático para esconder divergências.

Em uma falha futura, a aplicação deve permanecer sem inicializar até diagnóstico.
Migrações já confirmadas não são desfeitas por reverter o código da aplicação.
Prefira uma nova migração corretiva compatível ou restauração de backup testada,
com planejamento para preservar gravações posteriores. A adoção do banco de
desenvolvimento permanece uma etapa posterior e exige autorização expressa.

Validação manual: cadastrar, recarregar a página para conferir persistência, editar,
excluir, cancelar uma exclusão e desligar o backend para verificar o erro de conexão.
