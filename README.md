# GranaGalaxy - API Central

Este repositorio contem o modulo principal (API) do projeto **GranaGalaxy**.

A API sera o cerebro do sistema e sera integrada com:
- automacoes (`n8n` + WhatsApp via Evo API + IA/OCR)
- um WebApp (React) com telas separadas para Crianca e Pais

O objetivo do GranaGalaxy e criar uma experiencia simples e visual para uma crianca acompanhar a evolucao do proprio dinheiro (mesadas, depositos, compras), enquanto pais mantem controle e configuracao das regras.

## 1) Visao do produto

### Objetivo educacional
- Ensinar nocoes basicas de dinheiro:
  - entradas (mesada/depositos)
  - saidas (compras)
  - saldo
  - metas
- Evoluir para recompensa por guardar (bonus) e futuramente juros compostos e gamificacao.

### Objetivo tecnico
- Arquitetura modular:
  - API (este repositorio): regras e dados
  - n8n: ingestao automatizada via WhatsApp + leitura/classificacao de imagens
  - Front-end: experiencia visual para crianca + painel para pais

## 2) Modulos do sistema (panorama)

### Modulo A - API (este repositorio)
Responsavel por:
- persistencia e leitura de dados
- regras de negocio
- validacoes (ex.: impedir saldo negativo)
- consolidacoes (ex.: resumo mensal)
- autenticacao/autorizacao (pais vs crianca)
- endpoints REST consumidos pelo WebApp e pelo n8n

### Modulo B - Automacao (n8n + WhatsApp)
Responsavel por:
- receber imagens (comprovante/nota fiscal) via WhatsApp
- extrair texto (OCR) + classificar tipo (deposito vs gasto)
- transformar em intencao de transacao
- chamar API para registrar transacao
- responder no WhatsApp com confirmacao/saldo/erros

### Modulo C - WebApp (React)
Responsavel por:
- login e selecao de perfil (crianca vs pais)
- visualizacao amigavel para crianca (saldo + graficos + metas)
- painel de pais (configuracoes, ajustes, auditoria)

## 3) Regras de negocio (core)

### 3.1 Conceitos principais
- **Conta da Crianca**: possui um saldo unico e historico de transacoes.
- **Transacao**: registro imutavel de entrada ou saida.
- **Origem**: identifica se veio de automacao (WhatsApp), manual (pais) ou sistema (bonus).
- **Papeis (roles)**:
  - `CRIANCA`: leitura (e possivelmente iniciar pedidos de compra no futuro)
  - `PAI/MAE`: leitura e escrita (criar/editar configuracoes e registrar ajustes)
- **Metas**: objetivos de compra/sonho (ex.: brinquedo, jogo), com progresso.

### 3.2 Regras de saldo
- O saldo deve ser sempre derivavel do historico de transacoes.
- Ao registrar transacao:
  - entradas aumentam saldo
  - saidas diminuem saldo
- **Proibicao de saldo negativo**:
  - gastos/saques nao podem deixar saldo < 0 na fase inicial.

### 3.3 Regras sobre transacoes
- Tipos:
  - `DEPOSIT` (entrada)
  - `WITHDRAW` (saida)
- Toda transacao deve conter:
  - data/hora
  - valor
  - descricao curta
  - origem (`WHATSAPP` / `MANUAL` / `BONUS`)
  - referencia de evidencia opcional (id da imagem, hash, ou URL interna)
- Transacoes devem ser auditaveis:
  - nao devem ser apagadas
  - ajustes devem gerar novas transacoes (estorno/correcao)

### 3.4 Regras de bonus por guardar
Primeira evolucao planejada, ja prevista na modelagem.

- Existe uma configuracao de bonus:
  - "Se ficar X tempo sem gastar, aplica bonus Y%"
- Primeira versao sugerida:
  - bonus mensal baseado em "sem gastos no mes"
  - percentual configuravel pelos pais (ex.: 5%, 10%)
  - regra de base explicita (ultimo saldo, ultima mesada ou soma de depositos do mes)

### 3.5 Metas e progresso
- Uma meta tem:
  - nome
  - valor alvo
- Progresso = saldo atual (ou valor reservado no futuro, quando houver envelopes).
- Crianca ve metas com barra de progresso simples.

### 3.6 Transparencia e explicabilidade
- Crianca deve entender:
  - entrou X
  - saiu Y
  - agora tenho Z
- Pais devem ver:
  - historico completo
  - origens
  - possiveis erros de leitura (origem WhatsApp/IA)

## 4) Stack da API

- Java 21
- Spring Boot 4.0.3 (conforme `pom.xml`)
- Maven Wrapper (`./mvnw`)
- REST API

Objetivo deste modulo:
- ser fonte de verdade do dinheiro da crianca
- implementar regras de negocio
- expor endpoints para WebApp e n8n

### Autenticacao e autorizacao (MVP tecnico)
- WebApp autentica com Google e envia JWT no header `Authorization: Bearer <token>`.
- API valida JWT como Resource Server (issuer configuravel por `JWT_ISSUER_URI`, default Google).
- Endpoint de automacao usa credencial dedicada e separada (`N8N_API_TOKEN`).
- Autorizacao e por vinculo de conta (`account_users`):
  - `CHILD`: leitura
  - `PARENT`: leitura e escrita

## 5) Endpoints essenciais (primeira versao)

