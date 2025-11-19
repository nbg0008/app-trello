package com.medac.trello.api.model.notification;

public record CardDeletedNotificationDetails(String cardTitle, String listaTitle)
        implements NotificationDetails {

    @Override
    public String buildDescription() {
        return String.format("La tarjeta %s ha sido borrada de la lista %s",
                cardTitle, listaTitle);
    }
}
