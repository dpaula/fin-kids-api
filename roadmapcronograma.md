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
  - testes de repository
  - testes de migration (subida limpa de schema)
- [ ] Cobertura alvo da fase: minimo 50% na camada de dominio/persistencia.

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
- [ ] Testes de integracao de fluxo completo de transacao (servico + persistencia + HTTP)
- [ ] Cobertura alvo da fase: minimo 65% do dominio core.

## Fase 3 - API REST v1
Objetivo: entregar endpoints para consumo inicial do front e integracoes.

- [x] Endpoints de conta/resumo:
  - saldo atual
  - resumo mensal
- [x] Endpoints de transacoes:
  - criar transacao
  - listar por periodo
- [ ] Endpoints de metas (CRUD inicial).
- [ ] Endpoints de regra de bonus (consulta/atualizacao).
- [ ] Validacao de payload (Bean Validation + mensagens claras).
- [x] Padrao de resposta HTTP e contrato de erro.
- [ ] Documentacao tecnica inicial da API (OpenAPI/Swagger).
- [ ] Testes automatizados obrigatorios:
  - integracao de controllers
  - testes de contrato dos endpoints principais
- [ ] Cobertura alvo da fase: minimo 70% em servicos e controllers core.

## Fase 4 - Seguranca e autorizacao
Objetivo: proteger API por perfil de usuario e integracao automatizada.

- [ ] Estrutura de autenticacao (JWT/OAuth2 resource server, conforme decisao de arquitetura).
- [ ] Autorizacao por perfil:
  - `CRIANCA` (somente leitura permitida)
  - `PAI/MAE` (leitura e escrita administrativa)
- [ ] Endpoint seguro para automacao (n8n) com credencial dedicada.
- [ ] Auditoria de alteracoes sensiveis.
- [ ] Testes automatizados obrigatorios:
  - testes de autorizacao por role
  - testes de acesso negado
  - testes de endpoint de automacao
- [ ] Cobertura alvo da fase: minimo 75% em regras de seguranca e acesso.

## Fase 5 - Integração de automacao (contrato API)
Objetivo: suportar fluxo n8n/WhatsApp sem dependencia acoplada.

- [ ] Endpoint de ingestao de transacao automatizada com origem `WHATSAPP`.
- [ ] Suporte a evidencia da transacao (id/hash/url interna).
- [ ] Regras de idempotencia para evitar duplicidade.
- [ ] Mensagens de retorno para integracao (sucesso/erro de negocio).
- [ ] Testes automatizados obrigatorios:
  - integracao do endpoint de automacao
  - cenarios de duplicidade e saldo insuficiente
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
- [ ] Iniciar CRUD de metas (criar/listar/atualizar/remover).
- [ ] Iniciar endpoint de regras de bonus (consulta e atualizacao).
- [ ] Adicionar testes de integracao de repositorio para consultas agregadas (saldo/resumo).
