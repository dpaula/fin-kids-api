# GranaGalaxy API - Agents Reference

Este arquivo centraliza contexto para agentes e contribuidores trabalharem neste repositorio.

## 1) Projeto e objetivo
- Projeto: `GranaGalaxy`
- Modulo deste repositorio: API central (`fin-kids-api`)
- Objetivo do produto: educacao financeira infantil com visao simples para crianca e controle para pais.
- Objetivo tecnico: manter a API como fonte de verdade de dados e regras.

## 2) Stack atual do repositorio
- Linguagem: Java 21
- Framework: Spring Boot 4.0.3
- Build: Maven Wrapper (`./mvnw`)
- Tipo de API: REST
- Config atual: `src/main/resources/application.yaml`
- Observacao: persistencia com Liquibase e modelagem core ja implementadas; evolucoes seguem no roadmap.

## 3) Modulos do ecossistema (fora deste repo)
- WebApp (React): cliente visual para Crianca e Pais.
- Automacao (n8n + WhatsApp/Evo API + OCR/IA): ingestao automatizada de comprovantes e notas.

## 4) Responsabilidades da API (core)
- Persistir e consultar dados financeiros da conta da crianca.
- Aplicar regras de negocio e validacoes.
- Expor endpoints para WebApp e n8n.
- Garantir autenticacao/autorizacao por perfil.
- Garantir auditabilidade das transacoes.

## 5) Regras de negocio essenciais
- Conta da crianca com saldo unico e historico de transacoes.
- Transacao imutavel com tipo `DEPOSIT` ou `WITHDRAW`.
- Origem obrigatoria: `WHATSAPP`, `MANUAL` ou `BONUS`.
- Saldo derivado do historico, nunca de campo manual isolado.
- Proibido saldo negativo na fase inicial.
- Ajustes futuros devem gerar novas transacoes (nao apagar historico).

## 6) Perfis e acesso (visao inicial)
- `CRIANCA`: leitura de saldo, metas e historico simplificado.
- `PAI/MAE`: leitura total e escrita em transacoes/regras/metas.
- Integracao de automacao: endpoint seguro para insercao automatizada.

## 7) Contratos de integracao (conceitual)
- Entrada de transacao deve conter:
  - tipo
  - valor
  - descricao curta
  - data/hora
  - origem
  - evidencia opcional (id/hash/url interna)
- Resposta esperada para criacao:
  - id da transacao
  - saldo atualizado
  - erros claros quando rejeitar (ex.: saldo insuficiente)

## 8) Roadmap tecnico de referencia
- Fase 1: base da API (entidades, migrations, transacoes, saldo, resumo, validacoes).
- Fase 2: endpoint seguro para automacao n8n/WhatsApp.
- Fase 3: suporte total ao WebApp (telas crianca e pais).
- Fase 4: bonus por guardar, metas evoluidas e relatorios mensais.

## 9) Decisoes de arquitetura para manter
- API nao deve depender de n8n nem do front para existir.
- Consistencia de saldo e historico auditavel sao inegociaveis.
- Regras de bonus devem ser explicitas e versionaveis quando entrarem em producao.

## 10) Status atual
- Repositorio com base funcional entregue (transacoes, saldo/resumo, metas, regra de bonus e automacao segura).
- Seguranca por JWT (usuarios) e token dedicado (automacao) implementada.
- Proxima prioridade: trilha de auditoria, gestao administrativa de vinculos `account_users` e endurecimento de ambiente/CI.
