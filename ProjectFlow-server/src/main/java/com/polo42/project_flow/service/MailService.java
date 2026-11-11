package com.polo42.project_flow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Envio de e-mails transacionais.
 * <p>
 * Com SMTP configurado (spring.mail.host / MAIL_HOST) envia normalmente. Sem SMTP (uso local),
 * nao envia nada: registra no log o conteudo da mensagem, incluindo o codigo/link de verificacao
 * ou recuperacao, para permitir rodar a aplicacao sem credenciais.
 */
@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String host;
    private final String from;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${spring.mail.from:no-reply@projectflow.local}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.host = host;
        this.from = StringUtils.hasText(from) ? from : "no-reply@projectflow.local";
    }

    public boolean isSmtpConfigured() {
        return StringUtils.hasText(host) && mailSenderProvider.getIfAvailable() != null;
    }

    /**
     * @throws MailException se o SMTP estiver configurado e o envio falhar
     */
    public void send(String to, String subject, String body) {
        if (!isSmtpConfigured()) {
            LOGGER.warn("SMTP não configurado (defina MAIL_HOST): e-mail NÃO enviado, conteúdo registrado abaixo."
                    + "\n--- E-MAIL (modo desenvolvimento) ---\nPara: {}\nAssunto: {}\n\n{}\n--- fim ---",
                    to, subject, body);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSenderProvider.getObject().send(message);
    }
}
