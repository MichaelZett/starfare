package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BroadcasterTest {

    @Test
    void contextCloseStopsGlobalAndGameDeliveryIncludingLateSubscriptions() {
        var context = new AnnotationConfigApplicationContext(DefaultBroadcaster.class);
        var publisher = context.getBean(Broadcaster.class);
        GameId id = GameId.newId();
        List<GameEvent> received = new ArrayList<>();
        publisher.subscribe(id, received::add);
        publisher.subscribeAll(received::add);
        publisher.publish(new GameEvent.GameCreated(id));
        context.close();

        publisher.subscribe(id, received::add);
        publisher.subscribeAll(received::add);
        publisher.publish(new GameEvent.GameFinished(id, 1));

        assertThat(received).containsExactly(new GameEvent.GameCreated(id), new GameEvent.GameCreated(id));
    }

    private Broadcaster broadcaster;

    @BeforeEach
    void setUp() {
        broadcaster = new DefaultBroadcaster();
    }

    @Test
    void perGameSubscriberReceivesEventsForThatGame() {
        GameId id = GameId.newId();
        List<GameEvent> received = new ArrayList<>();

        broadcaster.subscribe(id, received::add);
        broadcaster.publish(new GameEvent.TurnAdvanced(id, 3));

        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isInstanceOfSatisfying(GameEvent.TurnAdvanced.class,
                event -> assertThat(event.turn()).isEqualTo(3));
    }

    @Test
    void perGameSubscriberDoesNotReceiveOtherGamesEvents() {
        GameId a = GameId.newId();
        GameId b = GameId.newId();
        List<GameEvent> received = new ArrayList<>();

        broadcaster.subscribe(a, received::add);
        broadcaster.publish(new GameEvent.TurnAdvanced(b, 5));

        assertThat(received).isEmpty();
    }

    @Test
    void unsubscribeStopsDelivery() {
        GameId id = GameId.newId();
        List<GameEvent> received = new ArrayList<>();

        Subscription registration = broadcaster.subscribe(id, received::add);
        broadcaster.publish(new GameEvent.TurnAdvanced(id, 1));
        registration.remove();
        broadcaster.publish(new GameEvent.TurnAdvanced(id, 2));

        assertThat(received).hasSize(1);
    }

    @Test
    void publishWithoutSubscribersIsNoOp() {
        GameId id = GameId.newId();

        assertThatCode(() -> broadcaster.publish(new GameEvent.PlayerJoined(id, 1))).doesNotThrowAnyException();
    }

    @Test
    void globalSubscriberReceivesAllEvents() {
        GameId a = GameId.newId();
        GameId b = GameId.newId();
        List<GameEvent> received = new ArrayList<>();

        broadcaster.subscribeAll(received::add);
        broadcaster.publish(new GameEvent.GameCreated(a));
        broadcaster.publish(new GameEvent.GameCreated(b));

        assertThat(received).hasSize(2);
    }

    @Test
    void globalUnsubscribeStopsDelivery() {
        GameId id = GameId.newId();
        List<GameEvent> received = new ArrayList<>();

        Subscription registration = broadcaster.subscribeAll(received::add);
        broadcaster.publish(new GameEvent.GameCreated(id));
        registration.remove();
        broadcaster.publish(new GameEvent.GameCreated(id));

        assertThat(received).hasSize(1);
    }

    @Test
    void publishDeliversToBothPerGameAndGlobalSubscribers() {
        GameId id = GameId.newId();
        List<GameEvent> perGame = new ArrayList<>();
        List<GameEvent> global = new ArrayList<>();

        broadcaster.subscribe(id, perGame::add);
        broadcaster.subscribeAll(global::add);
        broadcaster.publish(new GameEvent.ObserverJoined(id, "alice"));

        assertThat(perGame).hasSize(1);
        assertThat(global).hasSize(1);
    }

    @Test
    void multiplePerGameSubscribersAllReceive() {
        GameId id = GameId.newId();
        List<GameEvent> a = new ArrayList<>();
        List<GameEvent> b = new ArrayList<>();

        broadcaster.subscribe(id, a::add);
        broadcaster.subscribe(id, b::add);
        broadcaster.publish(new GameEvent.TurnAdvanced(id, 7));

        assertThat(a).hasSize(1);
        assertThat(b).hasSize(1);
    }
}
