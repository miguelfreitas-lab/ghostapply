-- Apenas para testar falha e rollback; não faz parte das migrações da aplicação.
CREATE TABLE flyway_failure_probe (id BIGINT PRIMARY KEY);
ALTER TABLE candidaturas ADD COLUMN empresa VARCHAR(200);
