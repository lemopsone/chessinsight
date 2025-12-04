package ru.chessinsight.infrastructure.persistence.jpa.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;

import java.util.List;
import java.util.UUID;

public interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {
    List<GameEntity> findAllByUserId(UUID userId);
    List<GameEntity> findAllByPgn(String pgn);
}