### Conta / Resumo
- consultar saldo atual e dados principais da conta
- consultar resumo por mes e por origem

### Transacoes
- criar transacao (entrada/saida)
- listar transacoes por periodo

### Regras
- consultar regra atual de bonus
- atualizar percentual e parametros (somente pais)

### Metas
- criar meta
- listar metas
- atualizar meta (somente pais)
- remover meta (somente pais)

### Usuarios
- consultar contexto da sessao autenticada (`GET /api/v1/users/me`) para bootstrap do front-end
- listar vinculos de usuarios por conta (`GET /api/v1/accounts/{accountId}/user-links`) para painel administrativo dos pais
- criar/atualizar vinculo de usuarios por conta (`PUT /api/v1/accounts/{accountId}/user-links/{userId}`)
- garantir permissoes administrativas para pai e mae via vinculo em `account_users`

## 6) Integracao com n8n + WhatsApp (Evo API)

Este modulo nao vive neste repositorio, mas a API precisa estar pronta para ele.

### Objetivo
- reduzir esforco manual dos pais
- transformar imagens em transacoes
- manter fluxo sem dependencia de input manual

### Fluxos principais (fase inicial)

#### Fluxo A - Deposito
- entrada: comprovante (imagem)
- OCR + IA classificam deposito e valor
- n8n chama API com `DEPOSIT`
- resposta no WhatsApp com valor reconhecido e saldo atualizado

Regras:
- se valor estiver ambiguo, pedir confirmacao humana
- evitar duplicidade da mesma imagem (bloquear ou pedir confirmacao)

#### Fluxo B - Gasto
- entrada: nota fiscal/comprovante de compra
- OCR + IA classificam gasto e valor
- n8n chama API com `WITHDRAW`
- resposta no WhatsApp com valor e saldo atualizado

Regras:
- se saldo ficar negativo, API rejeita
- n8n responde "saldo insuficiente" com instrucao de proximo passo

## 7) Frontend WebApp (React)

Este modulo nao vive neste repositorio, mas guia necessidades de API.

### Tela da Crianca
- saldo atual em destaque
- grafico de pizza (participacao de depositos por origem)
- crescimento mensal
- metas com barra de progresso
- historico simplificado com icones de entrada/saida

Regra:
- crianca nao altera configuracoes na fase inicial

### Tela dos Pais
- saldo + historico completo com filtros
- origem das transacoes (WhatsApp vs manual)
- correcoes/ajustes manuais
- configuracao de bonus
- gestao de metas
- auditoria

## 8) Roadmap de desenvolvimento

### Fase 1 - Base funcional da API (prioridade maxima)
1. Banco + migrations (Liquibase)
2. Entidades principais (conta, transacoes, regras, metas, usuarios)
3. Endpoints basicos:
   - criar transacao
   - consultar saldo e extrato
   - consultar resumo mensal
4. Regras de validacao:
   - impedir saldo negativo
   - padronizar valores monetarios
5. Testes basicos (unitarios e/ou integrados)

### Fase 2 - Integracao minima com n8n
1. Endpoint seguro para insercao via automacao
2. Registro de origem `WHATSAPP` e evidencia
3. Fluxos de deposito e gasto com respostas simples

### Fase 3 - WebApp minimo
1. Login
2. Tela crianca com saldo + historico simplificado
3. Tela pais com historico completo + criacao manual

### Fase 4 - Evolucoes educacionais
1. Bonus por guardar (configuravel)
2. Metas com progresso mais rico
3. Relatorios mensais

## 9) Criterios de qualidade (nao negociaveis)

- consistencia do saldo
- auditabilidade do historico
- seguranca (roles e endpoints de automacao)
- simplicidade de uso (principalmente para crianca)
- modularidade (API nao depende de n8n ou front para existir)

## 10) Ideias de evolucao (futuro)

- gamificacao (niveis, conquistas, mascote)
- simulador visual de juros compostos
- notificacoes automaticas (ex.: sem gastar ha 30 dias)
- termometro de desejos (metas com itens e imagens)
- mensagens educativas apos cada compra

## 11) Glossario

- **Saldo**: valor atual disponivel na conta da crianca
- **Transacao**: entrada ou saida registrada no sistema
- **Origem**: onde a transacao foi criada (`WHATSAPP`, `MANUAL`, `BONUS`)
- **Bonus**: aumento aplicado por regra configurada
- **Meta**: objetivo com valor alvo que a crianca quer alcancar

## 12) Status atual do repositorio

- API ja possui base funcional do dominio e persistencia com Liquibase.
- Endpoints core implementados:
  - transacoes (criacao/listagem)
  - saldo e resumo mensal
  - metas (CRUD inicial com exclusao logica)
  - regra de bonus (consulta/upsert de configuracao)
  - automacao segura (`/api/v1/automation/transactions`)
  - contexto autenticado de usuario (`GET /api/v1/users/me`)
- Seguranca implementada com dois canais:
  - JWT de usuario (OAuth2 Resource Server)
  - token dedicado para automacao n8n
- Documentacao OpenAPI/Swagger e cobertura automatizada com testes estao ativas no build.
- Pendencias principais de roadmap:
  - trilha de auditoria para alteracoes sensiveis
  - endpoint administrativo para criar/atualizar vinculos em `account_users`
  - execucao automatizada de bonus (alem da configuracao)
