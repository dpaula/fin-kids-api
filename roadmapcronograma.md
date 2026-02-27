# Roadmap e Cronograma - Fin Kids API (MVP)

Este documento controla o plano de execucao do projeto **somente da API** deste repositorio.

## 1) Objetivo do MVP da API
- Ser a fonte de verdade de dados financeiros da conta da crianca.
- Expor endpoints REST para WebApp e automacao (n8n/WhatsApp).
- Garantir regras de negocio essenciais:
  - saldo derivado do historico
  - proibicao de saldo negativo
  - auditabilidade das transacoes
  - segregacao de acesso por perfil
- Entregar base testada continuamente com testes automatizados e cobertura evolutiva.

## 2) Como usar este cronograma
- Cada item deve ser marcado no momento da entrega validada.
- Toda entrega deve incluir:
  - implementacao
  - testes automatizados
  - validacao local (build/test)
- Este arquivo deve ser atualizado a cada nova feature concluida.

## 3) Status atual do projeto

### 3.1 Entregas concluidas
- [x] Estrutura base do projeto Spring Boot 4 + Java 21 pronta.
- [x] Dependencias de persistencia adicionadas:
  - Spring Data JPA
  - MySQL Connector
  - Liquibase (starter do Spring Boot 4)
- [x] Perfis de ambiente criados:
  - `application-dev.yaml`
  - `application-prod.yaml`
- [x] Conexao com MySQL configurada e validada.
- [x] Liquibase configurado com changelog master e changesets iniciais.
- [x] Estrutura inicial de banco criada via migration:
  - `accounts`
  - `app_users`
  - `account_users`
  - `transactions`
  - `bonus_rules`
  - `goals`
- [x] Ajuste incremental de tipo booleano realizado via novo changeset.
- [x] Entidades JPA iniciais criadas e mapeadas.
- [x] Repositories JPA iniciais criados.
- [x] Teste automatizado de contexto executado com sucesso (`./mvnw test -Dspring.profiles.active=dev`).

### 3.2 Entrega concluida
- [x] Camada de servicos de dominio para regras de transacao (entrada/saida e saldo nao negativo).
- [x] Primeiros endpoints REST de transacoes.

### 3.3 Entrega concluida
- [x] Endpoints de conta/resumo para saldo atual e resumo mensal.
- [x] Testes automatizados da camada de resumo (servico + controller).

### 3.4 Entrega concluida
- [x] CRUD inicial de metas (criar/listar/atualizar/remover com exclusao logica).
- [x] Testes automatizados de metas (servico + controller).

### 3.5 Entrega concluida
- [x] Validacao de payload e parametros com Bean Validation nos controllers/DTOs.
- [x] Tratamento padronizado de erros de validacao HTTP 400 no `ApiExceptionHandler`.
- [x] Cobertura automatizada com JaCoCo no build (`verify`) com regra minima aplicada.
- [x] Testes de repositorio para consultas agregadas de saldo/resumo mensal.

### 3.6 Entrega concluida
- [x] Endpoints de regra de bonus implementados:
  - `GET /api/v1/accounts/{accountId}/bonus-rule`
  - `PUT /api/v1/accounts/{accountId}/bonus-rule`
- [x] Camada de servico para consulta e upsert de regra de bonus por conta.
- [x] Validacao de payload para regra de bonus (`percentage`, `conditionType`, `baseType`, `active`).
- [x] Testes automatizados da entrega (servico + controller) com cenarios de sucesso e erro.
- [x] Gate de cobertura elevado para classes core de servico/controller (>= 80% no JaCoCo check).

### 3.7 Entrega concluida
- [x] Testes de integracao end-to-end do fluxo de transacao via HTTP (`POST /transactions`).
- [x] Validacao de persistencia real do fluxo:
  - deposito bem-sucedido
  - saque bem-sucedido com saldo suficiente
  - bloqueio de saque por saldo insuficiente sem persistencia indevida
- [x] Cobertura de erros de contrato no fluxo integrado:
  - payload invalido (`400`)
  - conta inexistente (`404`)

### 3.8 Entrega concluida
- [x] Publicacao da documentacao tecnica OpenAPI/Swagger da API v1.
- [x] Configuracao central do OpenAPI com metadados e schema de erro padrao.
- [x] Anotacao dos controllers e DTOs principais para contrato documentado.
- [x] Teste de integracao da documentacao:
  - `GET /v3/api-docs`
  - `GET /swagger-ui/index.html`

