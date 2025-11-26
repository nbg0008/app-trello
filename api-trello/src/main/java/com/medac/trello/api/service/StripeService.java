package com.medac.trello.api.service;

import com.medac.trello.api.model.User;
import com.medac.trello.api.model.repository.UserRepository; // Necesario para guardar el Stripe ID
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.model.Subscription;

import java.util.Optional;

@Service
public class StripeService {

    @Value("${stripe.api.secretKey}")
    private String secretKey;

    @Value("${app.domain.success-url}")
    private String successUrl;

    @Value("${app.domain.cancel-url}")
    private String cancelUrl;

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public StripeService(SubscriptionService subscriptionService, UserRepository userRepository) {
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
    }

    // Inicializa la clave de Stripe al iniciar el servicio
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    //Obtiene el ID de Cliente de Stripe. Si no existe en la BD, lo crea en Stripe y lo guarda.

    private String getOrCreateStripeCustomerId(User user) throws Exception {
        // 1. Si el usuario ya tiene un Stripe ID, lo retornamos
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }

        // 2. Si no lo tiene, creamos uno nuevo en Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .putMetadata("userId", user.getId().toString())
                .build();

        Customer customer = Customer.create(params);

        // 3. Guardamos el nuevo ID en la base de datos del usuario
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);

        return customer.getId();
    }

    public String createSubscriptionCheckoutSession(User user, String priceId) throws Exception {

        String stripeCustomerId = getOrCreateStripeCustomerId(user);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomerId)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                // Usamos metadata para enviar el ID del plan de vuelta al webhook
                .putMetadata("user_id", user.getId().toString())
                .putMetadata("plan_id", priceId)
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public void handleWebhookEvent(Event event) throws StripeException {
        // La clave API ya está configurada en @PostConstruct

        Optional<StripeObject> optionalStripeObject = event.getDataObjectDeserializer().getObject();
        if (!optionalStripeObject.isPresent()) {
            throw new IllegalStateException("Cuerpo del evento de Stripe vacío.");
        }
        StripeObject stripeObject = optionalStripeObject.get();

        switch (event.getType()) {
            case "checkout.session.completed":
                // Este es el evento clave: el cliente pagó y se creó una suscripción.
                Session session = (Session) stripeObject;
                handleCheckoutSessionCompleted(session);
                break;

            case "customer.subscription.deleted":
                // La suscripción fue cancelada por el cliente o Stripe.
                Subscription subscriptionDeleted = (Subscription) stripeObject;
                handleSubscriptionDeleted(subscriptionDeleted);
                break;

            case "invoice.payment_failed":

                break;

            default:

                System.out.println("Unhandled event type: " + event.getType());
                break;
        }
    }

    // Lógica específica para cuando la sesión de checkout se completa
    private void handleCheckoutSessionCompleted(Session session) throws StripeException {
        String userIdStr = session.getClientReferenceId();

        String subscriptionId = session.getSubscription();

        if (userIdStr != null && subscriptionId != null) {

            Optional<User> userOptional = userRepository.findById(Long.parseLong(userIdStr));

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 2. Activar la suscripción en tu base de datos y guardar el Subscription ID
                subscriptionService.activateSubscription(user, subscriptionId);

                System.out.println("Suscripción activada para el usuario: " + userIdStr +
                        " con Subscription ID: " + subscriptionId);
            } else {
                System.err.println("Error: Usuario no encontrado para el ID: " + userIdStr);
            }
        }
    }

    // Lógica específica para cuando se cancela una suscripción
    private void handleSubscriptionDeleted(Subscription subscription) {
        String customerId = subscription.getCustomer();

        // 1. Encontrar el usuario por su Customer ID de Stripe
        Optional<User> userOptional = userRepository.findByStripeCustomerId(customerId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 2. Desactivar la suscripción del usuario en tu base de datos
            subscriptionService.deactivateSubscription(user);

            System.out.println("Suscripción desactivada para el usuario (Stripe Customer ID): " + customerId);
        } else {
            System.err.println("Error: Usuario no encontrado para el Stripe Customer ID: " + customerId);
        }
    }

    // Métodos de repositorio existentes
    public Optional<User> findUserByStripeCustomerId(String stripeCustomerId) {
        return userRepository.findByStripeCustomerId(stripeCustomerId);
    }

    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }
}


