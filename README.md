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
O teste existente `BackendApplicationTests` inicia o contexto completo e exige
PostgreSQL acessível e a migração aplicada.

Validação manual: cadastrar, recarregar a página para conferir persistência, editar,
excluir, cancelar uma exclusão e desligar o backend para verificar o erro de conexão.
