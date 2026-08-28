package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;

import java.util.List;

interface MessageStore {

    void save(DirectMessage message);

    List<DirectMessage> conversation(String firstUser, String secondUser);
}
