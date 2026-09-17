package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;

import java.time.Instant;
import java.util.List;

interface MessageStore {

    void save(DirectMessage message);

    List<DirectMessage> conversation(String firstUser, String secondUser);
    void markConversationRead(String viewer, String otherUser);
    void archiveConversation(String viewer, String otherUser);
    long deleteOlderThan(Instant cutoff);
}
