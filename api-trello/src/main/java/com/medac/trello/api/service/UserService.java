package com.medac.trello.api.service;

import com.medac.trello.api.exception.ResourceNotFoundException;
import com.medac.trello.api.model.User;
import com.medac.trello.api.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // ⬅️ ¡Nueva Importación!
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    //------------------------------------FUNCIONALIDADES DE CONSULTA----------------

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByStripeCustomerId(String customerId) {
        return userRepository.findByStripeCustomerId(customerId);
    }



    // 🎯 Inyección por constructor
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ---------------------- FUNCIONALIDAD DE REGISTRO ----------------------


    @Transactional
    public User registerUser(User newUser) {
        // 1. [Opcional] Verificar si el email ya está en uso
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        // 2. Cifrar la contraseña
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        // 3. Generar token y establecer la verificación a FALSE
        String token = UUID.randomUUID().toString();
        newUser.setConfirmationToken(token);
        newUser.setVerified(false);

        User savedUser = userRepository.save(newUser);


        // 4. Envío del email (Ahora que el usuario está guardado)
        emailService.sendConfirmationEmail(savedUser.getEmail(), savedUser.getConfirmationToken());

        return savedUser;
    }


    @Transactional
    public User verifyUser(String token) {
        User user = userRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación inválido."));

        if (user.isVerified()) {
            return user; // Ya verificado
        }

        user.setVerified(true);
        user.setConfirmationToken(null); // Limpiar el token usado

        return userRepository.save(user);
    }

    // ---------------------- FUNCIONALIDAD DE OAUTH2 ----------------------

    public User findOrCreateOAuthUser(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User(
                            name,
                            email,
                            email,
                            null
                    );


                    newUser.setVerified(true);

                    return userRepository.save(newUser);
                });
    }

    // ---------------------- FUNCIONALIDAD DE EDICIÓN ----------------------


    public User updateUsername(Long userId, String newUsername) {

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        if (userRepository.findByUsername(newUsername).isPresent()) {
            User existingUser = userRepository.findByUsername(newUsername).get();

            // Si el usuario existente NO es el que estamos actualizando (diferente ID)
            if (!existingUser.getId().equals(userId)) {
                throw new IllegalArgumentException("El nombre de usuario " + newUsername + " ya está en uso por otro usuario.");
            }
            // Si el usuario existente ES el mismo que estamos actualizando, no hacemos nada y continuamos.
        }

        // 3. Actualizar y guardar
        userToUpdate.setUsername(newUsername);
        return userRepository.save(userToUpdate);
    }


}