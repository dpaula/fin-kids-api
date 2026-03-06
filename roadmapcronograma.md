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

## 3) Painel de progresso (funcional x nao funcional)

Classificacao adotada:
- **Funcional**: atividades que entregam comportamento de negocio e contratos de uso da API.
- **Nao funcional**: atividades de qualidade, seguranca, operacao, observabilidade e entrega.

Separacao por fase:
- Funcional: Fases 1, 2, 3, 5 e 6.
- Nao funcional: Fases 4 e 7.

Percentual consolidado (base: itens de primeiro nivel das fases):
- Funcional: **36/37 concluidos (97.3%)**
- Nao funcional: **13/18 concluidos (72.2%)**
- Geral do roadmap: **49/55 concluidos (89.1%)**

Regra de atualizacao desta secao:
- Ao marcar/desmarcar qualquer item nas fases, atualizar imediatamente os 3 percentuais acima.

## 4) Status atual do projeto

### 4.1 Entregas concluidas
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

### 4.2 Entrega concluida
- [x] Camada de servicos de dominio para regras de transacao (entrada/saida e saldo nao negativo).
- [x] Primeiros endpoints REST de transacoes.

### 4.3 Entrega concluida
- [x] Endpoints de conta/resumo para saldo atual e resumo mensal.
- [x] Testes automatizados da camada de resumo (servico + controller).

### 4.4 Entrega concluida
- [x] CRUD inicial de metas (criar/listar/atualizar/remover com exclusao logica).
- [x] Testes automatizados de metas (servico + controller).

### 4.5 Entrega concluida
- [x] Validacao de payload e parametros com Bean Validation nos controllers/DTOs.
- [x] Tratamento padronizado de erros de validacao HTTP 400 no `ApiExceptionHandler`.
- [x] Cobertura automatizada com JaCoCo no build (`verify`) com regra minima aplicada.
- [x] Testes de repositorio para consultas agregadas de saldo/resumo mensal.

### 4.6 Entrega concluida
- [x] Endpoints de regra de bonus implementados:
  - `GET /api/v1/accounts/{accountId}/bonus-rule`
  - `PUT /api/v1/accounts/{accountId}/bonus-rule`
- [x] Camada de servico para consulta e upsert de regra de bonus por conta.
- [x] Validacao de payload para regra de bonus (`percentage`, `conditionType`, `baseType`, `active`).
- [x] Testes automatizados da entrega (servico + controller) com cenarios de sucesso e erro.
- [x] Gate de cobertura elevado para classes core de servico/controller (>= 80% no JaCoCo check).

### 4.7 Entrega concluida
- [x] Testes de integracao end-to-end do fluxo de transacao via HTTP (`POST /transactions`).
- [x] Validacao de persistencia real do fluxo:
  - deposito bem-sucedido
  - saque bem-sucedido com saldo suficiente
  - bloqueio de saque por saldo insuficiente sem persistencia indevida
- [x] Cobertura de erros de contrato no fluxo integrado:
  - payload invalido (`400`)
  - conta inexistente (`404`)

### 4.8 Entrega concluida
- [x] Publicacao da documentacao tecnica OpenAPI/Swagger da API v1.
- [x] Configuracao central do OpenAPI com metadados e schema de erro padrao.
- [x] Anotacao dos controllers e DTOs principais para contrato documentado.
- [x] Teste de integracao da documentacao:
  - `GET /v3/api-docs`
  - `GET /swagger-ui/index.html`

### 4.9 Entrega concluida
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

### 4.10 Entrega concluida
- [x] Idempotencia de transacoes por evidencia implementada no fluxo de automacao.
- [x] Constraint de unicidade em banco adicionada via Liquibase para evitar duplicidade:
  - `transactions(account_id, origin, evidence_reference)`
- [x] Tratamento de duplicidade padronizado com `409 Conflict`.
- [x] Documentacao OpenAPI atualizada com resposta `409` nos endpoints de criacao de transacao.
- [x] Testes automatizados da entrega:
  - unitario de servico para bloqueio de duplicidade
  - controller test para mapeamento HTTP `409`
  - integracao de automacao com tentativa duplicada de mesma evidencia

### 4.11 Entrega concluida
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

### 4.12 Entrega concluida
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

