package com.medac.trello.api.model.notification;

public record ListaUpdatedNotificationDetails<T>(
        String name,
        T fromValue,
        T toValue,
        ListaDetail whatChanged) implements NotificationDetails {


    public enum ListaDetail {
        NAME("nombre"), ORDER("orden"), BOARD("tablero");

        public final String text;

        ListaDetail(String text) {
            this.text = text;
        }
    }

    @Override
    public String buildDescription() {
        return switch (whatChanged) {
            case NAME, ORDER -> String.format("La lista %s ha cambiado el %s de %s a %s",
                    name, whatChanged.text, val(fromValue), val(toValue));
            case BOARD -> String.format("La lista %s ha cambiado de %s de %s a %s",
                    name, whatChanged.text, val(fromValue), val(toValue));
        };
    }

    private String val(Object value) {
        return value == null ? "-" : value.toString();
    }
}
