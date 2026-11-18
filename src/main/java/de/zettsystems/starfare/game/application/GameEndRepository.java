package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameEndEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface GameEndRepository extends JpaRepository<GameEndEntity, String> {
}