### 4.13 Entrega concluida
- [x] Trilha de auditoria para alteracoes sensiveis implementada.
- [x] Persistencia de eventos de auditoria via tabela dedicada `audit_events` com indices para consultas por conta e tempo.
- [x] Auditoria aplicada nos fluxos sensiveis:
  - criacao de transacao manual
  - criacao/atualizacao/remocao de metas
  - upsert da regra de bonus
- [x] Contexto do ator autenticado registrado em cada evento:
  - email do usuario autenticado
  - usuario global (quando encontrado no cadastro interno)
- [x] Testes automatizados da entrega:
  - unitario da camada de auditoria
  - integracao HTTP validando criacao de eventos auditaveis
  - garantia de nao auditoria para endpoint de automacao

### 4.14 Entrega concluida
- [x] Endpoint administrativo para gestao de vinculos usuario-conta implementado:
  - `GET /api/v1/accounts/{accountId}/user-links`
  - `PUT /api/v1/accounts/{accountId}/user-links/{userId}`
- [x] Regra de autorizacao aplicada no fluxo administrativo:
  - apenas perfil com permissao de escrita na conta (`PARENT`) pode gerenciar vinculos
- [x] Regra de negocio de upsert de vinculo entregue:
  - cria quando nao existe
  - atualiza `profileRole` quando ja existe
- [x] Auditoria de mudancas de vinculo registrada na trilha sensivel (`audit_events`).
- [x] Testes automatizados da entrega:
  - unitarios do servico de vinculos
  - testes de controller para contrato e validacao
  - integracao HTTP cobrindo sucesso, `401`, `403`, `404`

### 4.15 Entrega concluida
- [x] Pipeline local de release Docker implementada (sem dependencia de GitHub Actions).
- [x] Profile Maven `jib-docker-build` criado no `pom.xml` com:
  - imagem destino `docker.io/fplima/fin-kids-api`
  - build multi-arquitetura Linux (`amd64` + `arm64`)
  - `mainClass` da API (`br.com.autevia.finkidsapi.FinKidsApiApplication`)
  - publicacao de tags `${project.version}` e `latest`
- [x] Credenciais de publicacao mantidas fora de codigo:
  - **ajuste temporario de desenvolvimento**: credenciais Docker Hub hardcoded no profile do Jib
- [x] Script versionado para release local:
  - `scripts/release-local.sh`
  - validacoes de versao, workspace, build/test e `docker pull` opcional
- [x] Teste automatizado de smoke do script:
  - `scripts/test-release-local.sh`

### 4.16 Entrega concluida
- [x] Ambiente de testes isolado de banco externo implementado com profile dedicado:
  - `application-test.yaml` com datasource via Testcontainers JDBC (`jdbc:tc:mysql:8.4.0`)
  - token de automacao de teste e `jwk-set-uri` de teste para evitar dependencias externas em bootstrap
- [x] Testes de integracao e repositorio migrados de `@ActiveProfiles("dev")` para `@ActiveProfiles("test")`.
- [x] Teste de contexto principal (`FinKidsApiApplicationTests`) alinhado ao profile `test`.
- [x] Dependencias de Testcontainers adicionadas no `pom.xml` (test scope):
  - `org.testcontainers:junit-jupiter`
  - `org.testcontainers:mysql`
  - `org.testcontainers:jdbc`
- [x] Remocao de defaults sensiveis em configuracoes de ambiente:
  - `application-prod.yaml` sem usuario/senha default de banco
  - `application-dev.yaml` sem host/credenciais sensiveis hardcoded
- [x] Validacao local da entrega:
  - `./mvnw verify` com sucesso sem dependencia de MySQL remoto para testes

### 4.17 Entrega concluida
- [x] Endurecimento de integridade de dominio no banco via Liquibase:
  - check `transactions.amount > 0`
  - check `goals.target_amount > 0`
  - check `bonus_rules.percentage` entre `0` e `100`
- [x] Otimizacao de consulta para extrato/resumo com novos indices:
  - `transactions(account_id, occurred_at, type)`
  - `transactions(account_id, occurred_at, origin)`
- [x] Testes automatizados de persistencia para validar rejeicao de dados invalidos por constraints de banco.
- [x] Teste de migration em schema limpo cobrindo subida completa dos changesets e existencia dos artefatos de integridade.
- [x] Validacao local da entrega:
  - `./mvnw verify` com sucesso.

