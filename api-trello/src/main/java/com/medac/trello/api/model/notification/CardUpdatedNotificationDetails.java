package com.medac.trello.api.model.notification;

public record CardUpdatedNotificationDetails<T>(
        String name,
        T fromValue,
        T toValue,
        CardDetail whatChanged) implements NotificationDetails {


    public enum CardDetail {
        NAME("titulo"),
        DESCRIPCION("descripcion"),
        STARTS_ON("fecha de inicio"),
        EXPIRES_ON("fecha de fin"),
        LISTA("lista"),
        ORDER("orden");

        public final String text;

        CardDetail(String text) {
            this.text = text;
        }
    }

    @Override
    public String buildDescription() {
        return switch (whatChanged) {
            case NAME ->  String.format("La tarjeta %s ha cambiado el %s a %s",
                    name, whatChanged.text, toValue.toString());
            case ORDER -> String.format("La tarjeta %s ha cambiado el %s de %s a %s",
                    name, whatChanged.text, fromValue.toString(), toValue.toString());
            case DESCRIPCION, STARTS_ON, EXPIRES_ON -> String.format("La tarjeta %s ha cambiado la %s de %s a %s",
                    name, whatChanged.text, fromValue.toString(), toValue.toString());
            case LISTA -> String.format("La tarjeta %s ha cambiado de %s de %s a %s",
                    name, whatChanged.text, fromValue.toString(), toValue.toString());
        };
    }
}
