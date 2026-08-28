package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, Long> {

    @Query("""
            select message from DirectMessageEntity message
            where (message.senderUsername = :firstUser and message.recipientUsername = :secondUser)
               or (message.senderUsername = :secondUser and message.recipientUsername = :firstUser)
            order by message.sentAt asc, message.id asc
            """)
    List<DirectMessageEntity> findConversation(String firstUser, String secondUser);
}
