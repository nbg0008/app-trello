package com.medac.trello.api.service;

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.repository.UserRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class PasswordResetService {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Resend resendApi;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
    private static final int PASSWORD_LENGTH = 12;

    // CONSTRUCTOR
    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.resend-api-key}") String resendApiKey) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resendApi = new Resend(resendApiKey);

    }
    
    // Método para generar la contraseña
    private String generateRandomPassword() {
        return IntStream.range(0, PASSWORD_LENGTH)
                .map(i -> secureRandom.nextInt(CHARACTERS.length()))
                .mapToObj(CHARACTERS::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    // LÓGICA PRINCIPAL
    public void resetPassword(String email) { 
        final var user = userRepository.findByEmail(email) // <-- CORRECCIÓN: Usar findByEmail
            // Usamos la excepción correcta del proyecto
            .orElseThrow(() -> new ResourceNotFoundException("Email no encontrado."));

        String newPassword = generateRandomPassword(); // <-- Llama al método local
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        sendNewPasswordEmail(user.getEmail(), newPassword); // <-- Envío del correo
    }

    // MÉTODO PARA ENVIAR EL CORREO
    private void sendNewPasswordEmail(String toEmail, String newPassword) {
        try {
            final var mensaje = "Hola,\n\n"
                    + "Tu contraseña ha sido reseteada exitosamente. Tu nueva contraseña es:\n\n"
                    + newPassword + "\n\n"
                    + "Por favor, inicia sesión con esta contraseña y cámbiala lo antes posible.\n\n"
                    + "Gracias,\n"
                    + "El equipo de Trello.";
            
            sendEmail(toEmail, "Trello App: Reseteo de Contraseña Exitoso", mensaje);
        } catch (ResendException e) {
            LOG.error("Error al enviar el correo a " + toEmail + ": ", e);
        }
    }

    private void sendEmail(String toEmail, String subject, String body) throws ResendException {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("equipoflomind@flomind.es")
                .to(toEmail)
                .text(body)
                .subject(subject)
                .build();
        var data = resendApi.emails().send(params);
        LOG.info("Email sent correctly. ID = {}", data.getId());
    }
}