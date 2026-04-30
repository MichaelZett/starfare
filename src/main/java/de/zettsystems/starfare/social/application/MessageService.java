package de.zettsystems.starfare.social.application;

public interface MessageService {

    SendResult send(String from, String to, String text);

    enum SendResult {
        DELIVERED,
        EMPTY,
        REJECTED,
        RECIPIENT_OFFLINE,
        NOT_VISIBLE
    }
}
