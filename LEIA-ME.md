# ProjectFlow Server

[![CI](https://github.com/TiagoJSaraiva/ProjectFlow-server/actions/workflows/ci.yml/badge.svg)](https://github.com/TiagoJSaraiva/ProjectFlow-server/actions/workflows/ci.yml)

**Leia em outros idiomas:** [English](README.md)

## Sumário

1. [Visão geral](#visão-geral)
2. [Funcionalidades](#funcionalidades)
3. [Stack e arquitetura](#stack-e-arquitetura)
4. [Modelo de dados](#modelo-de-dados)
5. [Endpoints da API](#endpoints-da-api)
6. [Pré-requisitos](#pré-requisitos)
7. [Variáveis de ambiente](#variáveis-de-ambiente)
8. [Executando com Docker Compose](#executando-com-docker-compose)
9. [Executando sem Docker](#executando-sem-docker)
10. [Banco de dados](#banco-de-dados)
11. [Testes](#testes)
12. [Integração contínua](#integração-contínua)
13. [Implantação](#implantação)
14. [Detalhes de implementação e limitações conhecidas](#detalhes-de-implementação-e-limitações-conhecidas)
15. [Créditos](#créditos)

## Visão geral

O ProjectFlow é um editor de diagramas de fluxo: o usuário se cadastra, cria projetos e desenha diagramas
(nós retangulares e circulares ligados por conexões) em um canvas. **Este repositório é a API REST**
(Spring Boot) que guarda contas, projetos e diagramas.

- Frontend (Angular): <https://github.com/TiagoJSaraiva/ProjectFlow-webapp>
- Demo pública: <https://tiagojsaraiva.github.io/ProjectFlow-webapp/>

> A demo pública roda em um **modo de demonstração inteiramente no navegador** e **não** usa este
> servidor. Não existe instância hospedada desta API; para usar o frontend com a API de verdade é preciso
> rodá-los localmente (veja abaixo).

O código do servidor fica em `ProjectFlow-server/`; a raiz do repositório contém o arquivo do Docker Compose,
o workflow de CI e esta documentação.

## Funcionalidades

- Cadastro com verificação de e-mail (token válido por 60 minutos) e login que devolve um JWT (24 h).
- Recuperação de senha por e-mail (token válido por 30 minutos) e troca de senha do usuário logado.
- Projetos por usuário: criar, listar, ler, renomear/descrever e excluir. Cada projeto tem um diagrama.
- Persistência do diagrama: nós (`RECTANGLE`, `CIRCLE`; nome de até 35 caracteres), conexões com ponto de
  dobra e estilo, e o viewport (zoom, deslocamento, tamanho da grade). Salvar substitui o diagrama inteiro.
- Limites do diagrama: no máximo 500 nós e 1.000 conexões, sem ids repetidos em um mesmo salvamento.
- `ultimoAcesso` do projeto (atualizado ao carregar o diagrama) e `ultimaAlteracao` (atualizada ao salvar).
- Autorização por dono: toda operação sobre projeto, diagrama e usuário é restrita ao dono do JWT;
  recurso de outro usuário responde `404`.
- Exclusão de conta que remove todos os dados do usuário em uma única transação.
- Atualização de conta: só o nome pode ser alterado (o e-mail é fixo); a troca de senha exige a senha atual.
- Tokens emitidos antes de uma troca ou redefinição de senha deixam de ser aceitos.
- SMTP opcional: sem ele, os links de verificação e recuperação são escritos no log do servidor.
- Datas serializadas como strings ISO-8601 e geradas em UTC.
- Uma API legada `/api/elements` (modelo antigo de elementos) continua presente; o frontend não a usa.

## Stack e arquitetura

| Item | Escolha |
|---|---|
| Linguagem / build | Java 21, Maven Wrapper (`mvnw`) |
| Framework | Spring Boot 3.3.2 (Web, Data JPA, Security, Mail, Validation) |
| Banco | MySQL 8 (esquema criado pelo Hibernate com `ddl-auto=update`) |
| Autenticação | JWT sem estado (jjwt 0.11.5, HS512), senhas com hash BCrypt |
| Testes | JUnit 5, Spring Boot Test, MockMvc, H2 em modo MySQL |

Pacote `com.polo42.project_flow`, nas camadas habituais:

- `controller`: controllers REST (`AuthController`, `UserController`, `ProjectController`,
  `DiagramController`, `ElementController`).
- `service`: regras de negócio e verificação de dono (`ProjectService`, `DiagramService`, `UserService`,
  `AuthService`, `EmailVerificationService`, `PasswordResetService`, `MailService`, `ElementService`).
- `repository`: repositórios Spring Data JPA.
- `model` / `dto`: entidades JPA e objetos de requisição/resposta.
- `security`: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, handlers JSON de 401/403.
- `exception`: `GlobalExceptionHandler`, que devolve corpo JSON com o campo `message`.
- `config`: configuração de CORS e Jackson.

Como funcionam os fluxos principais:

- **JWT**: `POST /api/auth/login` devolve um token cujo subject é o id do usuário. O filtro carrega o usuário
  a cada requisição. O token também leva a claim `pv` com o carimbo da última troca de senha do usuário
  (epoch em milissegundos, `0` se nunca trocou); se não bater, o token é rejeitado com `401`.
- **Verificação de e-mail e recuperação de senha**: tokens UUID aleatórios ficam no banco
  (`email_verification_tokens`, `password_reset_tokens`) e são enviados como links montados a partir de
  `FRONTEND_BASE_URL` (`/verify-email?token=...`, `/reset?token=...`). Cada token é de uso único.
- **E-mail opcional**: o `MailService` só envia por SMTP quando `MAIL_HOST` está definido. Caso contrário,
  registra um aviso com a mensagem inteira, incluindo o link ou código, e não envia nada.
- **Erros**: o campo `{"message": "..."}` está sempre presente; veja os códigos abaixo.
- **Tempo**: o fuso padrão da JVM é forçado para UTC e o Hibernate usa `jdbc.time_zone=UTC`.

## Modelo de dados

| Tabela | Finalidade |
|---|---|
| `usuarios` | Contas: nome, e-mail único, hash BCrypt, flag de verificação, `senha_alterada_em` |
| `projetos` | Projetos: nome, descrição, criado / última alteração / último acesso, dono |
| `diagram_nodes` | Nós: id UUID gerado pelo cliente, tipo, `nome` (máx. 35), descrição, posição, tamanho/raio |
| `diagram_connections` | Conexões entre dois nós, ponto de dobra e estilo (JSON) |
| `diagram_view_states` | Viewport do projeto (uma linha por projeto) |
| `email_verification_tokens` | Tokens de verificação de e-mail |
| `password_reset_tokens` | Tokens de redefinição de senha |
| `elementos` | Modelo legado de elementos (usado só por `/api/elements`) |

As chaves estrangeiras não têm `ON DELETE CASCADE` no banco; a aplicação apaga as linhas dependentes.

## Endpoints da API

URL base: `http://localhost:8080`. Endpoints protegidos exigem `Authorization: Bearer <jwt>`.
Recursos de outro usuário respondem `404`. Datas são strings ISO-8601 sem fuso, em UTC.

| Método | Caminho | Autenticação | Descrição |
|---|---|---|---|
| POST | `/api/auth/register` | pública | Cadastro (`nome`, `email`, `senha` com ao menos 6 caracteres); envia o e-mail de verificação |
| POST | `/api/auth/login` | pública | Login (`email`, `senha`); devolve `token`, `userId`, `nome`, `email`. Credenciais erradas ou e-mail não verificado: `422` |
| POST | `/api/auth/verify-email` | pública | Confirma o e-mail com `token` |
| POST | `/api/auth/resend-verification` | pública | Reenvia o e-mail de verificação (`email`); sempre `200` com corpo vazio, exista ou não a conta e já esteja ou não verificada |
| POST | `/api/auth/recover` | pública | Solicita o e-mail de redefinição de senha (`email`) |
| POST | `/api/auth/reset-password` | pública | Define nova senha (`token`, `novaSenha`) |
| GET | `/api/users` | JWT | Lista usuários: devolve apenas quem chama |
| GET | `/api/users/{id}` | JWT | Dados da própria conta |
| PUT | `/api/users/{id}` | JWT | Atualiza o próprio nome (`nome`, 2 a 100 caracteres). `email` só pode ser enviado se for igual ao atual; tentar mudá-lo responde `422` |
| PUT | `/api/users/{id}/password` | JWT | Troca a própria senha: JSON `{"senhaAtual", "novaSenha"}` (nova senha de 6 a 100 caracteres); senha atual errada `422`; `204` e invalida os tokens existentes |
| DELETE | `/api/users/{id}` | JWT | Exclui a própria conta e todos os seus dados (`204`) |
| GET | `/api/projects/user/{userId}` | JWT | Lista os próprios projetos (`userId` deve ser o de quem chama) |
| POST | `/api/projects/user/{userId}` | JWT | Cria projeto (`nome` obrigatório, até 100; `descricao` até 10.000) |
| GET | `/api/projects/{id}` | JWT | Lê um projeto |
| PUT | `/api/projects/{id}` | JWT | Atualiza nome e descrição |
| DELETE | `/api/projects/{id}` | JWT | Exclui projeto, diagrama e elementos (`204`) |
| GET | `/api/projects/{projectId}/diagram` | JWT | Carrega o diagrama; atualiza `ultimoAcesso` |
| PUT | `/api/projects/{projectId}/diagram` | JWT | Substitui o diagrama e grava nome/descrição do projeto; atualiza `ultimaAlteracao` |
| GET, POST | `/api/elements/project/{projectId}` | JWT | Legado: lista / cria elementos |
| GET, PUT, DELETE | `/api/elements/{id}` | JWT | Legado: lê / atualiza / exclui um elemento |
| PUT | `/api/elements/{id}/content`, `/coordinates`, `/relations` | JWT | Legado: atualizações parciais (corpo em texto puro) |

Códigos de erro (todos os corpos trazem `message`):

| Status | Significado |
|---|---|
| 400 | Requisição inválida: validação de DTO (um campo por erro mais `message`), JSON malformado, UUID ou enum inválido |
| 401 | Token ausente, inválido ou expirado, ou emitido antes da última troca de senha |
| 403 | Acesso negado |
| 404 | Recurso inexistente, de outro usuário, ou rota desconhecida |
| 405 | Método HTTP não suportado |
| 415 | Tipo de conteúdo não suportado |
| 422 | Regra de negócio: e-mail em uso, token inválido, tentativa de trocar o e-mail, senha atual incorreta, nome de nó com mais de 35 caracteres, ids repetidos ou excesso de nós/conexões no diagrama, id de nó/conexão já usado por outro projeto, login recusado |
| 500 | Erro inesperado (registrado no log) |

O mesmo contrato está descrito em `ProjectFlow-server/ENDPOINTS - PROJECTFLOW.txt`.

## Pré-requisitos

- Docker com Compose, **ou**
- JDK 21 e um servidor MySQL 8 (o Maven Wrapper baixa o próprio Maven).

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `JWT_SECRET` | sim | nenhum | Segredo de assinatura do JWT, mínimo de 64 caracteres, gerado por você (`openssl rand -base64 64`). O servidor não sobe se estiver vazio, curto, for um valor de exemplo ou um segredo que foi publicado no início do histórico deste repositório |
| `DB_USERNAME` | sim | nenhum | Usuário do banco |
| `DB_PASSWORD` | sim | nenhum | Senha do banco |
| `DB_URL` | não | `jdbc:mysql://localhost:3306/project_flow?...` | URL JDBC (o compose aponta para o serviço `db`) |
| `DB_ROOT_PASSWORD` | só no compose | nenhum | Senha root do MySQL do container de banco |
| `CORS_ALLOWED_ORIGINS` | não | `http://localhost:4200` | Origens permitidas, separadas por vírgula |
| `FRONTEND_BASE_URL` | não | `http://localhost:4200` | URL base usada nos links dos e-mails |
| `SERVER_PORT` | não | `8080` | Porta publicada no host pelo Docker Compose |
| `MAIL_HOST` | não | vazio | Host SMTP; vazio desativa o envio (links vão para o log) |
| `MAIL_PORT` | não | `587` | Porta SMTP |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | não | vazio | Credenciais SMTP |
| `MAIL_FROM` | não | `MAIL_USERNAME` ou `no-reply@projectflow.local` | Remetente |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | não | `true` | Opções SMTP |

O `.env.example` na raiz lista as variáveis com valores fictícios. Copie para `.env` (nunca versionado) e edite.

## Executando com Docker Compose

```bash
cp .env.example .env
# edite o .env: defina as senhas do banco e um JWT_SECRET seu, por exemplo a saída de:
#   openssl rand -base64 64
docker compose up --build
```

O `JWT_SECRET` fica vazio de propósito no `.env.example`: o Compose e o servidor se recusam a iniciar até que você o preencha.

Isso sobe o MySQL 8 (sem publicar porta no host, para não conflitar com um MySQL local) e o servidor em
`http://localhost:8080`. Sem `MAIL_HOST`, leia os links de verificação e recuperação com
`docker compose logs server`.

> A configuração do Compose foi executada de ponta a ponta no Docker 28 (build da imagem, MySQL 8, cadastro,
> verificação de e-mail pelo fallback no log, login, operações de projeto e diagrama, isolamento por dono e
> exclusão de conta). O primeiro build baixa todas as dependências do Maven e pode levar vários minutos.

## Executando sem Docker

1. Crie o banco: `CREATE DATABASE project_flow;`.
2. Forneça as variáveis. Copie `.env.example` para `ProjectFlow-server/.env` e preencha (lido do diretório de
   trabalho quando a aplicação inicia pelo `main`, por exemplo `spring-boot:run` ou `java -jar`) ou exporte-as no
   shell; uma variável já definida no ambiente tem prioridade sobre o `.env`.
   `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` são obrigatórias.
3. Inicie o servidor:

```bash
cd ProjectFlow-server
./mvnw spring-boot:run
```

Configure o frontend Angular para usar `http://localhost:8080` como URL da API.

## Banco de dados

As tabelas são criadas e ampliadas pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`); não há scripts de
migração. O antigo `schema.sql` foi removido porque o MySQL nunca o executava e ele não correspondia mais às entidades.

O `ddl-auto=update` adiciona tabelas e colunas novas, mas não altera as existentes. Um banco MySQL criado por
uma versão anterior precisa de:

```sql
-- o nome do nó agora aceita até 35 caracteres (antes 20)
ALTER TABLE diagram_nodes MODIFY nome VARCHAR(35) NOT NULL;
-- adicionada automaticamente pelo Hibernate; equivalente manual, se preferir
ALTER TABLE usuarios ADD COLUMN senha_alterada_em DATETIME(6) NULL;
```

Os horários já gravados por versões anteriores foram escritos no fuso local do servidor; os novos são UTC.

## Testes

```bash
cd ProjectFlow-server
./mvnw test
```

Os testes são de integração com MockMvc e sobem a aplicação inteira com o perfil `test`: H2 em memória em modo
MySQL, segredo JWT exclusivo de teste e `JavaMailSender` simulado. Não precisam de MySQL, SMTP nem Docker.
Cobrem o carregamento do contexto, a autorização por dono em todos os endpoints com dois usuários, um teste por
bug corrigido (persistência do diagrama, exclusão de conta, códigos HTTP, datas ISO, nome de nó com 35
caracteres), os caminhos de e-mail com e sem SMTP, datas em UTC, a invalidação de tokens após troca de senha, a
validação de atualização de usuário e de senha, os limites do diagrama, tokens de verificação e redefinição
(expirados, reutilizados), login sem e-mail verificado, JWT forjado e expirado, `resend-verification` e a recusa,
na inicialização, de segredo JWT vazio, curto ou de exemplo.

## Integração contínua

`.github/workflows/ci.yml` executa `./mvnw -B test` com JDK 21 (diretório `ProjectFlow-server`) a cada push e
pull request para `main`.

## Implantação

**Não existe instância pública** desta API. A demo pública do frontend não a chama. Hospedá-la exigiria:

- uma máquina ou plataforma de contêineres capaz de rodar uma JVM (o `Dockerfile` gera uma imagem executável);
- um banco MySQL 8 gerenciado ou próprio;
- um `JWT_SECRET` forte, credenciais do banco e `CORS_ALLOWED_ORIGINS` com a origem do frontend, guardados como segredos;
- HTTPS, pois um frontend em HTTPS não pode chamar uma API em HTTP;
- um provedor SMTP (ou aceitar que os links só apareçam no log) e `FRONTEND_BASE_URL` apontando para o frontend;
- o frontend configurado com a URL da API e, antes de expor publicamente, as limitações abaixo tratadas.

## Detalhes de implementação e limitações conhecidas

- O diagrama é salvo por inteiro: as linhas existentes são apagadas e reinseridas. Não há bloqueio otimista;
  salvamentos concorrentes do mesmo projeto se sobrescrevem.
- Os ids de nós e conexões são gerados pelo cliente. Um salvamento que reutilize um id de outro projeto é recusado com `422`.
- Falha de autorização responde `404` (e não `403`) para não revelar ids de recursos de outros usuários.
- Troca ou redefinição de senha invalida todos os tokens anteriores, inclusive o de quem trocou; o cliente precisa fazer login de novo.
- Falhas de login usam `422` com mensagem genérica, não `401`.
- Não há limite de tentativas (rate limiting) em login, cadastro, recuperação e reenvio. O cadastro ainda responde `422` "e-mail já em uso",
  o que revela que um endereço está cadastrado (recuperação e reenvio respondem igual para endereços conhecidos e desconhecidos).
- Os tokens de verificação e redefinição ficam em texto puro no banco; os já usados não são removidos.
- O JWT dura 24 horas; não há refresh token nem revogação além da troca de senha.
- No modo de desenvolvimento (sem SMTP) o log contém os links de verificação e recuperação: não use esse modo com usuários reais.
- O esquema depende de `ddl-auto=update`; não há migrações versionadas.
- A API legada `/api/elements` e a tabela `elementos` não são usadas pelo frontend.
- A configuração Docker foi validada apenas localmente (nunca em uma plataforma de hospedagem), e o workflow do GitHub depende do runner hospedado.

## Créditos

Escrito por **Tiago J. Saraiva** (autor de todos os commits do histórico deste repositório).
