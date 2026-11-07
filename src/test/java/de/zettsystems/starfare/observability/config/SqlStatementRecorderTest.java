package de.zettsystems.starfare.observability.config;

import de.zettsystems.starfare.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class SqlStatementRecorderTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearThread() {
        SqlStatementRecorder.end();
    }

    @Test
    void countsTheStatementsOfThisThreadThroughHibernate() {
        SqlStatementRecorder.begin();
        for (int i = 0; i < 3; i++) {
            countGames();
        }
        SqlStatementRecorder.Recording recording = SqlStatementRecorder.end();

        assertThat(recording.count()).isEqualTo(3);
        assertThat(recording.repeated(3, 3)).singleElement().asString().startsWith("3× from ");
    }

    @Test
    void ignoresOtherThreadsAndTimesWithoutRecording() {
        countGames();
        SqlStatementRecorder.begin();
        CompletableFuture.runAsync(this::countGames).join();

        assertThat(SqlStatementRecorder.end().count()).isZero();
    }

    private void countGames() {
        entityManager.createQuery("select count(g) from GameSessionEntity g", Long.class).getSingleResult();
    }

    @Test
    void endWithoutBeginIsEmpty() {
        assertThat(SqlStatementRecorder.end().count()).isZero();
    }

    @Test
    void repeatedListsMostFrequentFirstAndShortensToTheFromClause() {
        SqlStatementRecorder.Recording recording = new SqlStatementRecorder.Recording();
        for (int i = 0; i < 4; i++) {
            recording.add("select a,b,c from game_sessions g where g.id=?");
        }
        for (int i = 0; i < 3; i++) {
            recording.add("select x from friendships f where f.a=?");
        }
        recording.add("select 1");

        assertThat(recording.repeated(3, 3)).containsExactly(
                "4× from game_sessions g where g.id=?", "3× from friendships f where f.a=?");
        assertThat(recording.repeated(4, 1)).containsExactly("4× from game_sessions g where g.id=?");
        assertThat(SqlStatementRecorder.Recording.shorten("select 1")).isEqualTo("select 1");
        assertThat(SqlStatementRecorder.Recording.shorten("select x from " + "t".repeat(100)))
                .hasSize(71).endsWith("…");
    }
}
