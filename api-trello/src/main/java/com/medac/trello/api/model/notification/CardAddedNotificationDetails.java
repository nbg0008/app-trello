package com.medac.trello.api.model.notification;

public record CardAddedNotificationDetails(
        String cardTitle,
        String listTitle,
        String boardTitle
) implements NotificationDetails {



    @Override
    public String buildDescription() {
        return String.format("La tarjeta %s ha sido añadida a la lista %s en el tablero %s",
                cardTitle, listTitle, boardTitle);
    }
}