### 4.18 Entrega concluida
- [x] Execucao automatizada de bonus mensal implementada em servico dedicado.
- [x] Agendamento configuravel com cron implementado para processar o mes de referencia anterior.
- [x] Regra de elegibilidade aplicada na execucao:
  - conta sem `WITHDRAW` no mes
  - regra de bonus ativa
  - idempotencia mensal por evidencia (`bonus:YYYY-MM`)
- [x] Base de calculo implementada para todos os modos configurados:
  - `LAST_BALANCE`
  - `LAST_ALLOWANCE` (ignora origem `BONUS` para ultima mesada)
  - `MONTHLY_DEPOSITS`
- [x] Trilha de auditoria de sistema para bonus aplicado:
  - `audit_events.action_type = BONUS_APPLIED`
  - `actor_email` tecnico configuravel por propriedade
- [x] Testes automatizados da entrega:
  - unitarios do servico de execucao
  - unitario do scheduler
  - integracao do fluxo de execucao + auditoria
  - repositorio com queries de suporte ao bonus

### 4.19 Entrega concluida
- [x] Evolucao de metas com progresso por saldo implementada para consumo de telas.
- [x] Endpoints de consulta por perfil implementados:
  - `GET /api/v1/accounts/{accountId}/child-view`
  - `GET /api/v1/accounts/{accountId}/parent-view`
- [x] Contrato de resposta da visao da crianca entregue com:
  - saldo atual
  - metas com progresso (`progressAmount`, `progressPercent`, `remainingAmount`, `achieved`)
  - historico simplificado de transacoes recentes
- [x] Contrato de resposta da visao dos pais entregue com:
  - saldo atual
  - resumo mensal por tipo e origem
  - regra de bonus atual (quando existente)
  - metas com progresso
  - historico detalhado de transacoes recentes
- [x] Testes automatizados da entrega:
  - unitarios do servico de visao da conta (calculo de progresso e composicao)
  - testes de controller do novo contrato HTTP
  - integracao HTTP cobrindo `200`, `401` e `403`
- [x] Validacao local da entrega:
  - `./mvnw verify` com sucesso
- [x] Validacao local da entrega:
  - `./mvnw verify` com sucesso.

## 5) Roadmap detalhado por fases

### 5.1 Atividades funcionais (regras de negocio e contrato de API)

## Fase 1 - Fundacao de dominio e persistencia [FUNCIONAL]
Objetivo: consolidar base tecnica e contrato minimo de dados.

- [x] Configuracao de banco e migrations iniciais.
- [x] Entidades e repositories base.
- [x] Modelagem de constraints adicionais no banco:
  - checks de valores monetarios positivos
  - indices para consultas de extrato e resumo
- [ ] Seeds opcionais para ambiente de desenvolvimento.
- [x] Testes automatizados da camada de persistencia:
  - [x] testes de repository
  - [x] testes de migration (subida limpa de schema)
- [x] Cobertura alvo da fase: minimo 50% na camada de dominio/persistencia.

## Fase 2 - Regras de negocio core [FUNCIONAL]
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

## Fase 3 - API REST v1 [FUNCIONAL]
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
- [x] Testes automatizados obrigatorios:
  - integracao de controllers
  - testes de contrato dos endpoints principais
- [x] Cobertura alvo da fase: minimo 70% em servicos e controllers core.

## Fase 5 - Integração de automacao (contrato API) [FUNCIONAL]
Objetivo: suportar fluxo n8n/WhatsApp sem dependencia acoplada.

- [x] Endpoint de ingestao de transacao automatizada com origem `WHATSAPP`.
- [x] Suporte a evidencia da transacao (id/hash/url interna).
- [x] Regras de idempotencia para evitar duplicidade.
- [x] Mensagens de retorno para integracao (sucesso/erro de negocio).
- [x] Testes automatizados obrigatorios:
  - [x] integracao do endpoint de automacao
  - [x] cenarios de duplicidade e saldo insuficiente
- [x] Cobertura alvo da fase: minimo 75% nos fluxos de automacao.

## Fase 6 - Metas e bonus educacional (MVP expandido) [FUNCIONAL]
Objetivo: habilitar recursos educacionais previstos no produto.

