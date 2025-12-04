package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.springframework.stereotype.Repository;
import ru.chessinsight.domain.game.model.Game;
import ru.chessinsight.domain.game.repository.GameRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.GameJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.GameEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PgGameRepository implements GameRepository {
    private final EntityMapper<Game, GameEntity> mapper;
    private final GameJpaRepository jpaRepository;

    public PgGameRepository(EntityMapper<Game, GameEntity> mapper, GameJpaRepository jpaRepository) {
        this.mapper = mapper;
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Game save(Game game) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(game)));
    }

    @Override
    public Optional<Game> findOneById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Game> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Game> findAllByPgn(String pgn) {
        return jpaRepository.findAllByPgn(pgn).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Game game) {
        jpaRepository.delete(mapper.toEntity(game));
    }
}
