package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, Long> {

    @Query("""
            select message from DirectMessageEntity message
            where ((message.senderPlayerId = :firstUser and message.recipientPlayerId = :secondUser)
               or (message.senderPlayerId = :secondUser and message.recipientPlayerId = :firstUser))
              and ((message.senderPlayerId = :viewer and message.senderArchivedAt is null)
                or (message.recipientPlayerId = :viewer and message.recipientArchivedAt is null))
            order by message.sentAt asc, message.id asc
            """)
    List<DirectMessageEntity> findConversation(@Param("firstUser") String firstUser, @Param("secondUser") String secondUser,
                                               @Param("viewer") String viewer);
    @Modifying @Query("update DirectMessageEntity message set message.readAt = :at where message.senderPlayerId = :other and message.recipientPlayerId = :viewer and message.readAt is null")
    int markConversationRead(@Param("viewer") String viewer, @Param("other") String other, @Param("at") Instant at);
    @Modifying @Query("update DirectMessageEntity message set message.senderArchivedAt = :at where message.senderPlayerId = :viewer and message.recipientPlayerId = :other and message.senderArchivedAt is null")
    int archiveSent(@Param("viewer") String viewer, @Param("other") String other, @Param("at") Instant at);
    @Modifying @Query("update DirectMessageEntity message set message.recipientArchivedAt = :at where message.recipientPlayerId = :viewer and message.senderPlayerId = :other and message.recipientArchivedAt is null")
    int archiveReceived(@Param("viewer") String viewer, @Param("other") String other, @Param("at") Instant at);
    long deleteBySentAtBefore(Instant cutoff);
}
