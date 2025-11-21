# ProjectFlow Server

## Configuração de email (Gmail SMTP)

Os fluxos de cadastro, verificação de conta e recuperação de senha exigem o envio de emails. O backend já está pronto para usar o SMTP do Gmail por padrão.

### 1. Pré-requisitos na conta

1. Ative a verificação em duas etapas (2FA) na sua conta Google.
2. Gere uma **senha de app** em https://myaccount.google.com/security → "Senhas de app" (selecionar "Correio" e nomear o dispositivo como "ProjectFlow").
3. Guarde o código de 16 caracteres — ele será usado como senha SMTP.

### 2. Variáveis de ambiente necessárias

Defina as variáveis antes de iniciar o Spring Boot (no PowerShell, execute e reabra o terminal):

```powershell
setx MAIL_USERNAME "seu-email@gmail.com"
setx MAIL_PASSWORD "senha-de-app-gerada"
```

Opcionalmente você pode sobrescrever qualquer outro parâmetro:

- `MAIL_HOST` (default `smtp.gmail.com`)
- `MAIL_PORT` (default `587`)
- `MAIL_SMTP_AUTH` (default `true`)
- `MAIL_SMTP_STARTTLS` (default `true`)
- `MAIL_FROM` (por padrão usa o mesmo valor de `MAIL_USERNAME`)

### 3. Testando

1. `cd ProjectFlow-server`
2. `./mvnw spring-boot:run`
3. Utilize o frontend ou um cliente REST para chamar `/api/auth/register`, `/api/auth/recover` ou `/api/auth/resend-verification`.
4. Verifique a caixa de entrada do Gmail configurado — as mensagens devem chegar imediatamente. Em caso de erro, o backend retornará HTTP 422 com a mensagem detalhada.

> **Importante:** o Gmail impõe limites de envio diários para contas pessoais. Para grandes volumes em produção considere provedores dedicados (SendGrid, Amazon SES, etc.).
