package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.*;
import java.util.Optional;

public interface GameTemplateService {
    Optional<GameTemplate> template(GameId source, String account);
    Optional<GameId> create(GameId source, String account, String name, boolean rematch);
}
