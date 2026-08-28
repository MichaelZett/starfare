package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;

import java.util.List;

public interface MessageService {

    SendResult send(String from, String to, String text);

    List<DirectMessage> conversation(String firstUser, String secondUser);

    enum SendResult {
        DELIVERED,
        EMPTY,
        REJECTED,
        RECIPIENT_OFFLINE,
        NOT_VISIBLE
    }
}
