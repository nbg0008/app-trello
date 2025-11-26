package com.medac.trello.api.service;

import com.medac.trello.api.model.User;
import com.medac.trello.api.model.Subscription;
import com.medac.trello.api.model.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    //Verifica si el usuario actual tiene una suscripción premium activa.

    public boolean isUserSubscribed(User user) {
        // Busca si existe *alguna* suscripción activa para este usuario.
        return subscriptionRepository.existsByUserAndIsActive(user, true);
    }

    @Transactional
    public Subscription activateSubscription(User user, String stripeSubscriptionId) {

        // 1. Desactivar suscripciones anteriores (Crucial para mantener la coherencia)
        subscriptionRepository.setInactiveByUserId(user.getId());

        // 2. Crear y guardar la nueva suscripción activa.
        Subscription newSubscription = new Subscription(
                user,
                stripeSubscriptionId,
                LocalDate.now(), // La fecha de inicio es hoy
                LocalDate.now().plusDays(100),
                true // Marcar como activa
        );
        return subscriptionRepository.save(newSubscription);
    }
    @Transactional
    public void deactivateSubscription(User user) {
        subscriptionRepository.setInactiveByUserId(user.getId());
        System.out.println("Suscripción desactivada en BD para el usuario ID: " + user.getId());
    }
}
