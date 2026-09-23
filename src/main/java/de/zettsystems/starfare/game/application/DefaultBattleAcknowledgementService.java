package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected BattleAcknowledgementRepository is kept by reference for the bean's lifetime by design.")
public class DefaultBattleAcknowledgementService implements BattleAcknowledgementService {

    private final BattleAcknowledgementRepository repository;

    public DefaultBattleAcknowledgementService(BattleAcknowledgementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> acknowledgedEventIndices(String playerId, GameId gameId, int turn) {
        return Set.copyOf(repository.findEventIndices(playerId, gameId.value(), turn));
    }

    @Override
    @Transactional
    public void acknowledge(String playerId, GameId gameId, int turn, int eventIndex) {
        repository.insertIfAbsent(playerId, gameId.value(), turn, eventIndex, Instant.now());
        repository.deleteEarlierTurns(playerId, gameId.value(), turn);
    }

    @Override
    @Transactional
    public void forgetGame(GameId gameId) {
        repository.deleteByGameId(gameId.value());
    }
}
