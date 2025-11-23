package com.medac.trello.api.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    private final Resend resendApi;

    public EmailService(@Value("${app.resend-api-key}") String resendApiKey) {
        this.resendApi = new Resend(resendApiKey);
    }


    public void sendConfirmationEmail(String toEmail, String token) {
        LOG.info("📬 Entrando a EmailService.sendConfirmationEmail()");

        try {

            String confirmationUrl = "http://localhost:8080/trello/v1/auth/confirm?token=" + token;
            String emailContent = String.format(
                    "¡Hola! Gracias por registrarte.\n\nPor favor, haz clic en el siguiente enlace:\n%s",
                    confirmationUrl
            );

            LOG.info("📨 Enviando correo a {}", toEmail);
            sendEmail(toEmail, "Confirma tu Cuenta en Trello App", emailContent);
            LOG.info("✅ Correo enviado correctamente");

        } catch (Exception e) {
            LOG.error("❌ Error enviando correo: ", e);
        }
    }

    public void sendBoardInvitation(String toEmail, String boardName, String acceptanceLink) {
        LOG.info("📬 Entrando a EmailService.sendBoardInvitation()");
        try {
            String emailContent = String.format(
                    "¡Hola! Te han invitado a colaborar en el tablero '%s'.\n\n" +
                            "Haz clic en el enlace para aceptar la invitación y unirte:\n%s",
                    boardName, acceptanceLink
            );

            LOG.info("📨 Enviando invitación a {}", toEmail);
            sendEmail(toEmail, "¡Has sido invitado al tablero de Trello: " + boardName + "!", emailContent);
            LOG.info("✅ Correo de invitación enviado correctamente");

        } catch (ResendException e) {
            LOG.error("❌ ERROR AL ENVIAR CORREO DE INVITACIÓN:", e);
        }
    }

    private void sendEmail(String toEmail, String subject, String body) throws ResendException{
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("equipoflomind@gmail.com");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("equipoflomind@gmail.com")
                .to(toEmail)
                .text(body)
                .subject(subject)
                .build();
        var data = resendApi.emails().send(params);
        LOG.info("Email sent correctly. ID = {}", data.getId());
    }

}