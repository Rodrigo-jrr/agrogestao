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

    /** Vai para o endereço novo: só ele pode concluir a troca. */
    public void sendEmailChangeConfirmation(User user, String newEmail, String token) {
        send(newEmail, "Confirme seu novo e-mail no AgroGestão", """
                Olá, %s!

                Você pediu para usar este endereço na sua conta do AgroGestão. Para confirmar, abra o link:

                %s

                O link vale por 24 horas. Até lá, o login continua com o e-mail antigo. Se não foi você,
                ignore esta mensagem.
                """.formatted(user.getName(), link("/confirmar-novo-email", token)));
    }

    /** Aviso ao endereço atual: se não foi a pessoa, ela fica sabendo antes de perder o acesso. */
    public void sendEmailChangeNotice(User user, String newEmail) {
        send(user.getEmail(), "Pedido de troca de e-mail no AgroGestão", """
                Olá, %s!

                Recebemos um pedido para trocar o e-mail da sua conta para %s. A troca só acontece quando o
                novo endereço for confirmado.

                Se não foi você, troque sua senha agora em "Meu perfil" ou pelo "Esqueci minha senha".
                """.formatted(user.getName(), newEmail));
    }

    public void sendPasswordChangedNotice(User user) {
        send(user.getEmail(), "Sua senha do AgroGestão foi alterada", """
                Olá, %s!

                A senha da sua conta acabou de ser alterada e as outras sessões abertas foram encerradas.

                Se não foi você, use "Esqueci minha senha" na tela de login para criar uma nova:
                %s
                """.formatted(user.getName(), appBaseUrl + "/esqueci-senha"));
    }

    /** Convite para quem ainda não tem conta. */
    public void sendInvitation(String email, String inviterName, String propertyName, String roleLabel, String token) {
        send(email, inviterName + " convidou você para o AgroGestão", """
                Olá!

                %s convidou você para a equipe da propriedade %s no AgroGestão, como %s.

                Para aceitar, abra o link abaixo e crie sua conta:

                %s

                O convite vale por 7 dias. Se você não conhece quem enviou, ignore esta mensagem.
                """.formatted(inviterName, propertyName, roleLabel.toLowerCase(), link("/convite", token)));
    }

    /** Aviso para quem já tinha conta e foi adicionado direto à equipe. */
    public void sendAddedToProperty(User user, String inviterName, String propertyName, String roleLabel) {
        send(user.getEmail(), "Você entrou na equipe de " + propertyName, """
                Olá, %s!

                %s adicionou você à equipe da propriedade %s no AgroGestão, como %s.

                A propriedade já aparece na sua conta: %s
                """.formatted(user.getName(), inviterName, propertyName, roleLabel.toLowerCase(), appBaseUrl + "/login"));
    }

    public void sendAccountDeletedNotice(String email, String name) {
        send(email, "Sua conta do AgroGestão foi excluída", """
                Olá, %s!

                Sua conta no AgroGestão foi excluída, junto com seus dados pessoais e as propriedades em que você
                era a única pessoa da equipe. Este é o último e-mail que enviamos para este endereço.

                Se não foi você que pediu a exclusão, fale conosco pelo contato indicado na Política de
                privacidade: %s
                """.formatted(name, appBaseUrl + "/privacidade"));
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
