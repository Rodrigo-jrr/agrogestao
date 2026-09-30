package com.agrogestao.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/** E-mails transacionais da conta. Falhas de envio são registradas no log e não interrompem o fluxo. */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${agrogestao.mail.from}") String from,
            @Value("${agrogestao.app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    public void sendEmailVerification(User user, String token) {
        send(user.getEmail(), "Confirme seu e-mail no AgroGestão", """
                Olá, %s!

                Para confirmar que este e-mail é seu, abra o link abaixo:

                %s

                O link vale por 24 horas. Se você não criou uma conta no AgroGestão, ignore esta mensagem.
                """.formatted(user.getName(), link("/verificar-email", token)));
    }

    public void sendPasswordReset(User user, String token) {
        send(user.getEmail(), "Redefinição de senha do AgroGestão", """
                Olá, %s!

                Recebemos um pedido para redefinir a senha da sua conta. Para criar uma senha nova, abra o link:

                %s

                O link vale por 1 hora e só pode ser usado uma vez. Se não foi você que pediu, ignore esta
                mensagem: sua senha atual continua valendo.
                """.formatted(user.getName(), link("/redefinir-senha", token)));
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(appBaseUrl + path).queryParam("token", token).toUriString();
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Não logamos o corpo: ele contém o link com o token.
            log.error("Falha ao enviar e-mail '{}' para usuário: {}", subject, ex.getMessage());
        }
    }
}
