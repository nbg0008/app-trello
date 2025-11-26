package com.medac.trello.api.model.controller;

import com.medac.trello.api.dto.SubscriptionRequestDTO;
import com.medac.trello.api.model.User;
import com.medac.trello.api.service.StripeService; // <-- Nuevo servicio para interactuar con Stripe API
import com.medac.trello.api.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException; // Para manejar la seguridad del webhook
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final StripeService stripeService; // <-- Inyectar el servicio de Stripe

    // Clave secreta para la verificación del Webhook (debe estar en application.properties)
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public SubscriptionController(SubscriptionService subscriptionService, StripeService stripeService) {
        this.subscriptionService = subscriptionService;
        this.stripeService = stripeService;
    }

    // ---ENDPOINT PARA INICIAR EL CHECKOUT (LLAMADO DESDE EL FRONTEND) ---
    // Devuelve una URL de Stripe para que el usuario sea redirigido y pague.
    @PostMapping("/checkout")
    public ResponseEntity<String> createCheckoutSession(
            @AuthenticationPrincipal User user,
            @RequestBody SubscriptionRequestDTO request
    ) {
        try {
            // aquí se gestiona la creación del customer y la sesión de Stripe
            String checkoutSessionUrl = stripeService.createSubscriptionCheckoutSession(
                    user,
                    request.getPlanName() // Asumimos que es el Price ID de Stripe
            );

            // Retornamos la URL a la que el frontend debe redirigir al usuario.
            return ResponseEntity.ok(checkoutSessionUrl);

        } catch (Exception e) {
            // Manejar errores de la API de Stripe o de negocio
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear la sesión de pago: " + e.getMessage());
        }
    }

    // --- ENDPOINT QUE LLAMA STRIPE (EL WEBHOOK) ---

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;

        // 1. Verificación de la firma de seguridad del Webhook
        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, webhookSecret
            );
        } catch (SignatureVerificationException e) {
            // Error en la firma: rechazar el request por seguridad
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Otro error al construir el evento
            return new ResponseEntity<>("Webhook processing failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            stripeService.handleWebhookEvent(event);
        } catch (Exception e) {

            System.err.println("Error processing Stripe event: " + e.getMessage());
        }

        return new ResponseEntity<>("Received", HttpStatus.OK);
    }

    // --- ENDPOINT PARA VERIFICAR EL ESTADO
    @GetMapping("/status")
    public ResponseEntity<Boolean> getSubscriptionStatus(@AuthenticationPrincipal User user) {
        boolean status = subscriptionService.isUserSubscribed(user);
        return ResponseEntity.ok(status);
    }
}