### 3.9 Entrega concluida
- [x] Endpoint seguro de automacao implementado:
  - `POST /api/v1/automation/transactions`
- [x] Autenticacao por token dedicado para chamadas n8n/WhatsApp (`Authorization: Bearer <token>`).
- [x] Integracao com regras core de transacao, forçando origem `WHATSAPP` no fluxo de automacao.
- [x] OpenAPI atualizado com `securityScheme` para automacao (`AutomationBearerAuth`).
- [x] Testes de integracao da automacao cobrindo:
  - sucesso com token valido (`201`)
  - token ausente/invalido (`401`)
  - payload invalido (`400`)
  - conta inexistente (`404`)
  - saldo insuficiente (`422`)

### 3.10 Entrega concluida
- [x] Idempotencia de transacoes por evidencia implementada no fluxo de automacao.
- [x] Constraint de unicidade em banco adicionada via Liquibase para evitar duplicidade:
  - `transactions(account_id, origin, evidence_reference)`
- [x] Tratamento de duplicidade padronizado com `409 Conflict`.
- [x] Documentacao OpenAPI atualizada com resposta `409` nos endpoints de criacao de transacao.
- [x] Testes automatizados da entrega:
  - unitario de servico para bloqueio de duplicidade
  - controller test para mapeamento HTTP `409`
  - integracao de automacao com tentativa duplicada de mesma evidencia

### 3.11 Entrega concluida
- [x] Estrutura de autenticacao de usuario via JWT (OAuth2 Resource Server) implementada.
- [x] Compatibilidade com login Google do WebApp considerada no contrato tecnico:
  - API validando JWT por `issuer-uri` configuravel (`JWT_ISSUER_URI`)
  - principal de usuario mapeado por claim `email`
- [x] Cadeias de seguranca separadas para evitar conflito entre canais:
  - `/api/v1/automation/**` com token dedicado n8n
  - `/api/v1/**` (demais endpoints) com JWT de usuario
- [x] Autorizacao por perfil e por conta aplicada usando vinculo `account_users.profile_role`:
  - `CHILD`: leitura
  - `PARENT`: leitura e escrita
- [x] Endpoints core protegidos com regras de acesso por `accountId` (`@PreAuthorize`).
- [x] OpenAPI atualizado com esquema de seguranca de usuario (`UserBearerAuth`).
- [x] Testes automatizados da entrega:
  - cenarios `401` (sem JWT)
  - cenarios `403` (role sem permissao de escrita)
  - cenarios de leitura permitida para crianca
  - regressao dos fluxos existentes com autenticacao valida

### 3.12 Entrega concluida
- [x] Endpoint de contexto de sessao para WebApp implementado:
  - `GET /api/v1/users/me`
- [x] Contrato de sessao entregue com:
  - dados basicos do usuario autenticado
  - lista de contas vinculadas com `profileRole` por conta
- [x] Regras de seguranca e consistencia aplicadas:
  - `401` para usuario nao autenticado ou contexto invalido
  - `404` para usuario inexistente ou sem vinculo de conta
- [x] Testes automatizados da entrega:
  - unitarios da camada de servico de sessao
  - integracao HTTP cobrindo `200`, `401` e `404`
  - regressao do teste de documentacao OpenAPI para novo endpoint

## 4) Roadmap detalhado por fases

## Fase 1 - Fundacao de dominio e persistencia
Objetivo: consolidar base tecnica e contrato minimo de dados.

- [x] Configuracao de banco e migrations iniciais.
- [x] Entidades e repositories base.
- [ ] Modelagem de constraints adicionais no banco:
  - checks de valores monetarios positivos
  - indices para consultas de extrato e resumo
- [ ] Seeds opcionais para ambiente de desenvolvimento.
- [ ] Testes automatizados da camada de persistencia:
  - [x] testes de repository
  - testes de migration (subida limpa de schema)
- [x] Cobertura alvo da fase: minimo 50% na camada de dominio/persistencia.

## Fase 2 - Regras de negocio core
Objetivo: implementar comportamento principal do produto.

- [x] Caso de uso: criar transacao `DEPOSIT`.
- [x] Caso de uso: criar transacao `WITHDRAW` com bloqueio de saldo negativo.
- [x] Calculo de saldo por historico de transacoes.
- [x] Consulta de extrato por periodo.
- [x] Consulta de resumo mensal (por origem e por tipo).
- [x] Padronizacao de validacoes:
  - valores monetarios
  - datas
  - campos obrigatorios
- [x] Tratamento padrao de erros de negocio.
- [x] Testes automatizados da entrega 3.2:
  - unitarios da camada de servico
  - testes de controller com MockMvc