- [x] Regras de bonus configuraveis por conta (consulta/upsert da regra por conta).
- [x] Execucao de bonus (job/agendamento) com trilha auditavel.
- [x] Evolucao de metas com progresso por saldo.
- [x] Endpoints de consulta para tela crianca e pais.
- [x] Testes automatizados obrigatorios:
  - [x] unitarios das regras de bonus e agendamento
  - [x] integracao de metas e progresso
- [x] Cobertura alvo da fase: minimo 80% dos modulos de bonus/metas.

### 5.2 Atividades nao funcionais (qualidade, seguranca, operacao e entrega)

## Fase 4 - Seguranca e autorizacao [NAO FUNCIONAL]
Objetivo: proteger API por perfil de usuario e integracao automatizada.

- [x] Estrutura de autenticacao (JWT/OAuth2 resource server, conforme decisao de arquitetura).
- [x] Autorizacao por perfil:
  - `CRIANCA` (somente leitura permitida, mapeado para `CHILD`)
  - `PAI/MAE` (leitura e escrita administrativa, mapeado para `PARENT`)
- [x] Endpoint seguro para automacao (n8n) com credencial dedicada.
- [x] Endpoint de contexto autenticado para bootstrap de sessao do front (`/api/v1/users/me`).
- [x] Auditoria de alteracoes sensiveis (transacoes manuais, metas e regra de bonus).
- [x] Endpoint administrativo para criar/atualizar vinculos de usuario-conta (`account_users`) com role por conta.
- [x] Testes automatizados obrigatorios:
  - testes de autorizacao por role
  - testes de acesso negado
  - [x] testes de endpoint administrativo de vinculos
  - [x] testes de endpoint de automacao
- [x] Cobertura alvo da fase: minimo 75% em regras de seguranca e acesso.

## Fase 7 - Qualidade operacional e preparo para producao [NAO FUNCIONAL]
Objetivo: estabilidade, observabilidade e padrao de release.

- [ ] Health checks e readiness/liveness ajustados.
- [ ] Logs estruturados e rastreabilidade de erro.
- [ ] Configuracao de metricas essenciais.
- [ ] Pipeline CI com etapas minimas (opcional, sem publish de imagem):
  - build
  - testes
  - cobertura
  - validacao de migrations
- [x] Isolar ambiente de testes/CI de banco externo e remover defaults sensiveis de credenciais.
- [x] Pipeline de imagem Docker com profile Maven (`jib-docker-build`):
  - adicionar profile no `pom.xml` com `jib-maven-plugin`
  - configurar imagem destino `docker.io/fplima/fin-kids-api`
  - configurar build multi-arquitetura Linux (`amd64` + `arm64`)
  - configurar `mainClass` da API: `br.com.autevia.finkidsapi.FinKidsApiApplication`
  - configurar porta de container (default atual: `8080`)
- [x] Publicacao local no Docker Hub via fluxo de desenvolvimento:
  - autenticacao via credenciais hardcoded no profile Jib (escopo de desenvolvimento/teste)
  - publicar tags `latest` e `${project.version}`
  - opcional: sobrescrever imagem destino com `DOCKER_IMAGE`
- [x] Regras de execucao de release local:
  - executar `./mvnw verify` antes do publish (default do script)
  - publicar imagem via profile `jib-docker-build`
  - validar push com `docker pull` da nova tag e `latest` (pode desativar com `--no-pull`)
- [x] Regra de qualidade:
  - build quebrado se teste falhar
  - build quebrado se cobertura minima nao for atingida
- [ ] Documentacao de deploy e rollback de banco.

## 6) Definicao de pronto (Definition of Done)
Um item so pode ser marcado como concluido quando:
- codigo implementado e revisado
- testes automatizados adicionados/atualizados e passando
- sem regressao no build local
- documentacao tecnica atualizada (quando aplicavel)
- item marcado neste `roadmapcronograma.md`

## 7) Proxima entrega recomendada (curto prazo)
- [ ] Prioridade funcional:
  - avaliar necessidade de seeds opcionais para ambiente de desenvolvimento (fase 1 pendente)
- [ ] Prioridade nao funcional:
  - health/readiness/liveness para operacao local e futura producao
  - logs estruturados e rastreabilidade de erro
  - metricas essenciais e documentacao de deploy/rollback de banco