- [x] Testes de integracao de fluxo completo de transacao (servico + persistencia + HTTP)
- [x] Cobertura alvo da fase: minimo 65% do dominio core.

## Fase 3 - API REST v1
Objetivo: entregar endpoints para consumo inicial do front e integracoes.

- [x] Endpoints de conta/resumo:
  - saldo atual
  - resumo mensal
- [x] Endpoints de transacoes:
  - criar transacao
  - listar por periodo
- [x] Endpoints de metas (CRUD inicial).
- [x] Endpoints de regra de bonus (consulta/atualizacao).
- [x] Validacao de payload (Bean Validation + mensagens claras).
- [x] Padrao de resposta HTTP e contrato de erro.
- [x] Documentacao tecnica inicial da API (OpenAPI/Swagger).
- [ ] Testes automatizados obrigatorios:
  - integracao de controllers
  - testes de contrato dos endpoints principais
- [x] Cobertura alvo da fase: minimo 70% em servicos e controllers core.

## Fase 4 - Seguranca e autorizacao
Objetivo: proteger API por perfil de usuario e integracao automatizada.

- [x] Estrutura de autenticacao (JWT/OAuth2 resource server, conforme decisao de arquitetura).
- [x] Autorizacao por perfil:
  - `CRIANCA` (somente leitura permitida, mapeado para `CHILD`)
  - `PAI/MAE` (leitura e escrita administrativa, mapeado para `PARENT`)
- [x] Endpoint seguro para automacao (n8n) com credencial dedicada.
- [x] Endpoint de contexto autenticado para bootstrap de sessao do front (`/api/v1/users/me`).
- [ ] Auditoria de alteracoes sensiveis.
- [x] Testes automatizados obrigatorios:
  - testes de autorizacao por role
  - testes de acesso negado
  - [x] testes de endpoint de automacao
- [ ] Cobertura alvo da fase: minimo 75% em regras de seguranca e acesso.

## Fase 5 - Integração de automacao (contrato API)
Objetivo: suportar fluxo n8n/WhatsApp sem dependencia acoplada.

- [x] Endpoint de ingestao de transacao automatizada com origem `WHATSAPP`.
- [x] Suporte a evidencia da transacao (id/hash/url interna).
- [x] Regras de idempotencia para evitar duplicidade.
- [x] Mensagens de retorno para integracao (sucesso/erro de negocio).
- [ ] Testes automatizados obrigatorios:
  - [x] integracao do endpoint de automacao
  - [x] cenarios de duplicidade e saldo insuficiente
- [ ] Cobertura alvo da fase: minimo 75% nos fluxos de automacao.

## Fase 6 - Metas e bonus educacional (MVP expandido)
Objetivo: habilitar recursos educacionais previstos no produto.

- [ ] Regras de bonus configuraveis por conta.
- [ ] Execucao de bonus (job/agendamento) com trilha auditavel.
- [ ] Evolucao de metas com progresso por saldo.
- [ ] Endpoints de consulta para tela crianca e pais.
- [ ] Testes automatizados obrigatorios:
  - unitarios das regras de bonus
  - integracao de metas e progresso
- [ ] Cobertura alvo da fase: minimo 80% dos modulos de bonus/metas.

## Fase 7 - Qualidade operacional e preparo para producao
Objetivo: estabilidade, observabilidade e padrao de release.

- [ ] Health checks e readiness/liveness ajustados.
- [ ] Logs estruturados e rastreabilidade de erro.
- [ ] Configuracao de metricas essenciais.
- [ ] Pipeline CI com etapas minimas:
  - build
  - testes
  - cobertura
  - validacao de migrations
- [ ] Regra de qualidade:
  - build quebrado se teste falhar
  - build quebrado se cobertura minima nao for atingida
- [ ] Documentacao de deploy e rollback de banco.

## 5) Definicao de pronto (Definition of Done)
Um item so pode ser marcado como concluido quando:
- codigo implementado e revisado
- testes automatizados adicionados/atualizados e passando
- sem regressao no build local
- documentacao tecnica atualizada (quando aplicavel)
- item marcado neste `roadmapcronograma.md`

## 6) Proxima entrega recomendada (curto prazo)
- [ ] Iniciar trilha de auditoria para alteracoes sensiveis (transacoes manuais, metas e regra de bonus).
- [ ] Fechar pendencias de fundacao de banco:
  - checks de valores monetarios positivos
  - testes de migration de subida limpa do schema
